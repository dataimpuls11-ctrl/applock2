# App Blocker (Android, Kotlin)

A simple, honest app blocker. Two rule types per app:

1. **Always Block** — the app is blocked at all times.
2. **Time Block** — the app is blocked only during a daily time window you set
   (e.g. 22:00–06:00). The window follows your **phone's clock**, so if your
   phone's timezone is set to Pakistan time, the schedule follows PKT
   automatically.

When a blocked app is opened, the service detects it immediately (via
Android's Accessibility API) and sends the device to the Home screen right
away — it does not wait minutes to kick in. A notification tells you which
app was blocked.

## How to open and build

1. Install **Android Studio** (Hedgehog or newer).
2. Open the `AppBlocker` folder as a project (`File → Open`).
3. Let Gradle sync (Android Studio will offer to create/update the Gradle
   wrapper automatically if asked — accept it).
4. Click **Run ▶** with a device or emulator connected, or
   **Build → Build Bundle(s)/APK(s) → Build APK(s)** to get an installable
   `.apk`.

## First-time setup on the phone

1. Open the app — you'll see the list of installed apps.
2. Tap **Enable Accessibility Permission** when prompted, find
   **"App Blocker"** in the system Accessibility list, and turn it ON.
   This permission is what lets the app notice instantly when a blocked app
   is opened.
3. On Android 13+, allow the notification permission when asked (so you see
   the "blocked" alert).
4. For each app in the list you can:
   - Toggle **Always Block** to block it permanently, or
   - Tap **Set Schedule** to pick a start time and end time for when it
     should be blocked.

## Notes on scope

This app only blocks apps while itself running with the Accessibility
permission on. It does **not** try to prevent itself from being uninstalled,
and it does **not** disable the Settings app — both of those would turn it
into the kind of hidden "device control" software (stalkerware-style
behavior) that is blocked for safety reasons, regardless of the good intent
behind self-control apps. You stay fully in control of your own device: you
can always open Settings → Apps → App Blocker and uninstall it or turn off
its Accessibility permission whenever you want.
