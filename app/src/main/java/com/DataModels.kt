package com.wtcb.myprompter

import java.text.SimpleDateFormat
import java.util.*

data class Script(
    val id: String,
    val title: String,
    val content: String,
    val dateCreated: Long,
    val dateModified: Long = dateCreated
) {
    fun getPreview(maxLength: Int = 100): String {
        return if (content.length > maxLength) {
            content.substring(0, maxLength) + "..."
        } else {
            content
        }
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(dateCreated))
    }
}

data class VideoRecording(
    val id: String,
    val title: String,
    val uri: String,
    val dateCreated: Long,
    val duration: Long,
    val size: Long
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(dateCreated))
    }

    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}