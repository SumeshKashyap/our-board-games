package com.sumesh.ourchess.game

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece as CPiece
import com.github.bhlangonijr.chesslib.PieceType as CPieceType
import com.github.bhlangonijr.chesslib.Side as CSide
import com.github.bhlangonijr.chesslib.Square as CSquare
import com.github.bhlangonijr.chesslib.move.Move as CMove

/**
 * The rules of chess for one game, wrapping chesslib behind the app's own simple types.
 * Not thread-safe: use it from a single thread (the UI thread).
 */
class ChessGame(startFen: String = START_FEN) {

    private class Played(val move: Move, val captured: Piece?)

    private val board = Board().apply { loadFromFen(startFen) }
    private val played = ArrayDeque<Played>()

    val fen: String get() = board.fen
    val sideToMove: Side get() = board.sideToMove.toSide()
    val moveCount: Int get() = played.size
    val lastMove: Move? get() = played.lastOrNull()?.move
    /** Every move played so far, oldest first. */
    val moves: List<Move> get() = played.map { it.move }
    val isInCheck: Boolean get() = board.isKingAttacked

    fun pieceAt(square: Square): Piece? = board.getPiece(square.toC()).toPiece()

    /** Every square with a piece on it. */
    fun pieces(): Map<Square, Piece> =
        Square.all.mapNotNull { sq -> pieceAt(sq)?.let { sq to it } }.toMap()

    fun kingSquare(side: Side): Square = board.getKingSquare(side.toC()).toSquare()

    fun legalMoves(): List<Move> = board.legalMoves().map { it.toMove() }

    fun legalMovesFrom(square: Square): List<Move> = legalMoves().filter { it.from == square }

    /** True if moving from [from] to [to] is a legal pawn promotion (so a piece must be chosen). */
    fun isPromotion(from: Square, to: Square): Boolean =
        legalMoves().any { it.from == from && it.to == to && it.promotion != null }

    /** Plays [move] if it is legal. Returns false (and changes nothing) otherwise. */
    fun makeMove(move: Move): Boolean {
        val legal = board.legalMoves().firstOrNull { it.toMove() == move } ?: return false
        val captured = capturedBy(move)
        board.doMove(legal)
        played.addLast(Played(move, captured))
        return true
    }

    fun makeUciMove(uci: String): Boolean = makeMove(Move.parseUci(uci))

    /** Takes back the last move. Returns it, or null if there was nothing to undo. */
    fun undo(): Move? {
        if (played.isEmpty()) return null
        board.undoMove()
        return played.removeLast().move
    }

    /** Pieces each side has lost so far, in the order they were captured. */
    fun lostPieces(side: Side): List<PieceType> =
        played.mapNotNull { it.captured }.filter { it.side == side }.map { it.type }

    fun status(): GameStatus = when {
        board.isMated -> GameStatus.CHECKMATE
        board.isStaleMate -> GameStatus.STALEMATE
        board.isInsufficientMaterial -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
        board.isRepetition -> GameStatus.DRAW_REPETITION
        board.halfMoveCounter >= 100 -> GameStatus.DRAW_FIFTY_MOVES
        else -> GameStatus.ONGOING
    }

    private fun capturedBy(move: Move): Piece? {
        pieceAt(move.to)?.let { return it }
        val mover = pieceAt(move.from) ?: return null
        // En passant: a pawn moving diagonally onto an empty square captures the pawn beside it.
        if (mover.type == PieceType.PAWN && move.from.file != move.to.file) {
            return pieceAt(Square(move.to.file, move.from.rank))
        }
        return null
    }

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}

// --- chesslib <-> app type conversions ---

private fun Square.toC(): CSquare = CSquare.squareAt(rank * 8 + file)

private fun CSquare.toSquare(): Square = Square(ordinal % 8, ordinal / 8)

private fun Side.toC(): CSide = if (this == Side.WHITE) CSide.WHITE else CSide.BLACK

private fun CSide.toSide(): Side = if (this == CSide.WHITE) Side.WHITE else Side.BLACK

private fun CPieceType.toType(): PieceType? = when (this) {
    CPieceType.KING -> PieceType.KING
    CPieceType.QUEEN -> PieceType.QUEEN
    CPieceType.ROOK -> PieceType.ROOK
    CPieceType.BISHOP -> PieceType.BISHOP
    CPieceType.KNIGHT -> PieceType.KNIGHT
    CPieceType.PAWN -> PieceType.PAWN
    else -> null
}

private fun CPiece.toPiece(): Piece? {
    if (this == CPiece.NONE) return null
    return Piece(pieceSide.toSide(), pieceType.toType() ?: return null)
}

private fun CMove.toMove(): Move = Move(from.toSquare(), to.toSquare(), promotion.toPiece()?.type)
