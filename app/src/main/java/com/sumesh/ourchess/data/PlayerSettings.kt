package com.sumesh.ourchess.data

import org.json.JSONObject

enum class BoardTheme(val label: String) {
    GREEN("Green"),
    WOOD("Wood"),
    BLUE("Blue"),
    HIGH_CONTRAST("High contrast"),
}

/** How the app looks and behaves for one player. Defaults suit most people. */
data class PlayerSettings(
    val boardTheme: BoardTheme = BoardTheme.GREEN,
    /** Shows a letter (K, Q, R, B, N) on each piece. */
    val largePrint: Boolean = false,
    val showLegalMoves: Boolean = true,
    /** In games, asks "Play this move?" before each move. */
    val confirmMoves: Boolean = false,
    val autoQueen: Boolean = false,
    val sounds: Boolean = false,
    val vibration: Boolean = false,
    val speakMoves: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("boardTheme", boardTheme.name)
        .put("largePrint", largePrint)
        .put("showLegalMoves", showLegalMoves)
        .put("confirmMoves", confirmMoves)
        .put("autoQueen", autoQueen)
        .put("sounds", sounds)
        .put("vibration", vibration)
        .put("speakMoves", speakMoves)

    companion object {
        fun fromJson(json: JSONObject?): PlayerSettings {
            if (json == null) return PlayerSettings()
            val d = PlayerSettings()
            return PlayerSettings(
                boardTheme = runCatching { BoardTheme.valueOf(json.getString("boardTheme")) }.getOrDefault(d.boardTheme),
                largePrint = json.optBoolean("largePrint", d.largePrint),
                showLegalMoves = json.optBoolean("showLegalMoves", d.showLegalMoves),
                confirmMoves = json.optBoolean("confirmMoves", d.confirmMoves),
                autoQueen = json.optBoolean("autoQueen", d.autoQueen),
                sounds = json.optBoolean("sounds", d.sounds),
                vibration = json.optBoolean("vibration", d.vibration),
                speakMoves = json.optBoolean("speakMoves", d.speakMoves),
            )
        }
    }
}
