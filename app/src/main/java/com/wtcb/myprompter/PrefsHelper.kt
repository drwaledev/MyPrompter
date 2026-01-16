package com.wtcb.myprompter

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("MyPrompterPrefs", Context.MODE_PRIVATE)

    var fontSize: Int
        get() = prefs.getInt("fontSize", 32)
        set(value) = prefs.edit().putInt("fontSize", value).apply()

    var textColor: Int
        get() = prefs.getInt("textColor", 0xFFFFFFFF.toInt())
        set(value) = prefs.edit().putInt("textColor", value).apply()

    var textOpacity: Int
        get() = prefs.getInt("textOpacity", 255)
        set(value) = prefs.edit().putInt("textOpacity", value).apply()

    var backgroundColor: Int
        get() = prefs.getInt("backgroundColor", 0x33000000.toInt())  // ← CHANGED: Was 0x80000000 (50% black), now 0x33000000 (20% black) for brighter video
        set(value) = prefs.edit().putInt("backgroundColor", value).apply()

    var useFrontCamera: Boolean
        get() = prefs.getBoolean("useFrontCamera", false)
        set(value) = prefs.edit().putBoolean("useFrontCamera", value).apply()

    var videoQuality: String
        get() = prefs.getString("videoQuality", "HIGHEST") ?: "HIGHEST"
        set(value) = prefs.edit().putString("videoQuality", value).apply()

    var scrollSpeed: Float
        get() = prefs.getFloat("scrollSpeed", 2f)
        set(value) = prefs.edit().putFloat("scrollSpeed", value).apply()

    var countdownSeconds: Int
        get() = prefs.getInt("countdownSeconds", 3)
        set(value) = prefs.edit().putInt("countdownSeconds", value).apply()

    var recordWithAudio: Boolean
        get() = prefs.getBoolean("recordWithAudio", true)
        set(value) = prefs.edit().putBoolean("recordWithAudio", value).apply()

    var userPoints: Int
        get() = prefs.getInt("userPoints", 0)
        set(value) = prefs.edit().putInt("userPoints", value).apply()

    var videosWatchedToday: Int
        get() = prefs.getInt("videosWatchedToday", 0)
        set(value) = prefs.edit().putInt("videosWatchedToday", value).apply()

    var lastVideoWatchDate: String
        get() = prefs.getString("lastVideoWatchDate", "") ?: ""
        set(value) = prefs.edit().putString("lastVideoWatchDate", value).apply()

    var wordLimitExtension: Int
        get() = prefs.getInt("wordLimitExtension", 0)
        set(value) = prefs.edit().putInt("wordLimitExtension", value).apply()
}