package com.wtcb.myprompter

import java.text.SimpleDateFormat
import java.util.*

class PointsManager(private val prefsHelper: PrefsHelper) {

    companion object {
        const val BASE_WORD_LIMIT = 500
        const val POINTS_PER_VIDEO = 20
        const val MAX_VIDEOS_PER_DAY = 10
        const val POINTS_FOR_100_WORDS = 20
        const val WORDS_PER_PURCHASE = 100
    }

    fun getCurrentWordLimit(): Int {
        return BASE_WORD_LIMIT + (prefsHelper.wordLimitExtension * WORDS_PER_PURCHASE)
    }

    fun getAvailablePoints(): Int {
        return prefsHelper.userPoints
    }

    fun canWatchMoreVideos(): Boolean {
        checkAndResetDailyLimit()
        return prefsHelper.videosWatchedToday < MAX_VIDEOS_PER_DAY
    }

    fun getVideosWatchedToday(): Int {
        checkAndResetDailyLimit()
        return prefsHelper.videosWatchedToday
    }

    fun getVideosRemainingToday(): Int {
        return MAX_VIDEOS_PER_DAY - getVideosWatchedToday()
    }

    fun addPointsForVideo() {
        checkAndResetDailyLimit()

        if (canWatchMoreVideos()) {
            prefsHelper.userPoints += POINTS_PER_VIDEO
            prefsHelper.videosWatchedToday += 1
            updateLastWatchDate()
        }
    }

    fun canExtendWordLimit(additionalWords: Int): Boolean {
        val extensionsNeeded = (additionalWords + WORDS_PER_PURCHASE - 1) / WORDS_PER_PURCHASE
        val pointsNeeded = extensionsNeeded * POINTS_FOR_100_WORDS
        return prefsHelper.userPoints >= pointsNeeded
    }

    fun extendWordLimit(additionalWords: Int): Boolean {
        val extensionsNeeded = (additionalWords + WORDS_PER_PURCHASE - 1) / WORDS_PER_PURCHASE
        val pointsNeeded = extensionsNeeded * POINTS_FOR_100_WORDS

        if (prefsHelper.userPoints >= pointsNeeded) {
            prefsHelper.userPoints -= pointsNeeded
            prefsHelper.wordLimitExtension += extensionsNeeded
            return true
        }
        return false
    }

    fun getWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun isWithinLimit(text: String): Boolean {
        return getWordCount(text) <= getCurrentWordLimit()
    }

    fun getWordsOverLimit(text: String): Int {
        val wordCount = getWordCount(text)
        val limit = getCurrentWordLimit()
        return if (wordCount > limit) wordCount - limit else 0
    }

    private fun checkAndResetDailyLimit() {
        val today = getCurrentDate()
        if (prefsHelper.lastVideoWatchDate != today) {
            prefsHelper.videosWatchedToday = 0
            prefsHelper.lastVideoWatchDate = today
        }
    }

    private fun updateLastWatchDate() {
        prefsHelper.lastVideoWatchDate = getCurrentDate()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}