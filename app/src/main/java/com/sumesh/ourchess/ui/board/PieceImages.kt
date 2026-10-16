package com.sumesh.ourchess.ui.board

import androidx.annotation.DrawableRes
import com.sumesh.ourchess.R
import com.sumesh.ourchess.game.Piece
import com.sumesh.ourchess.game.PieceType
import com.sumesh.ourchess.game.Side

@DrawableRes
fun pieceDrawable(piece: Piece): Int = pieceDrawable(piece.side, piece.type)

@DrawableRes
fun pieceDrawable(side: Side, type: PieceType): Int = when (side) {
    Side.WHITE -> when (type) {
        PieceType.KING -> R.drawable.piece_white_king
        PieceType.QUEEN -> R.drawable.piece_white_queen
        PieceType.ROOK -> R.drawable.piece_white_rook
        PieceType.BISHOP -> R.drawable.piece_white_bishop
        PieceType.KNIGHT -> R.drawable.piece_white_knight
        PieceType.PAWN -> R.drawable.piece_white_pawn
    }
    Side.BLACK -> when (type) {
        PieceType.KING -> R.drawable.piece_black_king
        PieceType.QUEEN -> R.drawable.piece_black_queen
        PieceType.ROOK -> R.drawable.piece_black_rook
        PieceType.BISHOP -> R.drawable.piece_black_bishop
        PieceType.KNIGHT -> R.drawable.piece_black_knight
        PieceType.PAWN -> R.drawable.piece_black_pawn
    }
}

fun PieceType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
