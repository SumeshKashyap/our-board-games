package com.sumesh.ourchess.checkers

import com.sumesh.ourchess.game.Square

/*
 * Russian-style checkers (shashki) on an 8×8 board:
 * - Pieces stand on the dark squares (a1 is dark). White moves first, from ranks 1–3 "up" the board.
 * - Men move one square diagonally forward, but capture forwards AND backwards.
 * - Kings are "flying": they move any distance diagonally and capture from a distance,
 *   landing on any empty square beyond the captured piece.
 * - Capturing is compulsory, and a capture must continue while further captures are possible.
 *   The player may choose any capturing sequence (no "capture the most" rule).
 * - Captured pieces are removed only when the move is finished; a piece can't be jumped twice.
 * - A man reaching the far row becomes a king immediately — even mid-capture, continuing as a king.
 * - After a king's capture, if it can continue capturing from some landing squares, it must land on one of those.
 * - You win when the opponent has no pieces or no legal move.
 */

enum class CheckersColor {
    WHITE, BLACK;

    val opposite: CheckersColor get() = if (this == WHITE) BLACK else WHITE
    /** The direction men move in (rank +1 for White). */
    val forward: Int get() = if (this == WHITE) 1 else -1
    /** The rank where this colour's men become kings. */
    val crownRank: Int get() = if (this == WHITE) 7 else 0
}

data class CheckersPiece(val color: CheckersColor, val king: Boolean = false)

/** A complete move: the squares the piece visits, and the opponent pieces it captures (in order). */
data class CheckersMove(val path: List<Square>, val captured: List<Square> = emptyList()) {
    val from: Square get() = path.first()
    val to: Square get() = path.last()
    val isCapture: Boolean get() = captured.isNotEmpty()

    /** "c3-d4", or "c3:e5:g3" for captures. */
    val notation: String get() = path.joinToString(if (isCapture) ":" else "-") { it.name }

    override fun toString() = notation

    companion object {
        fun parse(text: String, legal: List<CheckersMove>): CheckersMove? = legal.firstOrNull { it.notation == text }
    }
}

enum class CheckersStatus {
    ONGOING, WHITE_WINS, BLACK_WINS, DRAW;

    val isOver: Boolean get() = this != ONGOING
}

private val DIAGONALS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

fun Square.isDark(): Boolean = (file + rank) % 2 == 0

private fun inBoard(f: Int, r: Int) = f in 0..7 && r in 0..7

/** An immutable board position. */
data class CheckersPosition(val pieces: Map<Square, CheckersPiece>, val toMove: CheckersColor) {

    fun legalMoves(): List<CheckersMove> {
        val own = pieces.filterValues { it.color == toMove }
        val captures = own.flatMap { (sq, piece) -> capturesFrom(sq, piece) }
        if (captures.isNotEmpty()) return captures
        return own.flatMap { (sq, piece) -> quietMovesFrom(sq, piece) }
    }

    fun apply(move: CheckersMove): CheckersPosition {
        val moving = pieces.getValue(move.from)
        val next = pieces.toMutableMap()
        next.remove(move.from)
        move.captured.forEach(next::remove)
        val crowned = moving.king || move.path.any { it.rank == moving.color.crownRank }
        next[move.to] = moving.copy(king = crowned)
        return CheckersPosition(next, toMove.opposite)
    }

    private fun quietMovesFrom(sq: Square, piece: CheckersPiece): List<CheckersMove> {
        val moves = mutableListOf<CheckersMove>()
        for ((df, dr) in DIAGONALS) {
            if (!piece.king && dr != piece.color.forward) continue
            var f = sq.file + df
            var r = sq.rank + dr
            while (inBoard(f, r) && Square(f, r) !in pieces) {
                moves += CheckersMove(listOf(sq, Square(f, r)))
                if (!piece.king) break
                f += df; r += dr
            }
        }
        return moves
    }

    private fun capturesFrom(start: Square, piece: CheckersPiece): List<CheckersMove> {
        val results = mutableListOf<CheckersMove>()
        // The moving piece has left its square; captured pieces stay (as blockers) until the move ends.
        val board = pieces - start
        fun search(at: Square, king: Boolean, path: List<Square>, captured: List<Square>) {
            val jumps = jumpsFrom(board, at, king, piece.color, captured)
            if (jumps.isEmpty()) {
                if (captured.isNotEmpty()) results += CheckersMove(path, captured)
                return
            }
            for ((victim, landings) in jumps) {
                val nowCaptured = captured + victim
                val options = landings.map { land -> land to (king || land.rank == piece.color.crownRank) }
                // A king must choose a landing square that lets it keep capturing, if there is one.
                val continuing = options.filter { (land, k) -> jumpsFrom(board, land, k, piece.color, nowCaptured).isNotEmpty() }
                for ((land, k) in continuing.ifEmpty { options }) search(land, k, path + land, nowCaptured)
            }
        }
        search(start, piece.king, listOf(start), emptyList())
        return results
    }

