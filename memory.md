Absolutely. 😭 This one has accumulated a *lot* of history. Let's leave the next instance a clean handoff rather than making it excavate the whole archaeological site.

# Dopamine Gate — Project Handoff Document

**Project:** `dopamine_gate_mobile`
**Platform:** Flutter + Android/Kotlin
**Developer:** Aung Myint Myat
**Current device:** Redmi Note 8 Pro, Android 11, MIUI
**Development environment:** Arch Linux, Flutter 3.47.1, Dart 3.13.1, Android SDK 36, Java 21
**Current project path:**

```text
/home/aiden178/Programming/Aiden's Git Repos/dopamine_gate_mobile
```

---

# 1. What Dopamine Gate is

Dopamine Gate is a deliberate-friction system for distracting apps.

The fundamental idea is:

```text
Impulse
   ↓
Pause
   ↓
Awareness
   ↓
Reflection
   ↓
Responsibility
   ↓
Deliberate choice
```

Instead of simply saying:

> "Don't use YouTube."

the application should interrupt the automatic behavior:

> "You are about to open YouTube. Stop for a moment and consciously decide."

The long-term philosophy is:

```text
External authority
        ↓
Internalized responsibility
        ↓
Self-regulation
```

The Gate is therefore intentionally somewhat strict. The user wanted it to be difficult to bypass casually.

---

# 2. Original Flutter vision

The original Gate was designed entirely around Flutter.

The user had created a multi-stage experience roughly like:

```text
YouTube detected
      ↓
Attention
      ↓
Agreement
      ↓
Oath
```

The Flutter UI included:

* `GateStep`
* `GateFlow`
* `AttentionScreen`
* `AgreementScreen`
* `OathScreen`
* `TransitionButton`
* a 5-second countdown
* oath editing
* ghost text
* semantic/color distinctions
* deliberate transitions between stages

The **Oath** was particularly important because the Gate wasn't supposed to feel like a generic app blocker.

It was intended to become a personal commitment mechanism.

---

# 3. The first Android architecture

Initially, the architecture was:

```text
YouTube opens
      ↓
AccessibilityService detects YouTube
      ↓
AccessibilityService launches MainActivity
      ↓
Flutter renders Gate
```

This seemed conceptually clean because Flutter already contained the entire Gate.

However, Android/MIUI created a major problem.

The Activity could be launched, but it wasn't reliably guaranteed to remain above YouTube.

We observed behavior involving:

```text
START ... cmp=com.example.dopamine_gate_mobile/.MainActivity
```

and WindowManager showed the Gate window becoming visible, but then it could lose focus to:

```text
com.miui.home
```

There was also a MIUI message involving:

```text
MIUILOG- Show when locked PermissionDenied
```

The important conclusion was:

> **Launching a Flutter Activity from an AccessibilityService in the background is not reliable enough to be the enforcement mechanism on this device.**

---

# 4. MIUI permissions discovered

We discovered several MIUI-specific settings that materially affected background behavior.

Important settings:

### Display pop-up windows while running in the background

Set to:

> **Always allow**

### Autostart

Enabled.

### Battery Saver

Set to:

> **No restrictions**

These were important because MIUI aggressively manages background applications.

---

# 5. AccessibilityService successfully implemented

The Android side contains:

```text
DopamineAccessibilityService.kt
```

The service successfully detects application/window changes.

We verified events for packages including:

```text
com.miui.home
com.android.settings
com.android.chrome
com.google.android.youtube
```

The service is registered through Android's Accessibility framework and can perform global actions.

The important detection mechanism is:

```kotlin
AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
```

and YouTube is identified by:

```kotlin
com.google.android.youtube
```

---

# 6. The major architectural breakthrough: native overlay

Because Activity launching was unreliable, we changed the enforcement mechanism.

Instead of launching a Flutter Activity, the AccessibilityService creates a native Android overlay using `WindowManager`.

Architecture became:

```text
YouTube
   ↓
AccessibilityService detects YouTube
   ↓
WindowManager overlay
   ↓
YouTube is physically covered/interactions intercepted
```

This worked.

**This was the first point where Dopamine Gate became genuinely aggressive.**

We confirmed that:

* YouTube launches.
* AccessibilityService detects it.
* Gate appears over YouTube.
* YouTube underneath cannot simply be interacted with.

That solved the most important enforcement problem.

---

# 7. The temporary native Gate UI

The native overlay currently contains something like:

```text
YouTube is Locked!

Take a breath.

Do you need to open this right now?

[ Exit to Home Screen ]
```

