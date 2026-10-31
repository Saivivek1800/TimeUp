# TimeUp

**Reclaim** is an Android app that helps you take your time back from social media.

You choose the apps that steal your time and set **one combined daily limit** for all of them. When the limit is used up, opening any of those apps shows a calm reading screen with articles picked from your interests, instead of the app. The limit can only be changed **once every 30 days**, so a weak moment can't undo your goal.

**Everything stays on your phone.** No account, no analytics, no server. Articles are fetched from public RSS feeds.

## What is in this repo

| Folder | What it is |
|---|---|
| [`Reclaim/`](Reclaim) | The Android app (Kotlin, Jetpack Compose, Material 3) |
| [`designs/`](designs) | Screen designs and the app icon exported from Google Stitch |

## Build it

You need the Android SDK (Android Studio installs it) and JDK 17 or newer.

```
cd Reclaim
.\gradlew.bat :app:assembleDebug        # Windows
./gradlew :app:assembleDebug            # macOS / Linux
```

The APK appears in `Reclaim/app/build/outputs/apk/debug/`. Run the unit tests with `gradlew :app:testDebugUnitTest`.

## Supported Android versions

Android 8.0 (API 26) up to Android 17. Built for Android 16 (API 36).

## Permissions

Reclaim asks for two special permissions, and explains why on screen before you grant them:

- **Usage Access**, to add up how long you spend in the apps you chose.
- **Accessibility Service**, to notice when one of those apps opens. It reads only the app's package name, never your screen, messages or text.

## Status

Work in progress, not yet on the Play Store.
