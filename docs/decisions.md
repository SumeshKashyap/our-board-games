# Decisions

Key choices made while building Our Board Games, and why. Newest last.

### 1. Native Android with Kotlin + Jetpack Compose
Smooth on phones and tablets, good accessibility support, one codebase that adapts to screen size.
Cross-platform frameworks weren't needed: Android is the only target.

### 2. VS Code only, no Android Studio
The owner prefers working in VS Code. The Android SDK command-line tools, the Gradle wrapper and VS Code tasks cover
build, install, logs and tests. UI is checked with Robolectric screenshot tests instead of Android Studio previews.
The SDK lives in `~/Android/Sdk` because Android tools break on paths with spaces.

### 3. Stockfish as the engine (GPL-3.0)
The app started as personal/family use, so Stockfish's GPL licence was no obstacle, and it gives reliable, tunable
strength. When the repository became public, the project adopted GPL-3.0 to stay compliant. Even Stockfish's
weakest setting beats true beginners, so levels 1–2 also play an occasional random move.

### 4. Stockfish downloaded at build time, not committed
The binary is about 100 MB, too large to keep comfortably in git. Gradle downloads the official release and
verifies a pinned SHA-256, so builds are reproducible and tamper-evident.

### 5. Stockfish packaged as `libstockfish.so`
Android only lets apps execute files from their native-library directory. Naming the executable like a library (and
`useLegacyPackaging = true` so it is extracted on install) is the standard way chess apps ship UCI engines.

### 6. chesslib for rules, wrapped behind the app's own types
A mature, tested move generator saves writing one. Wrapping it (`ChessGame`) keeps the UI independent of it.

### 7. cburnett pieces with thickened outlines
A familiar, widely recognised design. Outlines were thickened from 1.5 to 2.0 so pieces stay clear for older eyes,
and converted to vector drawables so they're sharp at any size.

### 8. Tap-to-move first, drag second
Dragging is hard for unsteady hands and hides the piece under the finger. Tapping is the default; dragging works
too, with the piece enlarged and lifted above the finger.

### 9. Profiles with only a name
Anything more is friction for older users. Level, progress and saved games are remembered automatically.

### 10. Lichess puzzles, curated subset
The Lichess database is CC0, rated, and verified by millions of solves. 600 popular, well-rated puzzles (120 per
level) keep the APK small; `ContentTest` checks every one.

### 11. Simple JSON files instead of a database
A handful of small records doesn't justify Room or similar. Files are written atomically.

### 12. Public code, private photos
The repository is public, but the family photos on the loading screen are git-ignored and only included in APKs
built on the owner's computer. Public release APKs show a plain loading screen. This keeps pictures of family members
(including a young child) off the internet.

### 13. Players add their own photos on the device
Rather than rebuilding the app to change pictures, each player can add photos through Android's photo picker.
They stay on the device and join the random start-up selection, which also gives public-release installs
personal photos without anything being uploaded.

### 14. Settings belong to each player
Grandparents may want large print, high contrast and spoken moves while children don't. Settings live in the
profile, and the board reads them through a composition local, so switching player switches the whole look.

### 15. Sounds synthesised, off by default
Two tiny generated WAV files (a soft wooden tap and a double tap) avoid licensing questions and stay gentle.
Sounds, vibration and speech are opt-in, in keeping with "calm, nothing that startles".

### 16. Our Board Games keeps the old app ID
Renaming the Android application ID would make the new version a separate app: every family member would have to
reinstall and lose their profiles, photos and games. Only the visible name and icon changed; the code package is
still `com.sumesh.ourchess`.

### 17. Russian checkers, own engine
The family chose Russian-style rules (flying kings, backward captures). No suitable small engine exists, so the app
has its own: a full rules implementation with extensive tests, and an alpha-beta search that extends forced
captures. Levels change search depth and add random moves at the bottom.

### 18. Checkers puzzles mined by the engine
Hand-composed combinations are easy to get subtly wrong under Russian rules. Instead, a miner (`CheckersPuzzleMiner`,
run on demand) plays engine games and keeps positions with a single clearly-best line winning two or more pieces,
where every move the player must find is unique. Hard ones are rare, so there are fewer of them.

### 19. Ludo seats mix family and computer
Ludo is a family game. Each seat can be a profile, a guest or the computer, on one shared device; results are recorded
for each profile that played. Common Indian house rules were chosen (6 to start, three 6s lose the turn, safe stars,
extra roll for capture and for reaching home); there are no blocking pairs.

### 20. Releases from git tags via GitHub Actions
Pushing `vX.Y.Z` builds, tests, signs and publishes an APK, so releases are repeatable and the version always
matches the tag. The signing key is stored as encrypted GitHub secrets, with a backup on the owner's computer.
