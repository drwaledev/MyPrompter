package com.wtcb.myprompter

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var previewView: PreviewView
    private lateinit var scrollView: ScrollView
    private lateinit var teleprompterText: EditText
    private lateinit var speedDisplay: TextView
    private lateinit var countdownOverlay: TextView
    private lateinit var overlayView: View
    private lateinit var recordingDuration: TextView
    private lateinit var pointsDisplay: LinearLayout
    private lateinit var pointsText: TextView
    private lateinit var wordLimitText: TextView
    private lateinit var btnMenuToggle: MaterialButton
    private lateinit var btnCameraToggle: MaterialButton
    private lateinit var btnDecreaseSpeed: MaterialButton
    private lateinit var btnIncreaseSpeed: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var btnRecord: MaterialButton
    private lateinit var btnScripts: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var scrollSpeed = 2f
    private var isScrolling = false
    private var isRecording = false
    private var scrollPosition = 0f
    private var scrollRunnable: Runnable? = null
    private var countdownRunnable: Runnable? = null
    private var durationRunnable: Runnable? = null
    private var recordingStartTime = 0L

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: androidx.camera.video.Recording? = null
    private var savedVideoUri: Uri? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var storageManager: StorageManager
    private lateinit var pointsManager: PointsManager
    private lateinit var adManager: AdManager
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private var useFrontCamera = false

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadTextFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsHelper = PrefsHelper(this)
        storageManager = StorageManager(this)
        pointsManager = PointsManager(prefsHelper)
        adManager = AdManager(this)
        firebaseAuthManager = FirebaseAuthManager(this)

        MobileAds.initialize(this) {}
        setupAdCallbacks()
        setupFirebaseCallbacks()

        initializeViews()
        setupDrawer()
        setupButtons()
        setupTextWatcher()

        cameraExecutor = Executors.newSingleThreadExecutor()

        loadLastScript()
        applySettings()

        // Reset local points if not signed in
        resetPointsIfNotSignedIn()

        loadPointsFromFirebase()
        updatePointsDisplay()
        updateDrawerHeader()

        adManager.loadRewardedAd()

        if (!hasRequiredPermissions()) {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        } else {
            startCamera()
        }
    }

    private fun resetPointsIfNotSignedIn() {
        if (!firebaseAuthManager.isSignedIn()) {
            // Clear all local points data when not signed in
            prefsHelper.userPoints = 0
            prefsHelper.wordLimitExtension = 0
            prefsHelper.videosWatchedToday = 0
            prefsHelper.lastVideoWatchDate = ""
        }
    }

    private fun setupFirebaseCallbacks() {
        firebaseAuthManager.onSignInSuccess = { user ->
            Toast.makeText(this, "Signed in as ${user.email}", Toast.LENGTH_SHORT).show()
            loadPointsFromFirebase()
            updateDrawerHeader()
        }

        firebaseAuthManager.onSignInFailure = { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPointsFromFirebase() {
        if (firebaseAuthManager.isSignedIn()) {
            firebaseAuthManager.loadPointsFromFirestore { points, wordLimit, videosWatched, lastDate ->
                prefsHelper.userPoints = points
                prefsHelper.wordLimitExtension = wordLimit
                prefsHelper.videosWatchedToday = videosWatched
                prefsHelper.lastVideoWatchDate = lastDate
                updatePointsDisplay()
            }
        } else {
            // Not signed in - reset to zero
            prefsHelper.userPoints = 0
            prefsHelper.wordLimitExtension = 0
            prefsHelper.videosWatchedToday = 0
            prefsHelper.lastVideoWatchDate = ""
            updatePointsDisplay()
        }
    }

    private fun syncPointsToFirebase() {
        if (firebaseAuthManager.isSignedIn()) {
            firebaseAuthManager.syncPointsToFirestore(
                prefsHelper.userPoints,
                prefsHelper.wordLimitExtension,
                prefsHelper.videosWatchedToday,
                prefsHelper.lastVideoWatchDate
            )
        }
    }

    private fun updateDrawerHeader() {
        val headerView = navigationView.getHeaderView(0)
        val headerTitle = headerView.findViewById<TextView>(R.id.headerTitle)
        val headerSubtitle = headerView.findViewById<TextView>(R.id.headerSubtitle)

        val user = firebaseAuthManager.getCurrentUser()
        if (user != null) {
            headerTitle?.text = user.displayName ?: "MyPrompter"
            headerSubtitle?.text = user.email ?: "Professional Teleprompter"
        } else {
            headerTitle?.text = "MyPrompter"
            headerSubtitle?.text = "Professional Teleprompter"
        }
    }

    private fun setupAdCallbacks() {
        adManager.onAdLoaded = {
            runOnUiThread {
                Toast.makeText(this, "✅ Ad ready!", Toast.LENGTH_SHORT).show()
            }
        }

        adManager.onAdFailedToLoad = { error ->
            runOnUiThread {
                Toast.makeText(this, "❌ Ad failed to load", Toast.LENGTH_SHORT).show()
            }
        }

        adManager.onAdShown = {
            runOnUiThread {
                Toast.makeText(this, "📺 Showing ad...", Toast.LENGTH_SHORT).show()
            }
        }

        adManager.onAdDismissed = {
            runOnUiThread {
                Toast.makeText(this, "Ad closed", Toast.LENGTH_SHORT).show()
            }
        }

        adManager.onAdFailedToShow = { error ->
            runOnUiThread {
                Toast.makeText(this, "❌ Ad failed to show", Toast.LENGTH_SHORT).show()
                adManager.loadRewardedAd()
            }
        }

        adManager.onUserEarnedReward = { amount ->
            runOnUiThread {
                // Check if user is signed in before awarding points
                if (firebaseAuthManager.isSignedIn()) {
                    pointsManager.addPointsForVideo()
                    updatePointsDisplay()
                    syncPointsToFirebase()

                    val remaining = pointsManager.getVideosRemainingToday()
                    Toast.makeText(
                        this,
                        "🎉 +$amount Points earned!\n$remaining videos remaining today",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "⚠️ Please sign in first to earn points!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        previewView = findViewById(R.id.cameraPreview)
        scrollView = findViewById(R.id.scrollView)
        teleprompterText = findViewById(R.id.teleprompterText)
        speedDisplay = findViewById(R.id.speedDisplay)
        countdownOverlay = findViewById(R.id.countdownOverlay)
        overlayView = findViewById(R.id.overlayView)
        recordingDuration = findViewById(R.id.recordingDuration)
        pointsDisplay = findViewById(R.id.pointsDisplay)
        pointsText = findViewById(R.id.pointsText)
        wordLimitText = findViewById(R.id.wordLimitText)
        btnMenuToggle = findViewById(R.id.btnMenuToggle)
        btnCameraToggle = findViewById(R.id.btnCameraToggle)
        btnDecreaseSpeed = findViewById(R.id.btnDecreaseSpeed)
        btnIncreaseSpeed = findViewById(R.id.btnIncreaseSpeed)
        btnReset = findViewById(R.id.btnReset)
        btnRecord = findViewById(R.id.btnRecord)
        btnScripts = findViewById(R.id.btnScripts)
    }

    private fun setupDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupButtons() {
        btnMenuToggle.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnCameraToggle.setOnClickListener {
            toggleCamera()
        }

        btnDecreaseSpeed.setOnClickListener {
            if (scrollSpeed > 1f) {
                scrollSpeed -= 1f
                prefsHelper.scrollSpeed = scrollSpeed
                updateSpeedDisplay()
                Toast.makeText(this, "Speed: ${scrollSpeed.toInt()}", Toast.LENGTH_SHORT).show()
            }
        }

        btnIncreaseSpeed.setOnClickListener {
            if (scrollSpeed < 20f) {
                scrollSpeed += 1f
                prefsHelper.scrollSpeed = scrollSpeed
                updateSpeedDisplay()
                Toast.makeText(this, "Speed: ${scrollSpeed.toInt()}", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {
            scrollPosition = 0f
            scrollView.scrollTo(0, 0)
            if (isScrolling) {
                stopScrolling()
            }
            Toast.makeText(this, "Reset", Toast.LENGTH_SHORT).show()
        }

        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                checkWordLimitAndRecord()
            }
        }

        btnScripts.setOnClickListener {
            showScriptsOptions()
        }

        teleprompterText.setOnLongClickListener {
            showScriptsOptions()
            true
        }

        pointsDisplay.setOnClickListener {
            showPointsInfo()
        }
    }

    private fun setupTextWatcher() {
        teleprompterText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateWordLimitDisplay()
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_account -> showAccountDialog()
            R.id.menu_font_size -> showFontSizeDialog()
            R.id.menu_text_color -> showTextColorDialog()
            R.id.menu_text_opacity -> showTextOpacityDialog()
            R.id.menu_countdown -> showCountdownDialog()
            R.id.menu_camera -> toggleCamera()
            R.id.menu_video_quality -> showVideoQualityDialog()
            R.id.menu_audio -> toggleAudio()
            R.id.menu_earn_points -> showEarnPointsDialog()
            R.id.menu_extend_limit -> showExtendLimitDialog()
            R.id.menu_developer -> showDeveloperInfo()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showAccountDialog() {
        val user = firebaseAuthManager.getCurrentUser()

        if (user != null) {
            // User is signed in - show account info with sign out option
            MaterialAlertDialogBuilder(this)
                .setTitle("Account & Sync")
                .setMessage(
                    "✅ Signed In\n\n" +
                            "Name: ${user.displayName ?: "N/A"}\n" +
                            "Email: ${user.email}\n\n" +
                            "Your points are syncing across all your devices!"
                )
                .setPositiveButton("Sign Out") { _, _ ->
                    confirmSignOut()
                }
                .setNeutralButton("Sync Now") { _, _ ->
                    syncPointsToFirebase()
                    Toast.makeText(this, "✅ Synced successfully!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
        } else {
            // User is NOT signed in - prompt to sign in
            MaterialAlertDialogBuilder(this)
                .setTitle("Account & Sync")
                .setMessage(
                    "⚠️ Not Signed In\n\n" +
                            "Sign in with Google to:\n" +
                            "• Earn and sync points across devices\n" +
                            "• Keep your data safe in the cloud\n" +
                            "• Never lose your progress\n" +
                            "• Unlock point-based features\n\n" +
                            "Would you like to sign in now?"
                )
                .setPositiveButton("Sign In") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?\n\nYour points are saved in the cloud and will be restored when you sign in again.")
            .setPositiveButton("Sign Out") { _, _ ->
                firebaseAuthManager.signOut()

                // Clear local points data after sign out
                prefsHelper.userPoints = 0
                prefsHelper.wordLimitExtension = 0
                prefsHelper.videosWatchedToday = 0
                prefsHelper.lastVideoWatchDate = ""

                updateDrawerHeader()
                updatePointsDisplay()
                Toast.makeText(this, "✅ Signed out successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFontSizeDialog() {
        val sizes = arrayOf("Tiny (12sp)", "Small (20sp)", "Medium (32sp)", "Large (48sp)", "Huge (64sp)")
        val values = arrayOf(12, 20, 32, 48, 64)
        val currentIndex = when (prefsHelper.fontSize) {
            12 -> 0
            20 -> 1
            32 -> 2
            48 -> 3
            64 -> 4
            else -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Font Size")
            .setSingleChoiceItems(sizes, currentIndex) { dialog, which ->
                prefsHelper.fontSize = values[which]
                teleprompterText.textSize = values[which].toFloat()
                Toast.makeText(this, sizes[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showTextColorDialog() {
        val colors = arrayOf("White", "Yellow", "Green", "Blue", "Red")
        val colorValues = arrayOf(
            0xFFFFFFFF.toInt(),
            0xFFFFEB3B.toInt(),
            0xFF4CAF50.toInt(),
            0xFF2196F3.toInt(),
            0xFFF44336.toInt()
        )

        val currentIndex = colorValues.indexOf(prefsHelper.textColor)

        MaterialAlertDialogBuilder(this)
            .setTitle("Text Color")
            .setSingleChoiceItems(colors, currentIndex) { dialog, which ->
                prefsHelper.textColor = colorValues[which]
                applyTextColor()
                Toast.makeText(this, colors[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showTextOpacityDialog() {
        val levels = arrayOf("10%", "25%", "50%", "75%", "100%")
        val values = arrayOf(26, 64, 128, 191, 255)

        val currentOpacity = prefsHelper.textOpacity
        val currentIndex = when {
            currentOpacity < 45 -> 0
            currentOpacity < 96 -> 1
            currentOpacity < 160 -> 2
            currentOpacity < 224 -> 3
            else -> 4
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Text Opacity")
            .setSingleChoiceItems(levels, currentIndex) { dialog, which ->
                prefsHelper.textOpacity = values[which]
                applyTextColor()
                Toast.makeText(this, "Opacity: ${levels[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showCountdownDialog() {
        val options = arrayOf("No countdown", "3 seconds", "5 seconds", "10 seconds")
        val values = arrayOf(0, 3, 5, 10)
        val currentIndex = when (prefsHelper.countdownSeconds) {
            0 -> 0
            3 -> 1
            5 -> 2
            10 -> 3
            else -> 1
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Countdown Timer")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                prefsHelper.countdownSeconds = values[which]
                Toast.makeText(this, options[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showVideoQualityDialog() {
        val qualities = arrayOf("HIGHEST", "UHD (4K)", "FHD (1080p)", "HD (720p)", "SD (480p)")
        val qualityValues = arrayOf("HIGHEST", "UHD", "FHD", "HD", "SD")
        val currentIndex = qualityValues.indexOf(prefsHelper.videoQuality)

        MaterialAlertDialogBuilder(this)
            .setTitle("Video Quality")
            .setSingleChoiceItems(qualities, currentIndex) { dialog, which ->
                prefsHelper.videoQuality = qualityValues[which]
                Toast.makeText(this, qualities[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun toggleAudio() {
        prefsHelper.recordWithAudio = !prefsHelper.recordWithAudio
        Toast.makeText(
            this,
            if (prefsHelper.recordWithAudio) "Audio: ON" else "Audio: OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEarnPointsDialog() {
        // Check if user is signed in first
        if (!firebaseAuthManager.isSignedIn()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Sign In Required")
                .setMessage("⚠️ You need to sign in to earn points!\n\nSign in with Google to:\n• Watch ads and earn points\n• Sync points across devices\n• Unlock premium features")
                .setPositiveButton("Sign In") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val remaining = pointsManager.getVideosRemainingToday()
        val watched = pointsManager.getVideosWatchedToday()

        if (remaining <= 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Daily Limit Reached")
                .setMessage("You've watched all 10 videos today ($watched/10).\n\nCome back tomorrow to earn more points!")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Earn Points")
            .setMessage(
                "Watch a rewarded video to earn 20 points!\n\n" +
                        "Videos watched today: $watched/10\n" +
                        "Remaining: $remaining\n\n" +
                        "Ad status: ${if (adManager.isAdReady()) "✅ Ready" else if (adManager.isLoading()) "⏳ Loading..." else "❌ Not loaded"}"
            )
            .setPositiveButton("Watch Video") { _, _ ->
                if (adManager.isAdReady()) {
                    adManager.showRewardedAd()
                } else {
                    Toast.makeText(this, "⏳ Loading ad, please wait...", Toast.LENGTH_SHORT).show()
                    adManager.loadRewardedAd()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExtendLimitDialog() {
        // Check if user is signed in first
        if (!firebaseAuthManager.isSignedIn()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Sign In Required")
                .setMessage("⚠️ You need to sign in to use points!\n\nSign in to:\n• Earn points by watching ads\n• Extend your word limit\n• Save progress in the cloud")
                .setPositiveButton("Sign In") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val currentLimit = pointsManager.getCurrentWordLimit()
        val points = pointsManager.getAvailablePoints()

        MaterialAlertDialogBuilder(this)
            .setTitle("Extend Word Limit")
            .setMessage(
                "Current limit: $currentLimit words\n" +
                        "Available points: $points\n\n" +
                        "Cost: 20 points = +100 words\n\n" +
                        "How many words would you like to add?"
            )
            .setItems(arrayOf("+100 words (20 pts)", "+200 words (40 pts)", "+500 words (100 pts)")) { _, which ->
                val wordsToAdd = when (which) {
                    0 -> 100
                    1 -> 200
                    2 -> 500
                    else -> 0
                }
                attemptExtendLimit(wordsToAdd)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun attemptExtendLimit(words: Int) {
        if (pointsManager.extendWordLimit(words)) {
            updatePointsDisplay()
            syncPointsToFirebase()
            val newLimit = pointsManager.getCurrentWordLimit()
            Toast.makeText(
                this,
                "✅ Word limit extended to $newLimit words!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val needed = (words / 100) * 20
            val have = pointsManager.getAvailablePoints()
            Toast.makeText(
                this,
                "❌ Not enough points! Need $needed, have $have",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showPointsInfo() {
        val points = pointsManager.getAvailablePoints()
        val limit = pointsManager.getCurrentWordLimit()
        val remaining = pointsManager.getVideosRemainingToday()
        val user = firebaseAuthManager.getCurrentUser()

        val message = if (user != null) {
            "Signed in as: ${user.email}\n\n" +
                    "⭐ Points: $points\n" +
                    "📝 Word Limit: $limit words\n" +
                    "🎬 Videos remaining today: $remaining/10\n\n" +
                    "• Watch ads to earn 20 points\n" +
                    "• Use points to extend word limit\n" +
                    "• 20 points = +100 words\n" +
                    "• Your points sync across devices!"
        } else {
            "⚠️ Not Signed In\n\n" +
                    "📝 Word Limit: $limit words (base)\n\n" +
                    "Sign in with Google to:\n" +
                    "• Earn points by watching ads\n" +
                    "• Extend your word limit\n" +
                    "• Sync progress across devices\n\n" +
                    "Tap 'Sign In' below to get started!"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Points & Word Limit")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton(if (user != null) "Sign Out" else "Sign In") { _, _ ->
                if (user != null) {
                    confirmSignOut()
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
            .show()
    }

    private fun showDeveloperInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About Developer")
            .setMessage("MyPrompter v1.0\n\nDeveloped with ❤️ by WTC Business Solutions 07033924384\n\n© 2026 All rights reserved")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleCamera() {
        useFrontCamera = !useFrontCamera
        prefsHelper.useFrontCamera = useFrontCamera
        startCamera()
        Toast.makeText(
            this,
            if (useFrontCamera) "Front Camera" else "Audio: OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkWordLimitAndRecord() {
        val text = teleprompterText.text.toString()
        val wordCount = pointsManager.getWordCount(text)
        val limit = pointsManager.getCurrentWordLimit()

        if (wordCount > limit) {
            val over = wordCount - limit
            val extensionsNeeded = (over + 99) / 100
            val pointsNeeded = extensionsNeeded * 20

            // Check if user is signed in
            if (!firebaseAuthManager.isSignedIn()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Sign In Required")
                    .setMessage(
                        "Your script has $wordCount words but your limit is $limit words.\n\n" +
                                "⚠️ You need to sign in to extend your word limit!\n\n" +
                                "Sign in to earn points and unlock more words."
                    )
                    .setPositiveButton("Sign In") { _, _ ->
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Word Limit Exceeded")
                .setMessage(
                    "Your script has $wordCount words but your limit is $limit words.\n\n" +
                            "You need $pointsNeeded points to extend by ${extensionsNeeded * 100} words.\n\n" +
                            "Available points: ${pointsManager.getAvailablePoints()}"
                )
                .setPositiveButton("Extend Limit") { _, _ ->
                    if (pointsManager.extendWordLimit(over)) {
                        updatePointsDisplay()
                        syncPointsToFirebase()
                        Toast.makeText(this, "✅ Limit extended!", Toast.LENGTH_SHORT).show()
                        startRecordingWithCountdown()
                    } else {
                        Toast.makeText(this, "❌ Not enough points", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("Earn Points") { _, _ ->
                    showEarnPointsDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            startRecordingWithCountdown()
        }
    }

    private fun startRecordingWithCountdown() {
        val countdownSeconds = prefsHelper.countdownSeconds

        if (countdownSeconds == 0) {
            startRecording()
            return
        }

        var remainingSeconds = countdownSeconds
        countdownOverlay.visibility = View.VISIBLE
        countdownOverlay.text = "$remainingSeconds"

        countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingSeconds > 0) {
                    countdownOverlay.text = "$remainingSeconds"
                    remainingSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    countdownOverlay.visibility = View.GONE
                    startRecording()
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    private fun showScriptsOptions() {
        val options = arrayOf(
            "Import from File",
            "Import from URL",
            "View Scripts",
            "Save Script"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Scripts")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickFileLauncher.launch("text/*")
                    1 -> showUrlImportDialog()
                    2 -> openScriptsActivity()
                    3 -> saveCurrentScript()
                }
            }
            .show()
    }

    private fun showUrlImportDialog() {
        val input = EditText(this)
        input.hint = "Enter URL"

        MaterialAlertDialogBuilder(this)
            .setTitle("Import from URL")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val url = input.text.toString()
                if (url.isNotEmpty()) loadTextFromUrl(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openScriptsActivity() {
        val intent = Intent(this, ScriptsActivity::class.java)
        startActivityForResult(intent, REQUEST_LOAD_SCRIPT)
    }

    private fun saveCurrentScript() {
        val scriptText = teleprompterText.text.toString()
        if (scriptText.isEmpty()) {
            Toast.makeText(this, "Script is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this)
        val defaultTitle = "Script_${SimpleDateFormat("MMM_dd_HH_mm", Locale.US).format(Date())}"
        input.setText(defaultTitle)

        MaterialAlertDialogBuilder(this)
            .setTitle("Save Script")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text.toString().ifEmpty { defaultTitle }
                val script = Script(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    content = scriptText,
                    dateCreated = System.currentTimeMillis()
                )
                storageManager.saveScript(script)
                Toast.makeText(this, "Saved: $title", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadTextFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = BufferedReader(InputStreamReader(inputStream)).readText()
                teleprompterText.setText(text)
                Toast.makeText(this, "Imported", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTextFromUrl(url: String) {
        Thread {
            try {
                val text = java.net.URL(url).readText()
                runOnUiThread {
                    teleprompterText.setText(text)
                    Toast.makeText(this, "Imported from URL", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadLastScript() {
        val lastScript = getSharedPreferences("MyPrompterPrefs", MODE_PRIVATE)
            .getString("lastScript", getString(R.string.default_script))
        teleprompterText.setText(lastScript)
    }

    private fun applySettings() {
        scrollSpeed = prefsHelper.scrollSpeed
        useFrontCamera = prefsHelper.useFrontCamera
        teleprompterText.textSize = prefsHelper.fontSize.toFloat()
        applyTextColor()
        overlayView.setBackgroundColor(prefsHelper.backgroundColor)
        updateSpeedDisplay()
    }

    private fun applyTextColor() {
        val baseColor = prefsHelper.textColor
        val opacity = prefsHelper.textOpacity

        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)

        val colorWithOpacity = Color.argb(opacity, red, green, blue)
        teleprompterText.setTextColor(colorWithOpacity)
    }

    private fun updateSpeedDisplay() {
        speedDisplay.text = "${scrollSpeed.toInt()}"
    }

    private fun updatePointsDisplay() {
        val points = pointsManager.getAvailablePoints()
        val limit = pointsManager.getCurrentWordLimit()
        pointsText.text = "⭐ $points Points"
        wordLimitText.text = "📝 $limit words"
    }

    private fun updateWordLimitDisplay() {
        val wordCount = pointsManager.getWordCount(teleprompterText.text.toString())
        val limit = pointsManager.getCurrentWordLimit()

        if (wordCount > limit) {
            wordLimitText.setTextColor(Color.RED)
            wordLimitText.text = "📝 $wordCount/$limit ⚠️"
        } else {
            wordLimitText.setTextColor(Color.WHITE)
            wordLimitText.text = "📝 $limit words"
        }
    }

    private fun startScrolling() {
        if (isScrolling) return

        isScrolling = true
        scrollRunnable = object : Runnable {
            override fun run() {
                if (isScrolling) {
                    scrollPosition += scrollSpeed
                    scrollView.scrollTo(0, scrollPosition.toInt())

                    val maxScroll = teleprompterText.height - scrollView.height
                    if (scrollPosition >= maxScroll && maxScroll > 0) {
                        stopScrolling()
                        return
                    }

                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(scrollRunnable!!)
    }

    private fun stopScrolling() {
        isScrolling = false
        scrollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: run {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Teleprompter_$name")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyPrompter")
            }
        }

        val outputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val preparedRecording = videoCapture.output.prepareRecording(this, outputOptions)

        activeRecording = if (prefsHelper.recordWithAudio) {
            preparedRecording.withAudioEnabled()
        } else {
            preparedRecording
        }.start(ContextCompat.getMainExecutor(this)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    runOnUiThread {
                        isRecording = true
                        btnRecord.text = "⏹"
                        btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red_dark)

                        scrollPosition = 0f
                        scrollView.scrollTo(0, 0)
                        startScrolling()

                        recordingStartTime = SystemClock.elapsedRealtime()
                        recordingDuration.visibility = View.VISIBLE
                        updateDuration()

                        Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    runOnUiThread {
                        isRecording = false
                        stopScrolling()
                        recordingDuration.visibility = View.GONE
                        durationRunnable?.let { handler.removeCallbacks(it) }

                        if (!event.hasError()) {
                            savedVideoUri = event.outputResults.outputUri

                            // Save recording metadata
                            val videoRecording = VideoRecording(
                                id = System.currentTimeMillis().toString(),
                                title = name,
                                uri = savedVideoUri.toString(),
                                dateCreated = System.currentTimeMillis(),
                                duration = 0,
                                size = 0
                            )
                            storageManager.saveRecording(videoRecording)

                            // Open preview screen
                            val intent = Intent(this, VideoPreviewActivity::class.java)
                            intent.putExtra("VIDEO_URI", savedVideoUri)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "❌ Recording failed", Toast.LENGTH_SHORT).show()
                        }

                        btnRecord.text = "⬤"
                        btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
                        activeRecording = null
                    }
                }
            }
        }
    }

    private fun updateDuration() {
        durationRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
                    val seconds = (elapsed / 1000).toInt()
                    val minutes = seconds / 60
                    val secs = seconds % 60
                    recordingDuration.text = "⬤ %02d:%02d".format(minutes, secs)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(durationRunnable!!)
    }

    private fun stopRecording() {
        activeRecording?.stop()
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        cameraProvider?.unbindAll()

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val qualitySelector = when (prefsHelper.videoQuality) {
            "UHD" -> QualitySelector.from(Quality.UHD)
            "FHD" -> QualitySelector.from(Quality.FHD)
            "HD" -> QualitySelector.from(Quality.HD)
            "SD" -> QualitySelector.from(Quality.SD)
            else -> QualitySelector.from(Quality.HIGHEST)
        }

        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, videoCapture)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOAD_SCRIPT && resultCode == RESULT_OK) {
            val scriptContent = data?.getStringExtra("SCRIPT_CONTENT")
            if (scriptContent != null) {
                teleprompterText.setText(scriptContent)
                scrollPosition = 0f
                scrollView.scrollTo(0, 0)
                Toast.makeText(this, "Script loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        activeRecording?.stop()
        stopScrolling()
        countdownRunnable?.let { handler.removeCallbacks(it) }
        durationRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        applySettings()

        // Reset points if not signed in
        resetPointsIfNotSignedIn()

        // Load user's points from Firestore if signed in
        loadPointsFromFirebase()

        updatePointsDisplay()
        updateDrawerHeader()

        if (hasRequiredPermissions()) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isScrolling) stopScrolling()

        // Only sync if signed in
        if (firebaseAuthManager.isSignedIn()) {
            syncPointsToFirebase()
        }

        val script = teleprompterText.text.toString()
        getSharedPreferences("MyPrompterPrefs", MODE_PRIVATE)
            .edit()
            .putString("lastScript", script)
            .apply()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        private const val REQUEST_LOAD_SCRIPT = 1001
    }
}