package com.sumesh.ourchess.ui.tutorials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumesh.ourchess.tutorials.Lesson
import com.sumesh.ourchess.tutorials.Lessons
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ButtonGroup
import com.sumesh.ourchess.ui.common.ChoiceCard
import com.sumesh.ourchess.ui.common.MenuPage
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.common.StatusBanner
import com.sumesh.ourchess.ui.common.StatusColors
import com.sumesh.ourchess.ui.exercise.ExerciseBoard
import com.sumesh.ourchess.ui.exercise.ExercisePhase

@Composable
fun LessonListScreen(done: Set<String>, onOpen: (Lesson) -> Unit, onBack: () -> Unit) {
    MenuPage {
        ScreenHeader("Learn moves", onBack = onBack, backLabel = "Menu")
        Text("Each lesson shows you a useful move. You make the move yourself on the board.", style = MaterialTheme.typography.bodyLarge)
        Lessons.all.forEachIndexed { i, lesson ->
            ChoiceCard(
                title = lesson.title,
                subtitle = lesson.summary,
                onClick = { onOpen(lesson) },
                leading = { LessonNumber(i + 1, lesson.id in done) },
            )
        }
    }
}

@Composable
private fun LessonNumber(number: Int, done: Boolean) {
    Box(
        Modifier.size(56.dp).background(if (done) MaterialTheme.colorScheme.primary else Color(0xFFE4DCC8), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(32.dp))
        } else {
            Text(number.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun LessonScreen(vm: LessonViewModel, onComplete: () -> Unit, onBack: () -> Unit) {
    val session = vm.session
    val state = session.state
    val lessonFinished = state.isFinished && vm.isLastStep
    LaunchedEffect(lessonFinished) { if (lessonFinished) onComplete() }

    BoardScreenLayout(
        header = { ScreenHeader(vm.lesson.title, onBack = onBack, backLabel = "Lessons") },
        info = {
            if (vm.lesson.steps.size > 1) {
                Text("Step ${vm.stepIndex + 1} of ${vm.lesson.steps.size}", style = MaterialTheme.typography.bodyMedium)
            }
            when (state.phase) {
                ExercisePhase.SOLVED, ExercisePhase.SOLUTION_SHOWN -> {
                    StatusBanner("Well done!", StatusColors.Good)
                    Explanation(vm.step.success)
                }
                ExercisePhase.WRONG_MOVE -> {
                    StatusBanner("Not quite — try again", StatusColors.Alert)
                    Explanation(vm.step.instruction)
                }
                else -> Explanation(vm.step.instruction)
            }
        },
        board = { modifier -> ExerciseBoard(session, modifier) },
        actions = { stacked ->
            ButtonGroup(stacked) { m ->
                when {
                    lessonFinished -> {
                        BigButton("Again", vm::restartStep, m, icon = Icons.Filled.Refresh)
                        BigButton("All lessons", onBack, m)
                    }
                    state.isFinished -> BigButton("Next step", vm::nextStep, m, icon = Icons.AutoMirrored.Filled.ArrowForward)
                    else -> BigButton(
                        "Show me",
                        session::showSolution,
                        m,
                        enabled = state.phase == ExercisePhase.YOUR_TURN || state.phase == ExercisePhase.WRONG_MOVE,
                    )
                }
            }
        },
    )
}

@Composable
private fun Explanation(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(16.dp),
    )
}
