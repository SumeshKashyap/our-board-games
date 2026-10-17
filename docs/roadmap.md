# Roadmap

## Done

### Phase 0: Setup
Android SDK command-line tools, Gradle wrapper, Kotlin + Compose project, VS Code tasks and extension recommendations.

### Phase 1: Chess rules
`ChessGame` wrapper over chesslib with unit tests for every rule (castling, en passant, promotion, checkmate,
stalemate, repetition, 50-move rule, insufficient material).

### Phase 2: Board and game screen
High-visibility pieces, soft board colours, tap-to-move and drag, legal-move dots and capture rings, last-move and
check highlights, slide animations, promotion dialog, undo, new game (as White or Black), captured pieces, phone and
tablet layouts.

### Profiles, levels, puzzles and lessons
Welcome screen with name-only profiles, difficulty 1–5, main menu, 600 Lichess puzzles with hints and solutions,
7 hands-on lessons.

### Phase 3: Stockfish
Stockfish 19 opponent tuned per level, Hint button in games, natural "thinking" pause, fallback engine.

### Phase 4: Game flow and release
- Games saved automatically and resumed after the app is closed ("Continue your game" in the menu)
- Game-over dialog with *Play again*
- Material balance ("+3") next to captured pieces
- Renamed to **Our Board Games**, with a launcher icon
- Loading screen: 3 random photos for 5 seconds, then fading into the background of the player screen
- *My photos*: each player can add their own pictures to the start-up photos
- Documentation, GPL-3.0 licence, public GitHub repository
- CI on every push; signed releases from version tags

### Phase 5: Accessibility and settings
- Per-player Settings screen with live preview: board themes (Green, Wood, Blue, High contrast), large-print
  letters, show/hide possible moves, ask before each move, always choose a queen
- Optional move sounds, vibration, and reading moves aloud ("Computer: knight to f3, check")
- TalkBack: every square is described and can be tapped
- Every screen checked at 130% and 200% system font size
- Board coordinates moved outside the squares
- Magnifying glass to zoom the whole app in or out (80–150%), remembered per player

### Version 2.0: Our Board Games
- Renamed from *Our Chess* (same app ID, so devices update in place); new dice icon
- New home screen with a menu for each game
- **Checkers** (Russian rules) with its own engine (levels 1–5), puzzles mined by the engine, and 7 lessons
- **Ludo** for 2–4 players, mixing family members and computer players, with a hands-on tutorial
- **My statistics** for every player

## Ideas for later
- Two players on one device for chess and checkers
- More checkers puzzles at the harder levels (they are rare in engine games)
- Move list and simple game review ("what was my mistake?")
- More lessons (how each piece moves, basic checkmates, opening principles)
- Daily puzzle
- Statistics per player (games won per level)
