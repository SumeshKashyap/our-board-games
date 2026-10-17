package com.sumesh.ourchess.ludo

/** Who sits in a seat: a family member (with their profile, if any) or the computer. */
data class LudoSeat(val color: LudoColor, val name: String, val profileId: String?, val computer: Boolean)

/** A Ludo game in progress, saved as plain text so it can be resumed. */
data class LudoSaved(val seats: List<LudoSeat>, val state: LudoState) {

    fun encode(): String = buildString {
        appendLine(seats.joinToString("|") { s ->
            listOf(s.color.name, if (s.computer) "C" else "H", s.profileId.orEmpty(), s.name.replace("|", " ").replace("~", " ")).joinToString("~")
        })
        appendLine(state.players.joinToString(";") { c -> c.name + "=" + state.tokens.getValue(c).joinToString(",") })
        append(listOf(state.current, state.dice ?: 0, state.sixesInRow, state.winner?.name ?: "-").joinToString(","))
    }

    companion object {
        fun decode(text: String): LudoSaved? = runCatching {
            val lines = text.lines()
            val seats = lines[0].split("|").map { s ->
                val f = s.split("~")
                LudoSeat(LudoColor.valueOf(f[0]), f[3], f[2].ifEmpty { null }, computer = f[1] == "C")
            }
            val tokens = lines[1].split(";").associate { e ->
                LudoColor.valueOf(e.substringBefore('=')) to e.substringAfter('=').split(",").map(String::toInt)
            }
            val f = lines[2].split(",")
            val state = LudoState(
                players = seats.map { it.color },
                tokens = tokens,
                current = f[0].toInt(),
                dice = f[1].toInt().takeIf { it in 1..6 },
                sixesInRow = f[2].toInt(),
                winner = f[3].takeIf { it != "-" }?.let(LudoColor::valueOf),
            )
            LudoSaved(seats, state)
        }.getOrNull()
    }
}
