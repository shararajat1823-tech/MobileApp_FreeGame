# Running & Testing MiniPlay

Two ways to see the app: **on a phone** (fastest, no PC setup) or **on a PC**
(Android Studio — needed if you want to edit, debug, or run without a phone).

---

## Option A — On an Android phone (recommended, no PC needed)

Every push to the dev branch auto-builds a debug APK and publishes it to a
stable link:

**https://github.com/shararajat1823-tech/MobileApp_FreeGame/releases/download/latest-debug/app-debug.apk**

1. Open that link in your phone's browser → it downloads `app-debug.apk`.
2. Tap the downloaded file to install. If Android warns about "unknown
   sources", allow it for your browser/Files app, then install.
3. Open **MiniPlay** and play. Installing a newer build over the top keeps
   your saved scores and stats.

This is the best way to judge touch controls, animation smoothness and feel.

---

## Option B — On a PC with Android Studio

### What to install

1. **Android Studio** (free) — <https://developer.android.com/studio>.
   It bundles the Android SDK, the correct Gradle, and the emulator, so you do
   **not** need to install Java/Gradle/SDK separately.
2. During first-run setup (or via **Settings → Languages & Frameworks → Android
   SDK → SDK Platforms / SDK Tools**), make sure these are installed:
   - **SDK Platform 35** (Android 15)
   - **Android SDK Build-Tools 35.0.0**
   - **Android SDK Platform-Tools** (gives you `adb`)
   - **Android Emulator** + one **Virtual Device** image (e.g. Pixel 7, API 34
     or 35) — only needed if you don't have a physical phone.
3. ~10–15 GB free disk. Enable hardware acceleration for a usable emulator
   (Windows: Windows Hypervisor Platform; macOS: automatic; Linux: KVM).

> Minimum this project targets: **compileSdk/targetSdk 35, minSdk 24, JDK 17**
> (JDK 17 ships inside Android Studio).

### Get the code

```bash
git clone https://github.com/shararajat1823-tech/MobileApp_FreeGame.git
cd MobileApp_FreeGame
git checkout claude/gracious-pasteur-veq1g5   # branch with the latest games
```

### Open & run

1. In Android Studio: **File → Open…** and select the `MobileApp_FreeGame`
   folder. Wait for **Gradle sync** to finish (first sync downloads
   dependencies — needs internet).
2. Pick a target in the top toolbar: a **virtual device** you created, **or**
   your phone connected by USB with **Developer options → USB debugging** on.
3. Press **▶ Run** (or `Shift+F10`). It builds, installs and launches the app.
4. Use **Logcat** (bottom of the window) to see any runtime messages/errors.

### Command line (optional)

```bash
./gradlew :app:assembleDebug      # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest  # runs the unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk   # install on a running device/emulator
```

### Fastest "just look at it" on PC (no Gradle)

If you only want to *see* the published build on the PC:

1. Install Android Studio, create one virtual device, and start it.
2. Download `app-debug.apk` (link above) and drag it onto the emulator window,
   or run `adb install app-debug.apk`.

This runs the exact build that CI produced — no project sync required.

---

## Running the games' unit tests

The game rules (Snake, Ludo, Minesweeper, Ragdoll Archers physics/AI, etc.) are
pure Kotlin and covered by unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

CI also runs these on every push.

---

## Notes

- The app is **offline-first** — everything here works in airplane mode; no
  login or backend is required.
- The debug APK is signed with the standard Android debug key, so it installs
  by side-loading but is not meant for the Play Store.
