Dopamine gate's attention page is already loaded. When the user is redirected from the blocked app to the dopamine gate page, there it redirected to the homepage first then take a couple of seconds to redirect to dopamine gate page. My suspicion is that dopamine gate is creating another instance instead of redirecting to the existing attention page. 

How to investigate on this?

---

Yes — your suspicion is plausible, but there are **two separate things we should investigate**:

1. **Is Android creating/reusing another `MainActivity` instance?**
2. **Even if it reuses the Activity, is Flutter rebuilding `main.dart` and briefly showing the Main Page before `_checkLaunchReason()` finishes?**

Given your current code, **#2 is actually very likely**.

Your current startup sequence is:

```text
MainActivity starts
      ↓
Flutter engine starts
      ↓
main()
      ↓
DopamineGateApp created
      ↓
_isInGate = false
      ↓
_buildHome()
      ↓
Main Page appears immediately
      ↓
_checkLaunchReason()
      ↓
Android MethodChannel call
      ↓
wasOpenedFromBlockedApp = true
      ↓
setState()
      ↓
Attention Page appears
```

That would produce exactly what you're seeing:

> Main Page briefly appears → a couple seconds later → Attention Page.

So **don't change the Android launch architecture yet**. Let's investigate first.

---

# Step 1 — Add Activity lifecycle logs

Open:

```text
android/app/src/main/kotlin/com/example/dopamine_gate_mobile/MainActivity.kt
```

We'll add logs that tell us whether Android is creating a new Activity or reusing the existing one.

Add:

```kotlin
import android.os.Bundle
import android.util.Log
```

Then inside `MainActivity` add:

```kotlin
companion object {
    private const val TAG = "DopamineGateActivity"
}
```

Then add these methods:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Log.d(
        TAG,
        "onCreate — Activity instance=${this.hashCode()}"
    )

    openedFromBlockedApp =
        intent.getBooleanExtra(
            "fromBlockedApp",
            false
        )

    Log.d(
        TAG,
        "onCreate — fromBlockedApp=$openedFromBlockedApp"
    )
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)

    Log.d(
        TAG,
        "onNewIntent — Activity instance=${this.hashCode()}"
    )

    if (intent != null) {
        setIntent(intent)

        openedFromBlockedApp =
            intent.getBooleanExtra(
                "fromBlockedApp",
                false
            )

        Log.d(
            TAG,
            "onNewIntent — fromBlockedApp=$openedFromBlockedApp"
        )
    }
}

override fun onStart() {
    super.onStart()

    Log.d(
        TAG,
        "onStart — Activity instance=${this.hashCode()}"
    )
}

override fun onResume() {
    super.onResume()

    Log.d(
        TAG,
        "onResume — Activity instance=${this.hashCode()}"
    )
}

override fun onPause() {
    super.onPause()

    Log.d(
        TAG,
        "onPause — Activity instance=${this.hashCode()}"
    )
}

override fun onStop() {
    super.onStop()

    Log.d(
        TAG,
        "onStop — Activity instance=${this.hashCode()}"
    )

}

override fun onDestroy() {
    super.onDestroy()

    Log.d(
        TAG,
        "onDestroy — Activity instance=${this.hashCode()}"
    )
}
```

Don't worry about understanding every lifecycle callback yet. We're basically putting **cameras around the door**. 😄

---

# Step 2 — Add Flutter startup logs

Now open:

```text
lib/main.dart
```

Add some logs.

Inside `initState()`:

```dart
@override
void initState() {
  super.initState();

  debugPrint('FLUTTER: DopamineGateApp initState');

  _loadProtectedApps();
  _checkLaunchReason();
}
```

Then change `_checkLaunchReason()` to:

```dart
Future<void> _checkLaunchReason() async {
  debugPrint('FLUTTER: Checking launch reason...');

  try {
    final openedFromBlockedApp =
        await _channel.invokeMethod<bool>(
      'wasOpenedFromBlockedApp',
    );

    debugPrint(
      'FLUTTER: wasOpenedFromBlockedApp = '
      '$openedFromBlockedApp',
    );

    if (!mounted) {
      debugPrint(
        'FLUTTER: Widget no longer mounted',
      );
      return;
    }

    if (openedFromBlockedApp == true) {
      debugPrint(
        'FLUTTER: Entering Gate from blocked app',
      );

      setState(() {
        _isInGate = true;
        _currentStep = GateStep.attention;
      });
    }
  } on PlatformException catch (e) {
    debugPrint(
      'FLUTTER: Failed to check launch reason: '
      '${e.message}',
    );
  }
}
```

And add a log to `_buildHome()`:

```dart
Widget _buildHome() {
  debugPrint(
    'FLUTTER: _buildHome — '
    'isInGate=$_isInGate, '
    'step=$_currentStep',
  );

  if (_isInGate) {
    return GateFlow.buildStep(
      _currentStep,
      _nextStep,
      _exit,
    );
  }

  return _buildMainPage();
}
```

---

# Step 3 — Watch Logcat

Run the app and reproduce the problem:

```text
YouTube
   ↓