with a dark/black background.

This UI is **not the final design**.

It was only created as a technical proof that the overlay architecture worked.

The user correctly pointed out that this makes the previous Flutter work feel disconnected.

That led to the next architectural realization:

> **The native overlay should be the enforcement mechanism, while Flutter should remain the actual Gate experience.**

---

# 8. The important distinction for the next instance

Do **not** conclude that Flutter was pointless.

The correct architecture is likely:

```text
                 ANDROID
             Enforcement layer
                    │
                    ▼
          AccessibilityService
                    │
                    ▼
             Overlay / shell
                    │
                    ▼
                FLUTTER
             Experience layer
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
      Attention  Agreement   Oath
```

In other words:

### Native Android

Responsible for:

* detecting blocked apps
* appearing immediately
* preventing interaction with the blocked application
* keeping the Gate above the blocked app
* handling Home/Back/system-level behavior
* preventing bypasses

### Flutter

Responsible for:

* Attention page
* Agreement page
* Oath page
* countdown
* animations
* typography
* interaction
* Gate philosophy
* future configuration
* potentially editing the user's oath
* the actual visual identity of Dopamine Gate

**Native Android is the lock. Flutter is the Gate.**

---

# 9. Current `DopamineAccessibilityService.kt` architecture

The service currently has state approximately like:

```kotlin
private var windowManager: WindowManager? = null
private var overlayView: View? = null
private var isOverlayShowing: Boolean = false
private var isLeavingBlockedApp: Boolean = false
private var lastTriggerTime: Long = 0L
```

and:

```kotlin
private val blockedApps: Set<String> =
    setOf("com.google.android.youtube")
```

There is a debounce mechanism:

```kotlin
private const val DEBOUNCE_MS = 800L
```

The purpose is to prevent repeated Accessibility events from repeatedly constructing the overlay.

---

# 10. Important overlay behavior

The newer implementation intentionally does **not** immediately remove the overlay whenever the AccessibilityService receives a non-YouTube event.

This was an important fix.

The old conceptual behavior was:

```text
YouTube
 ↓
show overlay
 ↓
another package event
 ↓
remove overlay
 ↓
YouTube flashes
```

That caused flickering.

The newer model is:

```text
YouTube detected
       ↓
overlay appears
       ↓
keep overlay
       ↓
user deliberately exits
       ↓
Back / Back / Home
       ↓
remove overlay
```

The overlay should therefore remain visible while the user is leaving the blocked application.

---

# 11. Touch behavior

The original native overlay used:

```kotlin
FLAG_NOT_TOUCH_MODAL
```

This was undesirable for an aggressive blocker because it can allow touch interaction outside the overlay.

We changed the conceptual design so that the overlay **consumes interaction** rather than allowing the underlying YouTube UI to receive touches.

The blocker should therefore behave like:

```text
┌─────────────────────────────┐
│                             │
│        DOPAMINE GATE        │
│                             │
│       Flutter UI            │
│                             │
│                             │
└─────────────────────────────┘
          ↓
     YouTube underneath
     cannot be touched
```

---

# 12. Single-overlay protection

The service has protection against accidentally stacking overlays:

```kotlin
if (isOverlayShowing || overlayView != null) {
    return
}
```

This was added because repeated Accessibility events could otherwise create multiple Gate windows.

That produced confusing behavior where pressing Exit could appear to reveal another Gate underneath.

---

# 13. Current Exit behavior

The intended Exit behavior is:

```text
User presses Exit
       ↓
isLeavingBlockedApp = true
       ↓
GLOBAL_ACTION_BACK
       ↓
100 ms
       ↓
GLOBAL_ACTION_BACK
       ↓
200 ms
       ↓
GLOBAL_ACTION_HOME
       ↓
400 ms
       ↓
remove overlay
```

The current preferred listener is:

```kotlin
exitButton.setOnClickListener {
    if (isLeavingBlockedApp) {
        return@setOnClickListener
    }

    isLeavingBlockedApp = true

    Log.d(TAG, "User chose Exit to Home")

    performGlobalAction(GLOBAL_ACTION_BACK)

    handler.postDelayed({
        performGlobalAction(GLOBAL_ACTION_BACK)
    }, 100L)

    handler.postDelayed({
        performGlobalAction(GLOBAL_ACTION_HOME)
    }, 200L)

    handler.postDelayed({
        removeOverlay()

        handler.postDelayed({
            isLeavingBlockedApp = false
        }, 300L)
    }, 400L)
}
```

