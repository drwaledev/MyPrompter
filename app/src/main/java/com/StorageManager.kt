package com.wtcb.myprompter

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class StorageManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("MyPrompterStorage", Context.MODE_PRIVATE)

    // SCRIPTS MANAGEMENT
    fun saveScript(script: Script) {
        val scripts = getAllScripts().toMutableList()
        scripts.removeAll { it.id == script.id }
        scripts.add(0, script)

        val jsonArray = JSONArray()
        scripts.forEach { jsonArray.put(scriptToJson(it)) }

        prefs.edit().putString("scripts", jsonArray.toString()).apply()
    }

    fun getAllScripts(): List<Script> {
        val scriptsJson = prefs.getString("scripts", "[]") ?: "[]"
        val jsonArray = JSONArray(scriptsJson)
        val scripts = mutableListOf<Script>()

        for (i in 0 until jsonArray.length()) {
            scripts.add(jsonToScript(jsonArray.getJSONObject(i)))
        }

        return scripts.sortedByDescending { it.dateCreated }
    }

    fun getScript(id: String): Script? {
        return getAllScripts().find { it.id == id }
    }

    fun deleteScript(id: String) {
        val scripts = getAllScripts().toMutableList()
        scripts.removeAll { it.id == id }

        val jsonArray = JSONArray()
        scripts.forEach { jsonArray.put(scriptToJson(it)) }

        prefs.edit().putString("scripts", jsonArray.toString()).apply()
    }

    fun searchScripts(query: String): List<Script> {
        return getAllScripts().filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
        }
    }

    // RECORDINGS MANAGEMENT
    fun saveRecording(recording: VideoRecording) {
        val recordings = getAllRecordings().toMutableList()
        recordings.removeAll { it.id == recording.id }
        recordings.add(0, recording)

        val jsonArray = JSONArray()
        recordings.forEach { jsonArray.put(recordingToJson(it)) }

        prefs.edit().putString("recordings", jsonArray.toString()).apply()
    }

    fun getAllRecordings(): List<VideoRecording> {
        val recordingsJson = prefs.getString("recordings", "[]") ?: "[]"
        val jsonArray = JSONArray(recordingsJson)
        val recordings = mutableListOf<VideoRecording>()

        for (i in 0 until jsonArray.length()) {
            recordings.add(jsonToRecording(jsonArray.getJSONObject(i)))
        }

        return recordings.sortedByDescending { it.dateCreated }
    }

    fun deleteRecording(id: String) {
        val recordings = getAllRecordings().toMutableList()
        recordings.removeAll { it.id == id }

        val jsonArray = JSONArray()
        recordings.forEach { jsonArray.put(recordingToJson(it)) }

        prefs.edit().putString("recordings", jsonArray.toString()).apply()
    }

    fun searchRecordings(query: String): List<VideoRecording> {
        return getAllRecordings().filter {
            it.title.contains(query, ignoreCase = true)
        }
    }

    // JSON CONVERSION HELPERS
    private fun scriptToJson(script: Script): JSONObject {
        return JSONObject().apply {
            put("id", script.id)
            put("title", script.title)
            put("content", script.content)
            put("dateCreated", script.dateCreated)
            put("dateModified", script.dateModified)
        }
    }

    private fun jsonToScript(json: JSONObject): Script {
        return Script(
            id = json.getString("id"),
            title = json.getString("title"),
            content = json.getString("content"),
            dateCreated = json.getLong("dateCreated"),
            dateModified = json.optLong("dateModified", json.getLong("dateCreated"))
        )
    }

    private fun recordingToJson(recording: VideoRecording): JSONObject {
        return JSONObject().apply {
            put("id", recording.id)
            put("title", recording.title)
            put("uri", recording.uri)
            put("dateCreated", recording.dateCreated)
            put("duration", recording.duration)
            put("size", recording.size)
        }
    }

    private fun jsonToRecording(json: JSONObject): VideoRecording {
        return VideoRecording(
            id = json.getString("id"),
            title = json.getString("title"),
            uri = json.getString("uri"),
            dateCreated = json.getLong("dateCreated"),
            duration = json.optLong("duration", 0),
            size = json.optLong("size", 0)
        )
    }
}