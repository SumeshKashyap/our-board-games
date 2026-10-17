package com.sumesh.ourchess.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Stores profiles as a small JSON file in the app's private storage. */
class ProfileRepository(private val file: File) {

    fun load(): List<Profile> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONObject(file.readText()).getJSONArray("profiles")
            (0 until arr.length()).map { arr.getJSONObject(it).toProfile() }
        }.getOrDefault(emptyList())
    }

    fun create(name: String): Profile {
        val profile = Profile(id = UUID.randomUUID().toString(), name = name.trim(), lastUsed = System.currentTimeMillis())
        save(load() + profile)
        return profile
    }

    fun update(profile: Profile) = save(load().map { if (it.id == profile.id) profile else it })

    fun delete(id: String) = save(load().filterNot { it.id == id })

    private fun save(profiles: List<Profile>) {
        val json = JSONObject().put("profiles", JSONArray(profiles.map { it.toJson() }))
        // Write to a temp file first so a crash mid-write never corrupts saved profiles.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.toString())
        tmp.renameTo(file)
    }

    private fun Profile.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("level", level)
        .put("lastUsed", lastUsed)
        .put("nextPuzzle", JSONObject(nextPuzzle.mapKeys { it.key.toString() }))
        .put("puzzlesSolved", puzzlesSolved)
        .put("lessonsDone", JSONArray(lessonsDone.toList()))
        .put("settings", settings.toJson())

    private fun JSONObject.toProfile(): Profile {
        val next = optJSONObject("nextPuzzle")
        val done = optJSONArray("lessonsDone")
        return Profile(
            id = getString("id"),
            name = getString("name"),
            level = optInt("level", 1),
            lastUsed = optLong("lastUsed", 0),
            nextPuzzle = next?.keys()?.asSequence()?.associate { it.toInt() to next.getInt(it) }.orEmpty(),
            puzzlesSolved = optInt("puzzlesSolved", 0),
            lessonsDone = done?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty(),
            settings = PlayerSettings.fromJson(optJSONObject("settings")),
        )
    }
}
