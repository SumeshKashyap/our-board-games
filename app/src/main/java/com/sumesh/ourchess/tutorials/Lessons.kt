package com.sumesh.ourchess.tutorials

/**
 * One hands-on exercise: the player makes the [solution] moves themselves
 * (player, reply, player, ... in UCI notation) from position [fen].
 */
data class LessonStep(
    val fen: String,
    val instruction: String,
    val solution: List<String>,
    val success: String,
)

data class Lesson(val id: String, val title: String, val summary: String, val steps: List<LessonStep>)

object Lessons {
    val all = listOf(
        Lesson(
            id = "castling",
            title = "Castling",
            summary = "Move your king to safety",
            steps = listOf(
                LessonStep(
                    fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                    instruction = "Castling moves your King to safety and brings a Rook into play, all in one move.\n\n" +
                        "Tap your King, then tap the square two steps to its right (g1). The Rook jumps over by itself.",
                    solution = listOf("e1g1"),
                    success = "Your King is now safe in the corner, behind a wall of pawns.",
                ),
            ),
        ),
        Lesson(
            id = "promotion",
            title = "Pawn promotion",
            summary = "Turn a pawn into a Queen",
            steps = listOf(
                LessonStep(
                    fen = "8/4P1k1/8/8/8/8/8/4K3 w - - 0 1",
                    instruction = "When a pawn reaches the far side of the board, it becomes a new piece.\n\n" +
                        "Move your pawn to e8 and choose the Queen, the strongest piece.",
                    solution = listOf("e7e8q"),
                    success = "Your pawn is now a Queen! Promoting a pawn often wins the game.",
                ),
            ),
        ),
        Lesson(
            id = "en_passant",
            title = "En passant",
            summary = "A special pawn capture",
            steps = listOf(
                LessonStep(
                    fen = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                    instruction = "Black's pawn has just jumped two squares, from d7 to d5, and landed right beside your pawn.\n\n" +
                        "For this one move only, you may capture it \"in passing\": move your pawn diagonally to d6.",
                    solution = listOf("e5d6"),
                    success = "You captured the pawn in passing. This is only allowed straight after the two-square jump.",
                ),
            ),
        ),
        Lesson(
            id = "fork",
            title = "The fork",
            summary = "Attack two pieces at once",
            steps = listOf(
                LessonStep(
                    fen = "r3k3/8/8/3N4/8/8/8/4K3 w - - 0 1",
                    instruction = "A fork attacks two pieces at the same time. Your opponent can only save one of them.\n\n" +
                        "Move your Knight to c7. It checks the King and attacks the Rook. After the King moves, capture the Rook.",
                    solution = listOf("d5c7", "e8e7", "c7a8"),
                    success = "The King had to move, so your Knight won the Rook. Knights are excellent at forks!",
                ),
            ),
        ),
        Lesson(
            id = "pin",
            title = "The pin",
            summary = "Freeze a piece in place",
            steps = listOf(
                LessonStep(
                    fen = "4k3/pp6/2n5/8/8/8/PP6/4KB2 w - - 0 1",
                    instruction = "A pin stops a piece from moving because something more valuable stands behind it.\n\n" +
                        "Move your Bishop to b5 to pin the black Knight to its King.",
                    solution = listOf("f1b5"),
                    success = "The Knight cannot move now: if it did, its own King would be in check.",
                ),
            ),
        ),
        Lesson(
            id = "skewer",
            title = "The skewer",
            summary = "Win the piece hiding behind",
            steps = listOf(
                LessonStep(
                    fen = "4q3/8/4k3/8/8/8/8/R6K w - - 0 1",
                    instruction = "A skewer attacks a valuable piece. When it moves away, you win the piece behind it.\n\n" +
                        "Move your Rook to e1 to check the King. After the King steps aside, capture the Queen!",
                    solution = listOf("a1e1", "e6d5", "e1e8"),
                    success = "The King had to get out of check, leaving the Queen behind for your Rook.",
                ),
            ),
        ),
        Lesson(
            id = "back_rank",
            title = "Back-rank checkmate",
            summary = "Trap the King behind its pawns",
            steps = listOf(
                LessonStep(
                    fen = "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1",
                    instruction = "The black King is trapped behind its own pawns on the back row.\n\n" +
                        "Move your Rook all the way to a8. That is checkmate!",
                    solution = listOf("a1a8"),
                    success = "Checkmate! Always give your own King an escape square to avoid this trap.",
                ),
            ),
        ),
    )

    fun byId(id: String): Lesson = all.first { it.id == id }
}
