# Development guide

Our Board Games is built entirely from **VS Code** with the Android command-line tools. Android Studio is not needed.

## One-time setup (Linux)

1. **Java 17**
   ```bash
   sudo apt install openjdk-17-jdk
   ```
2. **Android SDK command-line tools** in `~/Android/Sdk` (avoid paths with spaces: Android tools dislike them).
   Download "Command line tools only" from <https://developer.android.com/studio#command-line-tools-only>, then:
   ```bash
   mkdir -p ~/Android/Sdk/cmdline-tools
   unzip commandlinetools-linux-*.zip -d /tmp/clt && mv /tmp/clt/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
   yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
   ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms/android-35" "build-tools/35.0.0"
   ```
3. **Tell Gradle where the SDK is**: create `local.properties` in the project root (it is git-ignored):
   ```properties
   sdk.dir=/home/<you>/Android/Sdk
   ```
4. **VS Code extensions**: open the folder and accept the recommended extensions (Kotlin, Gradle, Android).
5. Optional, for using `adb` in any terminal: add to `~/.bashrc`
   ```bash
   export PATH="$HOME/Android/Sdk/platform-tools:$PATH"
   ```

The first build downloads Gradle, the libraries, and Stockfish (about 80 MB, checked against a pinned SHA-256 and
cached in `.cache/stockfish/`).

## VS Code tasks

Open **Terminal → Run Task…** (or `Ctrl+Shift+P` → *Tasks: Run Task*):

| Task | What it does |
|---|---|
| Build debug APK | `./gradlew assembleDebug` (also `Ctrl+Shift+B`) |
| Run on device | Builds, installs and opens the app on the connected phone/tablet |
| Unit tests | Runs all tests |
| Screenshots (phone + tablet) | Renders every screen to `app/build/outputs/roborazzi/` |
| Logcat (app only) | Shows the app's log output from the device |
| List connected devices | `adb devices -l` |
| Build release APK (family, with photos) | Signed release APK including the private loading-screen photos |
| Install release APK on device | Installs the signed release build |
| Publish release (push version tag) | Tags `vX.Y.Z` and pushes it, triggering the GitHub release build |

`Ctrl+Shift+P` → *Tasks: Rerun Last Task* repeats the previous one.

## Running on a phone or tablet

1. On the device: **Settings → About tablet/phone**, tap **Build number** 7 times (on Lenovo ZUI: **ZUI version**).
   You can also search Settings for "build number".
2. **Settings → System → Developer options** → turn on **USB debugging** (or search Settings for "USB debugging").
3. Plug it in and accept **Allow USB debugging** (tick *Always allow from this computer*).
4. Run the **Run on device** task.

If `adb devices` shows `unauthorized`, accept the prompt on the device; if it shows `no permissions`, run
`sudo apt install android-sdk-platform-tools-common` and replug.

**Wireless**: Developer options → *Wireless debugging* → *Pair device with pairing code*, then
`adb pair <ip>:<port>` and `adb connect <ip>:<port>`.

## Testing

```bash
./gradlew testDebugUnitTest      # everything (about a minute)
./gradlew recordRoborazziDebug   # also writes screenshots to app/build/outputs/roborazzi/
```

- Tests run on the JVM; screens are rendered with Robolectric, so **no device or emulator is needed**.
- `StockfishEngineTest` runs the real Stockfish using the Linux x86-64 build (downloaded automatically). On other
  computers it is skipped.
- Check the screenshots after any UI change: phone portrait (411×891 dp) and tablet landscape (1280×800 dp).

## Family photos (loading screen)

Put pictures (`.jpg`, `.jpeg` or `.png`) in **`app/images/`**. They are shown behind the "Our Board Games" title when the
app starts. The folder is **git-ignored**: the photos never leave this computer, and only APKs built here include
them. Builds elsewhere show a plain green loading screen.

## Common changes

| To change… | Edit |
|---|---|
| Computer strength per level | `EngineLevels` in `engine/Engines.kt` |
| Level names and descriptions | `Levels` in `data/Profile.kt` |
| Board and piece colours | `BoardColors` in `ui/theme/Theme.kt` |
| Text sizes | `AppTypography` in `ui/theme/Theme.kt` |
| Lessons | `tutorials/Lessons.kt` (add a lesson; `ContentTest` verifies the moves are legal) |
| Puzzles | `app/src/main/assets/puzzles.csv` (`id,level,fen,moves,rating,goal`, Lichess format) |
| Stockfish version | `stockfishVersion` and the two SHA-256 values in `app/build.gradle.kts` |

## Piece images

The pieces are the cburnett set converted to Android vector drawables with outlines thickened from 1.5 to 2.0
for readability (`app/src/main/res/drawable/piece_*.xml`). The converter handled only `<g>`, `<path>` and
`<circle>` with inherited attributes, which is all that set uses.
