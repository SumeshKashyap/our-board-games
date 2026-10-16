package com.sumesh.ourchess.data

import com.sumesh.ourchess.game.Side
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A game in progress against the computer, from the standard starting position. */
data class SavedGame(val humanSide: Side, val moves: List<String>)

/** Keeps one game in progress per player and difficulty level, so closing the app never loses a game. */
class SavedGameStore(private val dir: File) {

    fun load(profileId: String, level: Int): SavedGame? {
        val file = file(profileId, level)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val moves = json.getJSONArray("moves")
            SavedGame(Side.valueOf(json.getString("humanSide")), (0 until moves.length()).map { moves.getString(it) })
        }.getOrNull()
    }

    fun save(profileId: String, level: Int, game: SavedGame) {
        dir.mkdirs()
        val json = JSONObject().put("humanSide", game.humanSide.name).put("moves", JSONArray(game.moves))
        val target = file(profileId, level)
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeText(json.toString())
        tmp.renameTo(target)
    }

    fun deleteAll(profileId: String) {
        dir.listFiles { f -> f.name.startsWith("$profileId-") }?.forEach { it.delete() }
    }

    private fun file(profileId: String, level: Int) = File(dir, "$profileId-L$level.json")
}
