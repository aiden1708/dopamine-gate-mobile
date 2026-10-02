# Dopamine Gate

> **A gatekeeper launcher for protecting productive time from distracting applications.**

**Dopamine Gate** is an Android productivity tool designed to intercept distracting applications during designated productive hours.

Instead of simply telling you *“you shouldn't use this app”*, Dopamine Gate puts a deliberate gate between **impulse and action**.

When you attempt to open a blocked application during productive hours, Dopamine Gate intercepts the launch and requires you to pause and consciously acknowledge what you are about to do before continuing.

The goal is not to eliminate entertainment.

The goal is to make **intentional use easier than impulsive use**.

---

## ✦ Why Dopamine Gate?

Modern applications are designed to minimize the distance between:

```text
Thought → Action → Reward
```

You think:

> "I'll just check YouTube for a minute."

And seconds later, you're watching something completely unrelated to what you intended to do.

Dopamine Gate introduces friction:

```text
Impulse
   ↓
Blocked Application
   ↓
Dopamine Gate
   ↓
Pause
   ↓
Conscious Decision
   ↓
Continue / Exit
```

That small interruption gives you a chance to recover your original intention.

---

## Features

* 🚧 Intercepts selected distracting applications
* ⏰ Restricts applications during configurable productive hours
* 🧠 Forces a deliberate pause before continuing
* 🔒 Uses Android Accessibility Services to detect foreground applications
* 🪟 Displays an overlay when a blocked application is detected
* ↩️ Allows the user to return to Dopamine Gate instead of continuing
* ⚡ Lightweight and designed to run continuously in the background
* 📱 Designed for Android devices

---

# How It Works

Dopamine Gate runs an Android `AccessibilityService` that observes changes in the foreground application.

When a configured application becomes active:

```text
AccessibilityService
        │
        ▼
Is this a blocked application?
        │
       Yes
        │
        ▼
Is the current time inside
productive hours?
        │
       Yes
        │
        ▼
Show Dopamine Gate
        │
        ├── Continue
        │
        └── Go to Dopamine Gate
```

The application does **not** need to modify the blocked application's code.

Instead, it observes the Android UI state and reacts when the user attempts to enter a distracting application.

---

# Philosophy

Dopamine Gate is based on a simple behavioral principle:

> **The goal is not to rely entirely on willpower. Change the environment so impulsive behavior requires a deliberate action.**

Traditional application blockers often work like this:

```text
Blocked → Denied
```

Dopamine Gate experiments with:

```text
Blocked → Pause → Decide
```

This distinction is important.

The user is still responsible for the decision.

The application simply creates enough friction to make an automatic decision less automatic.

---

# Project Structure

The project is currently built with **Flutter** for the application interface and **Android/Kotlin** for the system-level interception.

A simplified architecture looks like this:

```text
Flutter Application
│
├── Main UI
│   ├── Gate configuration
│   ├── Productive hours
│   └── Blocked applications
│
└── Android Platform
    │
    ├── AccessibilityService
    │   ├── Detect foreground application
    │   ├── Identify blocked packages
    │   └── Trigger Gate
    │
    ├── Overlay
    │   └── Display Dopamine Gate above blocked app
    │
    └── Android configuration
        ├── Accessibility permission
        ├── Overlay permission
        └── Package visibility
```

---

# Requirements

## Development Environment

You will need:

* Flutter SDK
* Dart SDK
* Android SDK
* Android Studio or another Android development environment
* Java / JDK
* An Android device or emulator

The project was developed and tested with:

```text
Flutter 3.47.1
Dart 3.13.1
Android SDK 36
Java 21
```

Other compatible versions may work, but these are the versions used during development.

---

# Installation

## 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/dopamine_gate_mobile.git
cd dopamine_gate_mobile
```

Replace `YOUR_USERNAME` with the GitHub account containing the repository.

---

## 2. Install Flutter dependencies

```bash
flutter pub get
```

Check that the Flutter environment is correctly configured:

```bash
flutter doctor
```

Resolve any Android/Flutter configuration problems reported by `flutter doctor`.

---

## 3. Connect an Android device

Enable **Developer Options** and **USB debugging** on your Android device.

Verify that Flutter can see the device:

```bash
flutter devices
```

Then run:

```bash
flutter run
```

---

# Android Permissions

Dopamine Gate requires system-level permissions because it needs to detect when a blocked application becomes active and display its gate.

## Accessibility Service

The Android Accessibility Service is responsible for detecting foreground application changes.

After installing the application:

1. Open Android Settings.
2. Go to **Accessibility**.
3. Find **Dopamine Gate**.
4. Enable the Accessibility Service.

Without this permission, Dopamine Gate cannot detect when a blocked application is opened.

---

## Display Over Other Apps

Dopamine Gate uses an Android overlay to display the gate above the intercepted application.

Enable:

**Settings → Apps → Dopamine Gate → Display over other apps**

The exact location of this setting can vary between Android versions and manufacturers.

---

# Configuration

The list of applications to intercept is represented using Android package names.

For example:

```text
YouTube
com.google.android.youtube
```

Other applications can be identified by their Android package name.

The Accessibility Service compares the currently active package against the configured blocked packages.

Conceptually:

```kotlin
if (currentPackage in blockedPackages) {
    showGate()
}
```

---

# Productive Hours

Dopamine Gate is intended to operate only during designated productive periods.

For example:

```text
07:00 ───────────────────── 17:00
       PRODUCTIVE HOURS
