package com.sumesh.ourchess.data

import org.json.JSONObject

enum class GameKind(val label: String) { CHESS("Chess"), CHECKERS("Checkers"), LUDO("Ludo") }

enum class Outcome { WON, DRAWN, LOST }

/** Results for one game at one level. */
data class Record(val won: Int = 0, val drawn: Int = 0, val lost: Int = 0) {
    val played: Int get() = won + drawn + lost

    fun add(outcome: Outcome) = when (outcome) {
        Outcome.WON -> copy(won = won + 1)
        Outcome.DRAWN -> copy(drawn = drawn + 1)
        Outcome.LOST -> copy(lost = lost + 1)
    }

    operator fun plus(other: Record) = Record(won + other.won, drawn + other.drawn, lost + other.lost)
}

/** A player's results, by game and level. */
data class Stats(val records: Map<String, Record> = emptyMap()) {

    fun record(game: GameKind, level: Int): Record = records[key(game, level)] ?: Record()

    fun total(game: GameKind): Record = (1..5).map { record(game, it) }.fold(Record(), Record::plus)

    fun add(game: GameKind, level: Int, outcome: Outcome): Stats =
        Stats(records + (key(game, level) to record(game, level).add(outcome)))

    fun toJson(): JSONObject = JSONObject().apply {
        records.forEach { (k, r) -> put(k, JSONObject().put("won", r.won).put("drawn", r.drawn).put("lost", r.lost)) }
    }

    companion object {
        private fun key(game: GameKind, level: Int) = "${game.name}:$level"

        fun fromJson(json: JSONObject?): Stats {
            if (json == null) return Stats()
            return Stats(json.keys().asSequence().associateWith { k ->
                val r = json.getJSONObject(k)
                Record(r.optInt("won"), r.optInt("drawn"), r.optInt("lost"))
            })
        }
    }
}
