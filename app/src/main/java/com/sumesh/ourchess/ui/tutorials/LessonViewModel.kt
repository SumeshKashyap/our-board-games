package com.sumesh.ourchess.ui.tutorials

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.tutorials.Lesson
import com.sumesh.ourchess.tutorials.LessonStep
import com.sumesh.ourchess.ui.exercise.ExerciseSession

class LessonViewModel(val lesson: Lesson) : ViewModel() {

    var stepIndex by mutableIntStateOf(0)
        private set
    val step: LessonStep get() = lesson.steps[stepIndex]
    val isLastStep: Boolean get() = stepIndex == lesson.steps.lastIndex

    var session by mutableStateOf(newSession())
        private set

    fun nextStep() {
        if (isLastStep) return
        session.cancel()
        stepIndex++
        session = newSession()
    }

    fun restartStep() {
        session.cancel()
        session = newSession()
    }

    private fun newSession() = ExerciseSession(viewModelScope, step.fen, setupMove = null, solution = step.solution)
}