```

Outside those hours, blocked applications can operate normally.

This is intentional.

The purpose of Dopamine Gate is not to permanently remove entertainment. It is to protect a specific period of time for work, study, or other intentional activities.

---

# Development

## Run in Debug Mode

```bash
flutter run
```

## Analyze the Flutter project

```bash
flutter analyze
```

## Build an APK

```bash
flutter build apk
```

The generated APK can be found under the Flutter build output directory.

---

# Android Architecture

The Android-specific portion of the project is written in Kotlin.

The central component is the Accessibility Service:

```text
DopamineAccessibilityService.kt
```

Its responsibilities include:

1. Receiving accessibility events.
2. Detecting changes in the foreground application.
3. Checking whether the application is blocked.
4. Checking whether blocking should currently be active.
5. Launching/displaying the Dopamine Gate.
6. Preventing the user from immediately falling through to the distracting application.
7. Handling cooldown/debounce logic to prevent repeated interception loops.

---

# Interception Flow

The intended flow is:

```text
User taps YouTube
        │
        ▼
Android opens YouTube
        │
        ▼
Accessibility Service detects YouTube
        │
        ▼
Is YouTube blocked?
        │
       YES
        │
        ▼
Dopamine Gate launches
        │
        ▼
User pauses
        │
        ├───────────────┐
        │               │
        ▼               ▼
    Continue       Go to Gate
        │               │
        ▼               ▼
    YouTube       Dopamine Gate
```

The implementation also uses debouncing/cooldown mechanisms to avoid repeatedly triggering the gate while Android is transitioning between activities.

---

# Design Principle: Friction, Not Force

Dopamine Gate is deliberately different from a conventional parental-control-style blocker.

It is designed for people who **want control over their own attention**.

The system therefore aims to create:

```text
Low friction for intentional behavior
              +
High friction for impulsive behavior
```

rather than simply making everything impossible.

---

# Current Status

Dopamine Gate is an experimental personal productivity project.

The project is actively being developed and its architecture may change.

Current development focuses on:

* Reliable application interception
* Preventing interception loops
* Gate launch reliability
* Transition/cooldown handling
* Productive-hour scheduling
* Improving the user experience
* Making the interception system reliable across Android devices

Android manufacturers may implement background processes, accessibility services, and overlays differently. Therefore, behavior can vary between devices.

---

# Known Limitations

Because Dopamine Gate relies on Android system functionality, several limitations exist.

### Android manufacturer differences

MIUI, HyperOS, Samsung One UI, and other Android variants may impose additional restrictions on background services and accessibility services.

### Accessibility Service dependency

If the Accessibility Service is disabled, Dopamine Gate cannot intercept applications.

### Overlay permission

The gate requires permission to display over other applications.

### System-level behavior

Android may terminate or restrict background processes depending on battery optimization and manufacturer-specific settings.

---

# Security & Privacy

Dopamine Gate is designed as a local productivity tool.

The application does not need the contents of the applications it blocks.

The Accessibility Service is used primarily to determine **which application is currently in the foreground**.

Because Accessibility Services are powerful Android capabilities, users should only enable them for software they trust.

---

# Contributing

Contributions, ideas, bug reports, and experiments are welcome.

Before making major architectural changes, please consider whether the change preserves the project's core principle:

> **Create a pause between impulse and action.**

For bugs, include:

* Android version
* Device manufacturer/model
* Flutter version
* Steps to reproduce
* Relevant `adb logcat` output

For example:

```bash
adb logcat | grep DopamineGate
```

---

# License

Add your preferred open-source license here.

For example:

```text
MIT License
```

If this project is eventually published publicly, the license should be chosen deliberately rather than left ambiguous.

---

# Project Vision

Dopamine Gate started from a simple observation:

> **Knowing that something is bad for you does not necessarily stop you from doing it.**

Human behavior is not controlled by knowledge alone.

Environment, timing, habit, friction, reward, and immediate impulses all matter.

Dopamine Gate is an experiment in building a small piece of technology around that reality.

Instead of asking:

> *"How can I become disciplined enough to never open distracting applications?"*

it asks:

> *"How can I design my environment so that I have a moment to choose before I act?"*

That is what **Dopamine Gate** is trying to build.

---

## Project Name

**Dopamine Gate**

A gate between:

```text
Impulse → Action
```

so there is room for:

```text
Impulse → Awareness → Choice → Action
```

---

**Dopamine Gate** — *Put a gate between the impulse and the action.*
