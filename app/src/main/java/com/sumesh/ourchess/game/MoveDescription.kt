package com.sumesh.ourchess.game

/** "white knight", "black pawn" */
fun Piece.spoken(): String = "${side.name.lowercase()} ${type.name.lowercase()}"

/**
 * Describes [move] in plain words, e.g. "Knight to f3", "Bishop takes pawn on c6, check",
 * "Castle king side", "Pawn to e8, becomes a queen, checkmate".
 * [before] is the game before the move is played; it is not changed.
 */
fun describeMove(before: ChessGame, move: Move): String {
    val mover = before.pieceAt(move.from) ?: return move.uci
    val after = ChessGame(before.fen).apply { makeMove(move) }
    val captured = before.pieceAt(move.to)
        ?: before.pieceAt(Square(move.to.file, move.from.rank)).takeIf {
            // En passant: a pawn moving diagonally onto an empty square
            mover.type == PieceType.PAWN && move.from.file != move.to.file
        }
    val name = mover.type.name.lowercase().replaceFirstChar { it.uppercase() }

    val text = buildString {
        when {
            mover.type == PieceType.KING && move.to.file - move.from.file == 2 -> append("Castle king side")
            mover.type == PieceType.KING && move.from.file - move.to.file == 2 -> append("Castle queen side")
            captured != null -> append("$name takes ${captured.type.name.lowercase()} on ${move.to.name}")
            else -> append("$name to ${move.to.name}")
        }
        move.promotion?.let { append(", becomes a ${it.name.lowercase()}") }
        when {
            after.status() == GameStatus.CHECKMATE -> append(", checkmate")
            after.isInCheck -> append(", check")
        }
    }
    return text
}

/** A move just played, for sound, vibration and speech feedback. [id] increases with every move. */
data class MoveEvent(val id: Int, val description: String, val byOpponent: Boolean, val capture: Boolean)