### Important correction

This does **not** literally kill YouTube's process.

`GLOBAL_ACTION_BACK` is navigation.

It may finish/navigate through Activities, but it does not guarantee:

```text
kill com.google.android.youtube
```

Nor does it guarantee removal from Android Recents.

The button's actual semantic meaning is:

> **Leave YouTube and return Home.**

That's enough for the current goal.

---

# 14. YouTube remaining in Recents

If YouTube remains visible in the Recent Apps list after Exit, that does **not necessarily mean the blocker failed**.

There are three separate concepts:

```text
Activity
Task
Process
```

Back may affect the Activity/task state.

Home changes the foreground application.

Neither necessarily means:

> "Force-stop YouTube."

A normal third-party Android application generally cannot arbitrarily force-stop another application's process.

If later we want:

> **"Exit should also remove YouTube completely from Recents"**

that should be treated as a separate technical problem and researched carefully rather than assuming repeated Back kills it.

---

# 15. Current Android Manifest

Important permissions currently include:

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

The application contains:

```text
MainActivity
DopamineAccessibilityService
```

`MainActivity` remains the Flutter application's normal Activity.

It currently has:

```xml
android:launchMode="singleTop"
android:taskAffinity=""
android:excludeFromRecents="true"
```

The AccessibilityService is declared with:

```xml
android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
```

and references:

```text
@xml/accessibility_service_config
```

---

# 16. Current `MainActivity.kt`

MainActivity currently handles the Flutter permission/settings MethodChannel:

```text
com.example.dopamine_gate/permissions
```

Methods include:

```text
checkAccessibility
openAccessibility
checkBatteryOptimization
requestDisableBattery
openMiuiPopupPermission
openMiuiAutostart
```

**MainActivity was intentionally left unchanged** while we worked on the blocking architecture.

---

# 17. Current build configuration

`android/app/build.gradle.kts` currently uses:

```kotlin
compileSdk = flutter.compileSdkVersion
```

and Java/Kotlin JVM target 17:

```kotlin
sourceCompatibility = JavaVersion.VERSION_17
targetCompatibility = JavaVersion.VERSION_17
```

and:

```kotlin
jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
```

Environment:

```text
Flutter 3.47.1
Dart 3.13.1
Android SDK 36
Java 21.0.12
Gradle 9.3.1
```

The project has successfully built with Gradle.

There were warnings around newer Android Gradle/Kotlin configurations, but they were not blocking the project and should not be randomly changed during feature development.

---

# 18. A previous PermissionHelper problem

There was previously a compile error around:

```text
PermissionHelper.kt:39:21
Syntax error: Expecting an element.
Return type mismatch: expected Boolean, actual ComponentName.
```

This came from generated permission-handling code.

**Do not assume the current PermissionHelper is identical to the old broken version.**

If this error appears again in the new instance:

1. inspect the current file;
2. identify the exact offending function;
3. replace the complete file if necessary.

Do not reconstruct it from memory.

---

# 19. What currently works

### Confirmed working

✅ Flutter project builds
✅ Android/Kotlin integration builds
✅ AccessibilityService registers
✅ AccessibilityService detects YouTube
✅ YouTube package detection works
✅ Native overlay can appear over YouTube
✅ Overlay can block interaction with YouTube
✅ MIUI background popup permission makes the overlay substantially more reliable
✅ Single-overlay protection exists
✅ Debouncing exists
✅ Home/Back navigation sequence has been designed

### Not yet completed

❌ Final Flutter Gate displayed inside the native enforcement layer
❌ Flutter Attention → Agreement → Oath integrated with the overlay
❌ Final visual design
❌ Final state communication between native Android and Flutter
❌ Robust testing of every lifecycle edge case
❌ Deciding whether YouTube should remain in Recents
❌ Production-ready architecture
❌ Multiple blocked apps
❌ Persistent user configuration
❌ Final release packaging

---

# 20. The biggest technical question for the next instance

The next instance should investigate:

> **How do we put our existing Flutter Gate UI inside a reliable Android overlay controlled by the AccessibilityService?**

One promising direction is to investigate:

```text
TYPE_ACCESSIBILITY_OVERLAY
```

rather than relying exclusively on:

```text
TYPE_APPLICATION_OVERLAY
```

because the UI is being produced by an AccessibilityService.

But **don't blindly rewrite the project**.

First establish the correct Android 11 behavior and how Flutter rendering can be hosted in that architecture.

