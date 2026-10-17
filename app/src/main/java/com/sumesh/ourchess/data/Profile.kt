package com.sumesh.ourchess.data

/** A player on this device. Only the name is asked for; the rest is remembered automatically. */
data class Profile(
    val id: String,
    val name: String,
    val level: Int = 1,
    val lastUsed: Long = 0,
    /** Index of the next puzzle to show, per difficulty level. */
    val nextPuzzle: Map<Int, Int> = emptyMap(),
    val puzzlesSolved: Int = 0,
    val lessonsDone: Set<String> = emptySet(),
    val settings: PlayerSettings = PlayerSettings(),
    val stats: Stats = Stats(),
    /** Small counters for checkers and Ludo, e.g. "checkers.puzzle.L3" → next puzzle, "checkers.solved". */
    val progress: Map<String, Int> = emptyMap(),
)

object Levels {
    val all = 1..5

    fun name(level: Int) = when (level) {
        1 -> "Beginner"
        2 -> "Easy"
        3 -> "Medium"
        4 -> "Hard"
        else -> "Expert"
    }

    fun description(level: Int) = when (level) {
        1 -> "Just learning how the pieces move"
        2 -> "Plays for fun now and then"
        3 -> "Knows the basics and some tactics"
        4 -> "Plays regularly and thinks ahead"
        else -> "A strong, experienced player"
    }
}