Dopamine Gate overlay
   ↓
Go to Dopamine Gate
   ↓
watch what happens
```

From your terminal, you can filter the logs:

```bash
adb logcat | grep -E "DopamineGate|FLUTTER"
```

Or, if you're using Android Studio/VS Code logs, just look for:

```text
DopamineGateActivity
FLUTTER:
```

---

# What we're looking for

### Case A — One Activity, but Flutter initially shows Main Page

You might see:

```text
DopamineGateActivity: onCreate — Activity instance=123456
DopamineGateActivity: onStart — Activity instance=123456
DopamineGateActivity: onResume — Activity instance=123456

FLUTTER: DopamineGateApp initState
FLUTTER: _buildHome — isInGate=false
FLUTTER: Checking launch reason...

FLUTTER: wasOpenedFromBlockedApp = true
FLUTTER: Entering Gate from blocked app
FLUTTER: _buildHome — isInGate=true
```

If that's what you see:

**Great. There isn't an Activity duplication problem.**

The problem is simply:

```text
Flutter shows Main Page
        ↓
waits for MethodChannel
        ↓
discovers it should show Gate
        ↓
switches to Attention
```

That's a UI initialization race, and we can fix it cleanly.

---

# Case B — Two Activity instances

If you see something like:

```text
onCreate — Activity instance=111111
...
onDestroy — Activity instance=111111

onCreate — Activity instance=222222
...
```

or two different `onCreate` instances:

```text
onCreate — Activity instance=111111
onCreate — Activity instance=222222
```

then **yes**, Android is creating another `MainActivity`.

That would be a different problem, and we'd investigate your:

```kotlin
Intent.FLAG_ACTIVITY_NEW_TASK
Intent.FLAG_ACTIVITY_CLEAR_TOP
Intent.FLAG_ACTIVITY_SINGLE_TOP
Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
```

and the current Activity/task state.

---

# Case C — `onNewIntent` instead of `onCreate`

You may see:

```text
onCreate — Activity instance=111111
```

when the app initially starts.

Then later:

```text
onNewIntent — Activity instance=111111
onNewIntent — fromBlockedApp=true
```

This means Android **reused the existing Activity**.

That's actually what we want.

But there's a subtle problem with our current Flutter code:

`onNewIntent()` changes the native variable:

```kotlin
openedFromBlockedApp = true
```

but Flutter's `_checkLaunchReason()` may have already run earlier.

So in that case we'd need to **send the event from Android to Flutter**, rather than have Flutter ask Android only once during startup.

---

## So don't change anything else yet

Let's get the evidence first.

The most useful thing you can send me is the log from:

```text
Go to Dopamine Gate
```

through:

```text
Attention Page finally appears
```

especially lines containing:

```text
DopamineGateActivity
FLUTTER:
DopamineGate
```

Then we can tell **exactly** whether we're dealing with:

```text
A. Flutter initialization timing
```

or

```text
B. Multiple MainActivity instances
```

or

```text
C. Existing Activity + onNewIntent
```

My current bet is **A**, because your `main.dart` deliberately starts with `_isInGate = false` and only discovers the blocked-app launch asynchronously. But let's make the logs prove it rather than guessing.