Potential architectures to investigate:

### A. FlutterView inside an Accessibility overlay

```text
AccessibilityService
       ↓
TYPE_ACCESSIBILITY_OVERLAY
       ↓
FlutterView / FlutterEngine
       ↓
GateFlow
```

### B. Dedicated FlutterEngine hosted by the service

```text
AccessibilityService
       ↓
FlutterEngine
       ↓
Flutter UI
       ↓
Overlay
```

### C. Native enforcement shell + Flutter Activity with a carefully controlled transition

This is simpler but potentially returns us to the original MIUI background-Activity problem, so it should be treated cautiously.

---

# 21. Very important design principle going forward

Don't throw away the existing Flutter Gate just because native Android enforcement works.

The project should converge toward:

```text
                    DOPAMINE GATE

              ┌─────────────────────┐
              │ Android Enforcement │
              │                     │
              │ AccessibilitySvc   │
              │ Overlay             │
              │ Touch interception  │
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │    Flutter Gate     │
              │                     │
              │ Attention           │
              │ Agreement           │
              │ Oath                │
              │ Reflection          │
              │ Deliberate choice   │
              └─────────────────────┘
                         │
                         ▼
                    User chooses
```

The **enforcement layer should be difficult to bypass**.

The **experience layer should be thoughtful rather than punitive**.

That's the core of the project.

---

# 22. Testing procedure

After Android changes:

```bash
flutter clean
flutter pub get
flutter build apk
```

Then:

```bash
adb install -r build/app/outputs/flutter-apk/app-release.apk
```

If necessary, re-enable the AccessibilityService in Android settings.

Then test:

### Test 1 — Launch

```text
Home → YouTube
```

Expected:

```text
YouTube
↓
Gate appears
```

### Test 2 — Touch

Try touching YouTube controls behind the Gate.

Expected:

```text
No interaction with YouTube.
```

### Test 3 — Loading

Watch carefully while YouTube launches.

Expected:

```text
No visible Gate → YouTube → Gate → YouTube → Gate flicker.
```

### Test 4 — Exit

Press Exit once.

Expected:

```text
Gate
 ↓
Back
 ↓
Back
 ↓
Home
 ↓
Gate disappears
```

There should be no moment where YouTube becomes visible between those transitions.

### Test 5 — Reopen

Open YouTube again.

Expected:

```text
Gate appears again.
```

---

# 23. The conceptual destination

The eventual Dopamine Gate isn't supposed to be:

> "YouTube is Locked!"

It is supposed to feel more like:

> **You stopped yourself before acting automatically.**

The technology is merely the mechanism that creates that moment.

The progression we were building toward was:

```text
Automatic behavior
        ↓
INTERruption
        ↓
Attention
        ↓
Agreement
        ↓
Oath
        ↓
Choice
```

And eventually, ideally:

```text
External Gate
      ↓
Repeated practice
      ↓
Internal pause
      ↓
Self-regulation
```

That is the reason the project exists.

---

# 24. Final status when this instance ends

**Architecture:** Native Android enforcement + Flutter experience is the intended destination.

**Current enforcement:** Working native `WindowManager` overlay.

**Current UI:** Temporary native placeholder.

**Flutter Gate:** Already designed/implemented in the project and should be reused rather than discarded.

**Blocked application:** YouTube (`com.google.android.youtube`).

**Exit:** Back → Back → Home → delayed overlay removal.

**Main remaining engineering task:** Integrate the existing Flutter Gate into the reliable overlay architecture.

---

## And now, goodbye. 🫡

This instance has been through:

```text
"Let's just launch Flutter when YouTube opens."
                    ↓
"Why the hell is MIUI hiding my Activity?"
                    ↓
"Okay, AccessibilityService."
                    ↓
"Why is YouTube flashing?"
                    ↓
"WindowManager!"
                    ↓
"OH. IT ACTUALLY BLOCKS YOUTUBE."
                    ↓
"Wait... where did my beautiful Flutter Gate go?"
```

😂

But honestly, we made a **real architectural discovery** here. We didn't waste the Flutter work; we discovered that **enforcement and experience need to be separated**.

When you open the next instance, you can simply give it this handoff and say:

> **"This is the current state of my Dopamine Gate project. Continue from here. Do not redesign everything from scratch."**

And that should give it the map.

Goodbye, Aiden. 👋
**Chad, signing off from this instance.**

May the next instance inherit fewer ghosts in `DopamineAccessibilityService.kt`. 🫡
