package com.sumesh.ourchess.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumesh.ourchess.ludo.LudoColor
import com.sumesh.ourchess.ludo.LudoGeometry.HOME
import com.sumesh.ourchess.ludo.LudoGeometry.YARD
import com.sumesh.ourchess.ludo.LudoRules
import com.sumesh.ourchess.ludo.LudoState
import com.sumesh.ourchess.ui.common.BigButton
import com.sumesh.ourchess.ui.common.BoardScreenLayout
import com.sumesh.ourchess.ui.common.ScreenHeader
import com.sumesh.ourchess.ui.common.StatusBanner
import com.sumesh.ourchess.ui.common.StatusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One tutorial step: Red rolls [dice] in [start] and moves a token. */
data class LudoTutorialStep(val title: String, val start: LudoState, val dice: Int, val instruction: String, val success: String)

object LudoTutorialSteps {
    private fun state(red: List<Int>, yellow: List<Int> = List(4) { YARD }) =
        LudoState(listOf(LudoColor.RED, LudoColor.YELLOW), mapOf(LudoColor.RED to red, LudoColor.YELLOW to yellow))

    val all = listOf(
        LudoTutorialStep(
            "Getting started", state(List(4) { YARD }), 6,
            "You play Red. All your tokens start in your yard.\n\nYou need a 6 to bring a token out. Tap the dice, then tap a token.",
            "Your token is out on its start square. A 6 also gives you another roll!",
        ),
        LudoTutorialStep(
            "Moving", state(listOf(0, YARD, YARD, YARD)), 4,
            "Tokens move round the board, clockwise, by the number on the dice.\n\nTap the dice, then your token.",
            "Your token moved 4 squares.",
        ),
        LudoTutorialStep(
            "Capturing", state(listOf(2, YARD, YARD, YARD), listOf(31, YARD, YARD, YARD)), 3,
            "Land exactly on an opponent's token to send it back to its yard.\n\nRoll, then move onto the yellow token.",
            "Captured! The yellow token went back home, and you get another roll as a reward.",
        ),
        LudoTutorialStep(
            "Safe squares", state(listOf(5, YARD, YARD, YARD), listOf(34, YARD, YARD, YARD)), 3,
            "Squares with a star, and the coloured start squares, are safe: no one can be captured there.\n\nRoll and move onto the star.",
            "Both tokens share the star safely. Use stars to hide from danger!",
        ),
        LudoTutorialStep(
            "Your home path", state(listOf(47, YARD, YARD, YARD)), 5,
            "After going all the way round, a token turns into its own coloured path towards the centre. No one can capture it there.\n\nRoll and move.",
            "Your token is on the red home path, safe from everyone.",
        ),
        LudoTutorialStep(
            "Reaching home", state(listOf(HOME, HOME, HOME, 53)), 3,
            "To reach home in the centre you need the exact number.\n\nRoll and bring your last token home.",
            "All four tokens are home: you win! That's everything you need to play Ludo.",
        ),
    )
}

class LudoTutorialViewModel : ViewModel() {
    var stepIndex by mutableIntStateOf(0)
        private set
    var game by mutableStateOf(LudoTutorialSteps.all[0].start)
        private set
    var face by mutableIntStateOf(1)
        private set
    var rolling by mutableStateOf(false)
        private set
    var done by mutableStateOf(false)
        private set
    var animation by mutableStateOf<LudoAnimation?>(null)
        private set
    private var nextId = 0

    val step: LudoTutorialStep get() = LudoTutorialSteps.all[stepIndex]
    val isLast: Boolean get() = stepIndex == LudoTutorialSteps.all.lastIndex
    val canRoll: Boolean get() = !done && !rolling && game.dice == null
    val movable: Set<Int> get() = if (game.dice != null && animation == null) game.movableTokens().toSet() else emptySet()

    fun roll() {
        if (!canRoll) return
        rolling = true
        viewModelScope.launch {
            repeat(6) { face = (1..6).random(); delay(80) }
            face = step.dice
            game = LudoRules.roll(game, step.dice).first
            rolling = false
        }
    }

    fun tokenTapped(token: Int) {
        if (token !in movable) return
        val (next, event) = LudoRules.move(game, token)
        animation = LudoAnimation(nextId++, event.color, token, event.from, event.to)
        game = next
        viewModelScope.launch {
            delay(140L * (if (event.from == YARD) 1 else event.to - event.from) + 300)
            animation = null
            done = true
        }
    }

    fun next() {
        if (isLast) return
        stepIndex++
        game = step.start
        done = false
    }

    fun restart() {
        game = step.start
        done = false
    }
}

@Composable
fun LudoTutorialScreen(vm: LudoTutorialViewModel, onComplete: () -> Unit, onBack: () -> Unit) {
    val finished = vm.done && vm.isLast
    LaunchedEffect(finished) { if (finished) onComplete() }

    BoardScreenLayout(
        header = { ScreenHeader("How to play Ludo", onBack = onBack, backLabel = "Back") },
        info = {
            Text("Step ${vm.stepIndex + 1} of ${LudoTutorialSteps.all.size}: ${vm.step.title}", style = MaterialTheme.typography.titleMedium)
            if (vm.done) StatusBanner("Well done!", StatusColors.Good)
            Text(
                if (vm.done) vm.step.success else vm.step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            )
        },
        board = { modifier -> LudoBoard(vm.game, vm.movable, vm.animation, vm::tokenTapped, modifier) },
        actions = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    finished -> BigButton("Back to Ludo", onBack, Modifier.fillMaxWidth())
                    vm.done -> BigButton("Next step", vm::next, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
                    else -> LudoDice(
                        vm.face, LudoColor.RED.main(), vm.canRoll, vm::roll,
                        label = if (vm.canRoll) "Tap to roll" else if (vm.movable.isNotEmpty()) "Now tap your token" else null,
                    )
                }
            }
        },
    )
}
