package com.sumesh.ourchess.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessGameTest {

    private fun ChessGame.play(vararg uci: String) = uci.forEach { assertTrue("illegal: $it", makeUciMove(it)) }

    @Test
    fun startingPosition() {
        val game = ChessGame()
        assertEquals(20, game.legalMoves().size)
        assertEquals(Side.WHITE, game.sideToMove)
        assertEquals(Piece(Side.WHITE, PieceType.KING), game.pieceAt(Square.parse("e1")))
        assertEquals(Piece(Side.BLACK, PieceType.QUEEN), game.pieceAt(Square.parse("d8")))
        assertNull(game.pieceAt(Square.parse("e4")))
        assertEquals(32, game.pieces().size)
        assertEquals(GameStatus.ONGOING, game.status())
    }

    @Test
    fun legalMovesFromSquare() {
        val game = ChessGame()
        assertEquals(setOf("e3", "e4"), game.legalMovesFrom(Square.parse("e2")).map { it.to.name }.toSet())
        assertEquals(setOf("a3", "c3"), game.legalMovesFrom(Square.parse("b1")).map { it.to.name }.toSet())
        assertTrue(game.legalMovesFrom(Square.parse("e8")).isEmpty()) // not black's turn
    }

    @Test
    fun illegalMoveIsRejected() {
        val game = ChessGame()
        assertFalse(game.makeUciMove("e2e5"))
        assertEquals(0, game.moveCount)
        assertEquals(Side.WHITE, game.sideToMove)
    }

    @Test
    fun foolsMateIsCheckmate() {
        val game = ChessGame()
        game.play("f2f3", "e7e5", "g2g4", "d8h4")
        assertTrue(game.isInCheck)
        assertEquals(GameStatus.CHECKMATE, game.status())
        assertEquals(Side.WHITE, game.sideToMove) // white is mated
        assertEquals(Square.parse("e1"), game.kingSquare(Side.WHITE))
    }

    @Test
    fun stalemate() {
        val game = ChessGame("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertEquals(GameStatus.STALEMATE, game.status())
    }

    @Test
    fun castlingMovesTheRook() {
        val game = ChessGame("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        game.play("e1g1")
        assertEquals(Piece(Side.WHITE, PieceType.KING), game.pieceAt(Square.parse("g1")))
        assertEquals(Piece(Side.WHITE, PieceType.ROOK), game.pieceAt(Square.parse("f1")))
        assertNull(game.pieceAt(Square.parse("h1")))
    }

    @Test
    fun enPassantCaptureIsRecorded() {
        val game = ChessGame()
        game.play("e2e4", "a7a6", "e4e5", "d7d5", "e5d6")
        assertNull(game.pieceAt(Square.parse("d5")))
        assertEquals(listOf(PieceType.PAWN), game.lostPieces(Side.BLACK))
    }

    @Test
    fun promotion() {
        val game = ChessGame("8/P6k/8/8/8/8/8/K7 w - - 0 1")
        assertTrue(game.isPromotion(Square.parse("a7"), Square.parse("a8")))
        assertFalse(game.isPromotion(Square.parse("a1"), Square.parse("a2")))
        assertFalse(game.makeMove(Move(Square.parse("a7"), Square.parse("a8")))) // piece must be chosen
        game.play("a7a8n")
        assertEquals(Piece(Side.WHITE, PieceType.KNIGHT), game.pieceAt(Square.parse("a8")))
    }

    @Test
    fun undoRestoresPositionAndCaptures() {
        val game = ChessGame()
        val start = game.fen
        game.play("e2e4", "d7d5", "e4d5")
        assertEquals(listOf(PieceType.PAWN), game.lostPieces(Side.BLACK))
        assertEquals(Move.parseUci("e4d5"), game.lastMove)
        assertEquals(Move.parseUci("e4d5"), game.undo())
        assertTrue(game.lostPieces(Side.BLACK).isEmpty())
        game.undo(); game.undo()
        assertEquals(start, game.fen)
        assertNull(game.undo())
    }

    @Test
    fun drawByInsufficientMaterial() {
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, ChessGame("8/8/4k3/8/8/3K4/8/8 w - - 0 1").status())
    }

    @Test
    fun drawByThreefoldRepetition() {
        val game = ChessGame()
        game.play("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8")
        assertEquals(GameStatus.DRAW_REPETITION, game.status())
    }

    @Test
    fun drawByFiftyMoveRule() {
        val game = ChessGame("8/8/4k3/8/8/3K4/8/R7 w - - 99 80")
        game.play("a1a2")
        assertEquals(GameStatus.DRAW_FIFTY_MOVES, game.status())
    }

    @Test
    fun uciRoundTrip() {
        assertEquals("e7e8q", Move.parseUci("e7e8q").uci)
        assertEquals(PieceType.QUEEN, Move.parseUci("e7e8q").promotion)
        assertEquals(Square(4, 3), Square.parse("e4"))
    }
}

class MoveDescriptionTest {
    private fun describe(fen: String, uci: String) = describeMove(ChessGame(fen), Move.parseUci(uci))

    @Test
    fun describesMovesInPlainWords() {
        assertEquals("Knight to f3", describe(ChessGame.START_FEN, "g1f3"))
        assertEquals("Pawn takes pawn on d5", describe("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", "e4d5"))
        assertEquals("Castle king side", describe("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1"))
        assertEquals("Castle queen side", describe("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1c1"))
        assertEquals("Rook to a8, checkmate", describe("6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1", "a1a8"))
        assertEquals("Pawn to e8, becomes a queen, check", describe("k7/4P3/8/8/8/8/8/K7 w - - 0 1", "e7e8q"))
        assertEquals("Pawn takes pawn on d6", describe("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6"))
        assertEquals("white knight", Piece(Side.WHITE, PieceType.KNIGHT).spoken())
    }
}
