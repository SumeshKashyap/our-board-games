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
- Renamed to **Our Chess**, with a launcher icon
- Loading screen with private family photos
- Documentation, GPL-3.0 licence, public GitHub repository
- CI on every push; signed releases from version tags

## Next: Phase 5, accessibility and settings
- Settings screen: board colour themes (including a high-contrast theme), "Large print" letters on pieces,
  show/hide legal moves, confirm each move, always promote to a queen
- Optional move sounds and vibration
- TalkBack descriptions for squares and pieces; optionally speak moves aloud ("Knight to f3")
- Respect the system font size everywhere (test at 130% and 200%)

## Ideas for later
- Two players on one device
- Move list and simple game review ("what was my mistake?")
- More lessons (how each piece moves, basic checkmates, opening principles)
- Daily puzzle
- Statistics per player (games won per level)
