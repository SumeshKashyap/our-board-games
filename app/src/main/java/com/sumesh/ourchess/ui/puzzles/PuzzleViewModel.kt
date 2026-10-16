package com.sumesh.ourchess.ui.puzzles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.puzzles.Puzzle
import com.sumesh.ourchess.ui.exercise.ExerciseSession

class PuzzleViewModel(
    private val puzzles: List<Puzzle>,
    startIndex: Int,
    private val onSolved: () -> Unit,
    /** Remembers where to continue next time. */
    private val onAdvance: (nextIndex: Int) -> Unit,
) : ViewModel() {

    var index by mutableIntStateOf(startIndex.coerceIn(0, puzzles.lastIndex))
        private set
    val count: Int get() = puzzles.size
    val puzzle: Puzzle get() = puzzles[index]

    var session by mutableStateOf(newSession())
        private set

    fun next() {
        session.cancel()
        index = (index + 1) % puzzles.size
        onAdvance(index)
        session = newSession()
    }

    fun retry() {
        session.cancel()
        session = newSession()
    }

    private fun newSession(): ExerciseSession {
        val p = puzzle
        return ExerciseSession(viewModelScope, p.fen, p.moves.first(), p.moves.drop(1)) { unaided ->
            if (unaided) onSolved()
            // Either way, next time start with a fresh puzzle.
            onAdvance((puzzles.indexOf(p) + 1) % puzzles.size)
        }
    }
}
