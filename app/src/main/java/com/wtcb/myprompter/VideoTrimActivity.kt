package com.wtcb.myprompter

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.RangeSlider
import java.io.File

class VideoTrimActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var btnTrim: MaterialButton
    private lateinit var rangeSlider: RangeSlider
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var tvTotalDuration: TextView

    private var videoUri: Uri? = null
    private var videoDuration: Int = 0
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var startTimeMs: Int = 0
    private var endTimeMs: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_trim)

        videoUri = intent.getParcelableExtra("VIDEO_URI")
        
        if (videoUri == null) {
            Toast.makeText(this, "Error loading video", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupVideo()
        setupControls()
    }

    private fun initializeViews() {
        videoView = findViewById(R.id.videoView)
        btnBack = findViewById(R.id.btnBack)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnTrim = findViewById(R.id.btnTrim)
        rangeSlider = findViewById(R.id.rangeSlider)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)
        tvTotalDuration = findViewById(R.id.tvTotalDuration)
    }

    private fun setupVideo() {
        // Get video duration
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, videoUri)
            videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
            retriever.release()

            // Set initial values
            startTimeMs = 0
            endTimeMs = videoDuration

            // Setup range slider
            rangeSlider.valueFrom = 0f
            rangeSlider.valueTo = videoDuration.toFloat()
            rangeSlider.values = listOf(0f, videoDuration.toFloat())

            // Update UI
            updateTimeDisplays()
            tvTotalDuration.text = "Total: ${formatTime(videoDuration)}"

            // Setup video view
            videoView.setVideoURI(videoUri)
            videoView.setOnPreparedListener { 
                // Video prepared
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading video: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupControls() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Play/Pause button
        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }

        // Range slider
        rangeSlider.addOnChangeListener { slider, _, fromUser ->
            if (fromUser) {
                val values = slider.values
                startTimeMs = values[0].toInt()
                endTimeMs = values[1].toInt()
                updateTimeDisplays()
                
                // Seek to start position
                if (!isPlaying) {
                    videoView.seekTo(startTimeMs)
                }
            }
        }

        // Trim button
        btnTrim.setOnClickListener {
            trimVideo()
        }
    }

    private fun playVideo() {
        videoView.seekTo(startTimeMs)
        videoView.start()
        isPlaying = true
        btnPlayPause.text = "⏸"
        startMonitoring()
    }

    private fun pauseVideo() {
        videoView.pause()
        isPlaying = false
        btnPlayPause.text = "▶"
        stopMonitoring()
    }

    private fun startMonitoring() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    val currentPos = videoView.currentPosition
                    
                    // Stop at end time
                    if (currentPos >= endTimeMs) {
                        pauseVideo()
                        videoView.seekTo(startTimeMs)
                    } else {
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopMonitoring() {
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateTimeDisplays() {
        tvStartTime.text = "Start: ${formatTime(startTimeMs)}"
        tvEndTime.text = "End: ${formatTime(endTimeMs)}"
        val duration = endTimeMs - startTimeMs
        btnTrim.text = "Trim (${formatTime(duration)})"
    }

    private fun trimVideo() {
        // For now, we'll pass the trim times back to preview
        // In a real app, you'd use FFmpeg or MediaCodec to actually trim the video
        
        Toast.makeText(this, 
            "Trimming from ${formatTime(startTimeMs)} to ${formatTime(endTimeMs)}", 
            Toast.LENGTH_LONG).show()

        // Return trim parameters
        val intent = Intent().apply {
            putExtra("START_TIME", startTimeMs)
            putExtra("END_TIME", endTimeMs)
            putExtra("VIDEO_URI", videoUri)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pauseVideo()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        videoView.stopPlayback()
    }
}
