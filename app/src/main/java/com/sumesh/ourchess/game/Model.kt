package com.sumesh.ourchess.game

enum class Side {
    WHITE, BLACK;

    val opposite: Side get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val uciChar: Char) {
    KING('k'), QUEEN('q'), ROOK('r'), BISHOP('b'), KNIGHT('n'), PAWN('p');
}

data class Piece(val side: Side, val type: PieceType)

/** A board square; file 0..7 = a..h, rank 0..7 = 1..8. */
data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Square out of range: $file,$rank" }
    }

    val name: String get() = "${'a' + file}${rank + 1}"
    val isLight: Boolean get() = (file + rank) % 2 == 1

    override fun toString() = name

    companion object {
        val all: List<Square> = (0..7).flatMap { rank -> (0..7).map { file -> Square(file, rank) } }

        fun parse(name: String): Square = Square(name[0] - 'a', name[1] - '1')
    }
}

data class Move(val from: Square, val to: Square, val promotion: PieceType? = null) {
    /** UCI notation, e.g. "e2e4" or "e7e8q". */
    val uci: String get() = from.name + to.name + (promotion?.uciChar ?: "")

    override fun toString() = uci

    companion object {
        fun parseUci(uci: String): Move {
            val promo = uci.getOrNull(4)?.let { c -> PieceType.entries.first { it.uciChar == c.lowercaseChar() } }
            return Move(Square.parse(uci.substring(0, 2)), Square.parse(uci.substring(2, 4)), promo)
        }
    }
}

enum class GameStatus {
    ONGOING,
    CHECKMATE,
    STALEMATE,
    DRAW_REPETITION,
    DRAW_FIFTY_MOVES,
    DRAW_INSUFFICIENT_MATERIAL;

    val isOver: Boolean get() = this != ONGOING
}
