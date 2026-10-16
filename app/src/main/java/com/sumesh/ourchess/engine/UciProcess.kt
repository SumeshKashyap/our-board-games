package com.sumesh.ourchess.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import kotlin.coroutines.coroutineContext

/** How hard the engine tries. Exactly one of [depth] or [moveTimeMs] limits the search. */
data class SearchSettings(
    val skill: Int,
    val depth: Int? = null,
    val moveTimeMs: Int? = null,
)

/**
 * A running UCI chess engine (Stockfish), started on first use and shared by all callers.
 * Searches run one at a time; if the process dies it is restarted on the next search.
 */
class UciProcess(private val binaryPath: String) : Closeable {

    private val mutex = Mutex()
    private var process: Process? = null
    private lateinit var writer: BufferedWriter
    private lateinit var reader: BufferedReader

    /** Returns the engine's move for [fen] in UCI notation, or null if there is none. */
    suspend fun bestMove(fen: String, settings: SearchSettings): String? {
        val move = mutex.withLock {
            // Searches are short; let one finish even if the caller gives up, so the engine stays in sync.
            withContext(Dispatchers.IO + NonCancellable) { search(fen, settings) }
        }
        coroutineContext.ensureActive()
        return move
    }

    private fun search(fen: String, s: SearchSettings): String? {
        try {
            ensureStarted()
            send("setoption name Skill Level value ${s.skill}")
            send("position fen $fen")
            send(if (s.moveTimeMs != null) "go movetime ${s.moveTimeMs}" else "go depth ${s.depth ?: 1}")
            while (true) {
                val line = reader.readLine() ?: throw IllegalStateException("engine exited")
                if (line.startsWith("bestmove")) {
                    return line.split(' ').getOrNull(1)?.takeUnless { it == "(none)" }
                }
            }
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    private fun ensureStarted() {
        if (process?.isAlive == true) return
        val p = ProcessBuilder(binaryPath).redirectErrorStream(true).start()
        process = p
        writer = p.outputStream.bufferedWriter()
        reader = p.inputStream.bufferedReader()
        send("uci")
        waitFor("uciok")
        send("setoption name Threads value 1")
        send("setoption name Hash value 32")
        send("isready")
        waitFor("readyok")
    }

    private fun send(command: String) {
        writer.write(command)
        writer.newLine()
        writer.flush()
    }

    private fun waitFor(token: String) {
        while (true) {
            val line = reader.readLine() ?: throw IllegalStateException("engine exited before '$token'")
            if (line.startsWith(token)) return
        }
    }

    override fun close() {
        process?.let { p ->
            runCatching { send("quit") }
            p.destroy()
        }
        process = null
    }
}
