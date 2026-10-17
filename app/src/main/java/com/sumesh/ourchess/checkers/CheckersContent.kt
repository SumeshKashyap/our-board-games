package com.sumesh.ourchess.checkers

import android.content.res.AssetManager

/** A checkers puzzle: [position] (see [CheckersPosition.key]) and the moves to find (player, reply, player, …). */
data class CheckersPuzzle(val id: String, val level: Int, val position: CheckersPosition, val solution: List<String>, val goal: String)

object CheckersPuzzles {
    fun parse(csv: String): List<CheckersPuzzle> = csv.lineSequence().drop(1).filter { it.isNotBlank() }.map { line ->
        val f = line.split(',')
        CheckersPuzzle(f[0], f[1].toInt(), CheckersPosition.fromKey(f[2].replace(';', ',')), f[3].split(' '), f[4])
    }.toList()

    fun load(assets: AssetManager): List<CheckersPuzzle> =
        assets.open("checkers_puzzles.csv").bufferedReader().use { parse(it.readText()) }
}

/** One hands-on step: the player makes [solution] (player, reply, …) from position [key]. */
data class CheckersLessonStep(val key: String, val instruction: String, val solution: List<String>, val success: String)

data class CheckersLesson(val id: String, val title: String, val summary: String, val steps: List<CheckersLessonStep>)

object CheckersLessons {
    val all = listOf(
        CheckersLesson(
            "moving", "Moving a piece", "One step forward, diagonally",
            listOf(
                CheckersLessonStep(
                    "W:wc3;bh8", "Pieces only ever stand on the dark squares and move diagonally.\n\n" +
                        "An ordinary piece moves one square forward. Tap your piece on c3, then tap d4.",
                    listOf("c3-d4"), "That's all there is to a normal move: one square, diagonally forward.",
                ),
            ),
        ),
        CheckersLesson(
            "capture", "Capturing", "Jump over to take a piece",
            listOf(
                CheckersLessonStep(
                    "W:wc3;wa3;bd4;bh8", "Capture by jumping over an opponent's piece to the empty square behind it.\n\n" +
                        "Capturing is compulsory: if you can capture, you must. Jump from c3 over d4 to e5.",
                    listOf("c3:e5"), "The black piece is gone. Remember: when a capture is possible, you have to take it.",
                ),
            ),
        ),
        CheckersLesson(
            "backwards", "Capturing backwards", "Ordinary pieces can capture backwards",
            listOf(
                CheckersLessonStep(
                    "W:we5;bd4;bh8", "An ordinary piece only moves forward, but it may capture backwards too.\n\n" +
                        "Jump backwards from e5 over d4 to c3.",
                    listOf("e5:c3"), "Well done. Watch out for this — your opponent's pieces can capture backwards too.",
                ),
            ),
        ),
        CheckersLesson(
            "multi", "Several captures in one move", "Keep jumping while you can",
            listOf(
                CheckersLessonStep(
                    "W:wc3;bd4;bf6;ba7", "If, after a capture, you can capture again, you must keep going in the same move.\n\n" +
                        "Jump from c3 to e5, then on from e5 to g7.",
                    listOf("c3:e5:g7"), "Two pieces in one move! You can tap each landing square, or just the last one.",
                ),
            ),
        ),
        CheckersLesson(
            "king", "Becoming a king", "Reach the far side",
            listOf(
                CheckersLessonStep(
                    "W:wc7;ba1", "A piece that reaches the far row becomes a king. Kings are marked with a gold crown.\n\n" +
                        "Move your piece from c7 to d8.",
                    listOf("c7-d8"), "Your piece is now a king. Kings are very powerful, as the next lesson shows.",
                ),
            ),
        ),
        CheckersLesson(
            "flying", "The flying king", "Kings move and capture from far away",
            listOf(
                CheckersLessonStep(
                    "W:WKa1;bh2", "A king can move any number of squares along a diagonal, forwards or backwards.\n\n" +
                        "Move your king all the way from a1 to h8.",
                    listOf("a1-h8"), "A king can cross the whole board in one move.",
                ),
                CheckersLessonStep(
                    "W:WKa1;bd4;be7", "A king also captures from a distance, landing on any empty square beyond.\n\n" +
                        "But if it can capture again, it must land where it can. Take d4 landing on f6, then take e7 landing on d8.",
                    listOf("a1:f6:d8"), "Two pieces captured from far away. That is the power of the flying king!",
                ),
            ),
        ),
        CheckersLesson(
            "crown_capture", "Crowned during a capture", "Become a king and keep going",
            listOf(
                CheckersLessonStep(
                    "W:wb6;bc7;bg5", "If a piece reaches the far row while capturing, it becomes a king straight away and " +
                        "continues capturing as a king.\n\nJump from b6 over c7 to d8, then as a king take g5, landing on h4.",
                    listOf("b6:d8:h4"), "Crowned and captured again in one move!",
                ),
            ),
        ),
    )

    fun byId(id: String) = all.first { it.id == id }
}