    /** Possible single jumps from [at]: each captured square with its possible landing squares. */
    private fun jumpsFrom(
        board: Map<Square, CheckersPiece>,
        at: Square,
        king: Boolean,
        color: CheckersColor,
        captured: List<Square>,
    ): List<Pair<Square, List<Square>>> {
        val jumps = mutableListOf<Pair<Square, List<Square>>>()
        for ((df, dr) in DIAGONALS) {
            var f = at.file + df
            var r = at.rank + dr
            if (king) while (inBoard(f, r) && Square(f, r) !in board) { f += df; r += dr }
            if (!inBoard(f, r)) continue
            val victimSq = Square(f, r)
            val victim = board[victimSq] ?: continue
            if (victim.color == color || victimSq in captured) continue
            val landings = mutableListOf<Square>()
            var lf = f + df
            var lr = r + dr
            while (inBoard(lf, lr) && Square(lf, lr) !in board) {
                landings += Square(lf, lr)
                if (!king) break
                lf += df; lr += dr
            }
            if (landings.isNotEmpty()) jumps += victimSq to landings
        }
        return jumps
    }

    /** A compact text form, used for repetition checks and saving. "W:" or "B:" then pieces like "wc3,WKd4". */
    fun key(): String = (if (toMove == CheckersColor.WHITE) "W:" else "B:") +
        pieces.entries.sortedBy { it.key.rank * 8 + it.key.file }.joinToString(",") { (sq, p) ->
            val c = if (p.color == CheckersColor.WHITE) "w" else "b"
            (if (p.king) c.uppercase() + "K" else c) + sq.name
        }

    companion object {
        val START: CheckersPosition = CheckersPosition(
            buildMap {
                for (r in 0..7) for (f in 0..7) {
                    val sq = Square(f, r)
                    if (!sq.isDark()) continue
                    if (r <= 2) put(sq, CheckersPiece(CheckersColor.WHITE))
                    if (r >= 5) put(sq, CheckersPiece(CheckersColor.BLACK))
                }
            },
            CheckersColor.WHITE,
        )

        fun fromKey(key: String): CheckersPosition {
            val toMove = if (key.startsWith("W")) CheckersColor.WHITE else CheckersColor.BLACK
            val body = key.substringAfter(':')
            val pieces = if (body.isBlank()) emptyMap() else body.split(',').associate { token ->
                val king = token.length > 3
                val color = if (token[0].lowercaseChar() == 'w') CheckersColor.WHITE else CheckersColor.BLACK
                Square.parse(token.takeLast(2)) to CheckersPiece(color, king)
            }
            return CheckersPosition(pieces, toMove)
        }
    }
}

/** One game of checkers with history (for undo, repetition and the no-progress draw). */
class CheckersGame(start: CheckersPosition = CheckersPosition.START) {

    private val positions = mutableListOf(start)
    private val played = mutableListOf<CheckersMove>()

    val position: CheckersPosition get() = positions.last()
    val toMove: CheckersColor get() = position.toMove
    val moves: List<CheckersMove> get() = played.toList()
    val lastMove: CheckersMove? get() = played.lastOrNull()
    val moveCount: Int get() = played.size

    fun legalMoves(): List<CheckersMove> = if (status().isOver) emptyList() else position.legalMoves()

    fun makeMove(move: CheckersMove): Boolean {
        if (move !in position.legalMoves()) return false
        positions += position.apply(move)
        played += move
        return true
    }

    fun makeMove(notation: String): Boolean =
        CheckersMove.parse(notation, position.legalMoves())?.let(::makeMove) ?: false

    fun undo(): CheckersMove? {
        if (played.isEmpty()) return null
        positions.removeAt(positions.lastIndex)
        return played.removeAt(played.lastIndex)
    }

    /** Pieces of [color] captured so far, as men/kings at the moment of capture. */
    fun lost(color: CheckersColor): List<CheckersPiece> =
        played.indices.flatMap { i -> played[i].captured.mapNotNull { positions[i].pieces[it] } }.filter { it.color == color }

    fun status(): CheckersStatus {
        val pos = position
        if (pos.legalMoves().isEmpty()) {
            return if (pos.toMove == CheckersColor.WHITE) CheckersStatus.BLACK_WINS else CheckersStatus.WHITE_WINS
        }
        // Same position (same side to move) three times.
        if (positions.count { it == pos } >= 3) return CheckersStatus.DRAW
        // 15 moves each (30 plies) with no capture and no man moving.
        if (played.size >= NO_PROGRESS_PLIES) {
            val recent = played.indices.toList().takeLast(NO_PROGRESS_PLIES)
            val noProgress = recent.all { i -> !played[i].isCapture && positions[i].pieces[played[i].from]?.king == true }
            if (noProgress) return CheckersStatus.DRAW
        }
        return CheckersStatus.ONGOING
    }

    companion object {
        const val NO_PROGRESS_PLIES = 30
    }
}
