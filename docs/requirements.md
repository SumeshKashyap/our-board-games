# Requirements

This document records what Our Chess must do and why. It is the reference for future changes: if a change
conflicts with something here, update this document deliberately rather than drifting from it.

## Purpose and audience

- A chess app for **Android phones and tablets**, for **personal and family use** (not published on the Play Store).
- The main audience is **older players**. Everything is judged by one question: *can a grandparent use this
  comfortably without help?*
- Young family members use it too, so it should be friendly, not clinical.

## Core principles

1. **Very intuitive and simple.** Few screens, few buttons, plain words. Nothing that needs explaining.
2. **Easy on the eyes.** Pieces must be easy to tell apart at a glance:
   - Large pieces with thick outlines, on a soft, low-glare board (cream and muted green, no pure white or black).
   - High contrast between white and black pieces on both square colours.
   - Highlights never rely on colour alone: shapes too (dots, rings, borders).
   - Text larger than Android defaults; buttons at least 64 dp tall with text labels.
3. **Forgiving.** No clock, unlimited undo, hints, confirmation before destructive actions (new game, removing a player).
4. **Calm.** Nothing flashy: no sounds that startle, no ads, no accounts, no internet needed.

## Functional requirements

### Starting the app
- **Loading screen** shows family photos as the background, with the app name **"Our Chess"**,
  while the app gets ready (engine start-up, puzzle loading).
  - Each launch shows **3 photos chosen at random** from all available photos: the bundled family photos plus
    every player's own photos.
  - On landscape tablets the three are shown side by side; on phones they cross-fade one after another.
  - They stay at full brightness for **5 seconds**, then **slowly fade** into a soft background behind the
    player-selection screen. Tapping skips ahead.
  - The bundled photos are **private**: they must never be uploaded to the public repository or public releases (see below).

### Profiles
- First launch: a gentle welcome that asks only for the player's **name**.
- Later launches: **choose an existing player** or **create a new one**. Players can be removed (with confirmation).
- Each profile automatically remembers its difficulty level, puzzle progress, finished lessons and game in progress.
- **My photos**: each player can add their own pictures (from the device's photo picker) or remove them. These join
  the pool of start-up photos. They are resized and stored privately on the device; nothing is uploaded.

### Difficulty
- After choosing a profile, the player picks a **difficulty level from 1 to 5**
  (Beginner, Easy, Medium, Hard, Expert), each with a one-line description.
- The level controls both the computer opponent's strength and which puzzles are shown.

### Main menu
After choosing the level, the player chooses one of:
1. **Play against computer**: a standard game.
2. **Puzzles**: chess puzzles to solve.
3. **Learn moves**: tutorials that teach useful moves.

The menu also offers *My photos*, *Settings*, *Change level* and *Switch player*.

### Settings (per player)
Each player has their own settings, so one person's choices never affect another's:
- **Board colours**: Green (default), Wood, Blue, **High contrast** (yellow/blue).
- **Large print**: a letter badge on each piece (K, Q, R, B, N; pawns are left plain).
- **Show possible moves**: dots/rings for legal moves (on by default). Hints always show.
- **Ask before each move**: in games, every move needs "Yes, play it" or "No".
- **Always choose a queen**: skip the promotion dialog.
- **Move sounds**: a soft tap (moves) or double tap (captures).
- **Vibrate**: a short buzz on each move, firmer for the opponent's move.
- **Read moves aloud**: e.g. "Computer: knight to f3, check", using the device's text-to-speech.

Sound, vibration and speech are off by default. Switching one on gives an immediate sample.
A preview board on the Settings screen shows the look straight away.

### Playing against the computer
- Only human-vs-computer play (no two-player or online modes for now).
- Choose to play as White or Black; the board turns around when playing Black.
- Tap-to-move (tap the piece, then the square) is the default; drag-and-drop also works.
- Legal moves are shown; the last move is highlighted; a king in check glows red.
- The computer's move slides slowly so it is easy to follow.
- Status in plain words: "Your move", "Computer is thinking…", "Check! Your move", "Checkmate — you won!".
- **Undo** (unlimited), **Hint** (suggests a good move), **New Game** (with confirmation).
- Promotion offers four large piece choices.
- Captured pieces and material balance (e.g. "+3") are shown.
- When the game ends, a dialog announces the result and offers **Play again**.
- Games are **saved automatically** and resume after the app is closed.
- The screen stays on during play.

### Puzzles
- Real puzzles, matched to the chosen level (120 per level).
- The opponent plays first, then the player finds the best move. Wrong moves are taken back with "Not quite — try again".
- **Hint** shows the piece and its destination; **Show answer** plays the solution slowly.
- Progress (next puzzle, number solved) is saved per player.

### Learn moves (tutorials)
- Short, hands-on lessons: the explanation is shown, then the player makes the move on the board.
- Lessons: castling, pawn promotion, en passant, the fork, the pin, the skewer, back-rank checkmate.
- *Show me* demonstrates the move. Finished lessons are ticked off.

## Non-functional requirements

- **Devices**: Android 8.0+ (API 26), 64-bit ARM phones and tablets; portrait and landscape.
- **Accessibility**: every screen must work at system font sizes up to 200% (buttons stack, boards stay large and
  the page scrolls instead of shrinking the board). TalkBack reads each square ("e4, white pawn") and can make moves.
- **Offline**: everything works without internet.
- **Development**: VS Code-native workflow; no Android Studio required.
- **Source**: public GitHub repository, GPL-3.0 (required because Stockfish is bundled).
- **Releases**: created by pushing version tags (`vX.Y.Z`); GitHub Actions builds a signed APK and publishes it.
- **Privacy**: family photos stay on the development computer (`app/images/`, git-ignored). Only locally built
  APKs include them; public release APKs show a plain loading screen.

## Out of scope (for now)

Two-player on one device, online play, accounts, chess clocks, game analysis, Play Store publishing.
See [roadmap.md](roadmap.md) for ideas.
