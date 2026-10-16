package com.sumesh.ourchess.puzzles

import android.content.res.AssetManager

/**
 * A puzzle from the Lichess puzzle database (CC0).
 * [fen] is the position before the opponent's move; [moves] starts with that opponent move,
 * followed by the solution (player, reply, player, ...), all in UCI notation.
 */
data class Puzzle(
    val id: String,
    val level: Int,
    val fen: String,
    val moves: List<String>,
    val rating: Int,
    val goal: String,
)

object PuzzleRepository {
    fun parse(csv: String): List<Puzzle> = csv.lineSequence()
        .drop(1)
        .filter { it.isNotBlank() }
        .map { line ->
            val f = line.split(',')
            Puzzle(f[0], f[1].toInt(), f[2], f[3].split(' '), f[4].toInt(), f[5])
        }
        .toList()

    fun load(assets: AssetManager): List<Puzzle> =
        assets.open("puzzles.csv").bufferedReader().use { parse(it.readText()) }
}
