package com.wtcb.myprompter

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import android.os.Handler
import android.os.Looper

class VideoPreviewActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var btnFullscreen: MaterialButton
    private lateinit var btnVolume: MaterialButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView

    // Save & Share buttons
    private lateinit var btnSaveToDevice: LinearLayout
    private lateinit var btnShare: LinearLayout
    private lateinit var switchSaveForLater: MaterialSwitch

    // Edit buttons
    private lateinit var btnAddCaptions: LinearLayout
    private lateinit var btnAddImage: LinearLayout
    private lateinit var btnAddText: LinearLayout
    private lateinit var btnReplaceBackground: LinearLayout
    private lateinit var btnResizeVideo: LinearLayout
    private lateinit var btnAddMusic: LinearLayout
    private lateinit var btnTrimVideo: LinearLayout
    private lateinit var btnEnhanceEyeContact: LinearLayout

    private var videoUri: Uri? = null
    private var isPlaying = false
    private var isMuted = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("VIDEO_URI", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("VIDEO_URI")
        }

        if (videoUri == null) {
            Toast.makeText(this, "Error loading video", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupVideoView()
        setupButtons()
    }

    private fun initializeViews() {
        videoView = findViewById(R.id.videoView)
        btnBack = findViewById(R.id.btnBack)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        btnVolume = findViewById(R.id.btnVolume)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvDuration = findViewById(R.id.tvDuration)

        btnSaveToDevice = findViewById(R.id.btnSaveToDevice)
        btnShare = findViewById(R.id.btnShare)
        switchSaveForLater = findViewById(R.id.switchSaveForLater)

        btnAddCaptions = findViewById(R.id.btnAddCaptions)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnAddText = findViewById(R.id.btnAddText)
        btnReplaceBackground = findViewById(R.id.btnReplaceBackground)
        btnResizeVideo = findViewById(R.id.btnResizeVideo)
        btnAddMusic = findViewById(R.id.btnAddMusic)
        btnTrimVideo = findViewById(R.id.btnTrimVideo)
        btnEnhanceEyeContact = findViewById(R.id.btnEnhanceEyeContact)
    }

    private fun setupVideoView() {
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mediaPlayer ->
            val duration = mediaPlayer.duration
            tvDuration.text = "-" + formatTime(duration)
            seekBar.max = duration

            videoView.start()
            isPlaying = true
            btnPlayPause.text = "⏸"
            startUpdatingSeekBar()
        }

        videoView.setOnCompletionListener {
            isPlaying = false
            btnPlayPause.text = "▶"
            stopUpdatingSeekBar()
            seekBar.progress = 0
            tvCurrentTime.text = "0:00"
        }

        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            finish()
        }

        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                videoView.pause()
                btnPlayPause.text = "▶"
                isPlaying = false
                stopUpdatingSeekBar()
            } else {
                videoView.start()
                btnPlayPause.text = "⏸"
                isPlaying = true
                startUpdatingSeekBar()
            }
        }

        btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        btnVolume.setOnClickListener {
            toggleMute()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    tvCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopUpdatingSeekBar()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isPlaying) {
                    startUpdatingSeekBar()
                }
            }
        })

        btnSaveToDevice.setOnClickListener {
            saveOriginalVideo()
        }

        btnShare.setOnClickListener {
            shareVideo()
        }

        switchSaveForLater.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Video saved for later", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddCaptions.setOnClickListener {
            Toast.makeText(this, "Add captions feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnAddImage.setOnClickListener {
            Toast.makeText(this, "Image editing feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnAddText.setOnClickListener {
            Toast.makeText(this, "Text overlay feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnReplaceBackground.setOnClickListener {
            Toast.makeText(this, "Replace background feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnResizeVideo.setOnClickListener {
            Toast.makeText(this, "Resize video feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnAddMusic.setOnClickListener {
            Toast.makeText(this, "Add music feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnTrimVideo.setOnClickListener {
            Toast.makeText(this, "Trim video feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnEnhanceEyeContact.setOnClickListener {
            Toast.makeText(this, "Enhance eye contact feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveOriginalVideo() {
        try {
            val contentResolver = contentResolver
            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val videoDetails = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "MyPrompter_${System.currentTimeMillis()}.mp4")
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyPrompter")
                    put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            videoUri?.let { sourceUri ->
                val newUri = contentResolver.insert(videoCollection, videoDetails)

                newUri?.let { destUri ->
                    contentResolver.openOutputStream(destUri)?.use { outputStream ->
                        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        videoDetails.clear()
                        videoDetails.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
                        contentResolver.update(destUri, videoDetails, null, null)
                    }

                    Toast.makeText(this, "✅ Video saved to gallery!", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Failed to save video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startUpdatingSeekBar() {
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                if (isPlaying && videoView.isPlaying) {
                    val currentPosition = videoView.currentPosition
                    seekBar.progress = currentPosition
                    tvCurrentTime.text = formatTime(currentPosition)
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun stopUpdatingSeekBar() {
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun toggleFullscreen() {
        if (window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            btnFullscreen.text = "⊡"
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            btnFullscreen.text = "⛶"
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            videoView.setOnPreparedListener { mp ->
                mp.setVolume(0f, 0f)
            }
            btnVolume.text = "🔇"
        } else {
            videoView.setOnPreparedListener { mp ->
                mp.setVolume(1f, 1f)
            }
            btnVolume.text = "🔊"
        }
    }

    private fun shareVideo() {
        videoUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share video via"))
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            videoView.pause()
            isPlaying = false
            stopUpdatingSeekBar()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdatingSeekBar()
        videoView.stopPlayback()
    }
}