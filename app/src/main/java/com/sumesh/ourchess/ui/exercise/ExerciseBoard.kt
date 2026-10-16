package com.sumesh.ourchess.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sumesh.ourchess.game.Side
import com.sumesh.ourchess.ui.board.ChessBoard
import com.sumesh.ourchess.ui.common.PromotionDialog

/** The board for an exercise, plus its promotion dialog when needed. */
@Composable
fun ExerciseBoard(session: ExerciseSession, modifier: Modifier) {
    val state = session.state
    ChessBoard(
        pieces = state.pieces,
        flipped = state.userSide == Side.BLACK,
        selected = state.selected,
        targets = state.targets,
        lastMove = state.lastMove,
        checkSquare = state.checkSquare,
        animation = state.animation,
        onSquareTap = session::onSquareTapped,
        onDragStart = session::onDragStart,
        onDrop = session::onDrop,
        modifier = modifier,
    )
    if (state.pendingPromotion != null) {
        PromotionDialog(state.userSide, session::onPromotionChosen, session::onPromotionCancelled)
    }
}
