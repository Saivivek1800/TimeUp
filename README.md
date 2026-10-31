# TimeUp

**Reclaim** is an Android app that helps you take your time back from social media.

You choose the apps that steal your time and set **one combined daily limit** for all of them. When the limit is used up, opening any of those apps shows a calm reading screen with articles picked from your interests, instead of the app. The limit can only be changed **once every 30 days**, so a weak moment can't undo your goal.

**Everything stays on your phone.** No account, no analytics, no server. Articles are fetched from public RSS feeds.

## What is in this repo

| Folder | What it is |
|---|---|
| [`Reclaim/`](Reclaim) | The Android app (Kotlin, Jetpack Compose, Material 3) |
| [`designs/`](designs) | Screen designs and the app icon exported from Google Stitch |
| [`docs/`](docs) | The beta download website (hosted on Vercel) and the [privacy policy](docs/privacy.html) |

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

## Roadmap: test first, then the Play Store

Reclaim is **free**. There is no payment and no account, and it keeps no data off your phone. The plan for releasing it:

1. **Beta download (planned).** A simple website with a **Download** button will offer the Android app file (`.apk`). Anyone can install it, try it, and send feedback. The link will be added here when the page is live.
2. **Improve it.** Feedback from testers decides what gets fixed or changed.
3. **Google Play Store (planned).** Once the beta has settled, the app will be published on Google Play.

Right now it is **work in progress**: not yet on the website and not yet on the Play Store.

### What to expect when you install the beta

Because the beta is installed from a website and not from the Play Store, Android will ask for a few extra steps:

- Allow your browser to **install unknown apps**. Play Protect may show an "unknown developer" warning, so choose "Install anyway".
- On Android 13 and newer, the accessibility switch may be greyed out. Open **App info for Reclaim**, tap the **⋮** menu, choose **Allow restricted settings**, then turn on Reclaim in Accessibility settings.

The Play Store version will not need these steps.
