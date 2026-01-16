package com.wtcb.myprompter

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddTextActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var textOverlay: TextView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var etTextInput: TextInputEditText
    private lateinit var spinnerColor: Spinner
    private lateinit var spinnerSize: Spinner
    private lateinit var spinnerPosition: Spinner
    private lateinit var btnApply: MaterialButton
    private lateinit var btnSave: MaterialButton

    private var videoUri: Uri? = null
    private var isPlaying = false

    // Text properties
    private var textContent = ""
    private var textColor = Color.WHITE
    private var textSize = 24f
    private var textPosition = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_text)

        videoUri = intent.getParcelableExtra("VIDEO_URI")
        
        if (videoUri == null) {
            Toast.makeText(this, "Error loading video", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupVideo()
        setupControls()
        setupSpinners()
    }

    private fun initializeViews() {
        videoView = findViewById(R.id.videoView)
        textOverlay = findViewById(R.id.textOverlay)
        btnBack = findViewById(R.id.btnBack)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        etTextInput = findViewById(R.id.etTextInput)
        spinnerColor = findViewById(R.id.spinnerColor)
        spinnerSize = findViewById(R.id.spinnerSize)
        spinnerPosition = findViewById(R.id.spinnerPosition)
        btnApply = findViewById(R.id.btnApply)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupVideo() {
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { 
            // Video prepared
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
                videoView.pause()
                btnPlayPause.text = "▶"
                isPlaying = false
            } else {
                videoView.start()
                btnPlayPause.text = "⏸"
                isPlaying = true
            }
        }

        // Apply button - Preview text
        btnApply.setOnClickListener {
            applyTextSettings()
        }

        // Save button
        btnSave.setOnClickListener {
            saveTextOverlay()
        }
    }

    private fun setupSpinners() {
        // Color spinner
        val colors = arrayOf("White", "Black", "Red", "Blue", "Green", "Yellow", "Orange", "Purple")
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colors)
        spinnerColor.adapter = colorAdapter
        
        spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                textColor = when (position) {
                    0 -> Color.WHITE
                    1 -> Color.BLACK
                    2 -> Color.RED
                    3 -> Color.BLUE
                    4 -> Color.GREEN
                    5 -> Color.YELLOW
                    6 -> Color.parseColor("#FF5722") // Orange
                    7 -> Color.parseColor("#9C27B0") // Purple
                    else -> Color.WHITE
                }
                updateTextOverlay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Size spinner
        val sizes = arrayOf("Small", "Medium", "Large", "Extra Large")
        val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        spinnerSize.adapter = sizeAdapter
        spinnerSize.setSelection(1) // Default to Medium
        
        spinnerSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                textSize = when (position) {
                    0 -> 18f  // Small
                    1 -> 24f  // Medium
                    2 -> 32f  // Large
                    3 -> 42f  // Extra Large
                    else -> 24f
                }
                updateTextOverlay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Position spinner
        val positions = arrayOf("Top", "Center", "Bottom")
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positions)
        spinnerPosition.adapter = positionAdapter
        spinnerPosition.setSelection(2) // Default to Bottom
        
        spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                textPosition = when (position) {
                    0 -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    1 -> Gravity.CENTER
                    2 -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    else -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
                updateTextOverlay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyTextSettings() {
        textContent = etTextInput.text.toString()
        
        if (textContent.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            return
        }

        updateTextOverlay()
        Toast.makeText(this, "Text applied! Preview on video", Toast.LENGTH_SHORT).show()
    }

    private fun updateTextOverlay() {
        if (textContent.isEmpty()) return

        textOverlay.text = textContent
        textOverlay.setTextColor(textColor)
        textOverlay.textSize = textSize
        
        val params = textOverlay.layoutParams as FrameLayout.LayoutParams
        params.gravity = textPosition
        
        // Add padding based on position
        val padding = 32
        when (textPosition) {
            Gravity.TOP or Gravity.CENTER_HORIZONTAL -> {
                params.topMargin = padding
                params.bottomMargin = 0
            }
            Gravity.CENTER -> {
                params.topMargin = 0
                params.bottomMargin = 0
            }
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL -> {
                params.topMargin = 0
                params.bottomMargin = padding
            }
        }
        
        textOverlay.layoutParams = params
        textOverlay.visibility = android.view.View.VISIBLE
        
        // Add text shadow for better readability
        textOverlay.setShadowLayer(8f, 0f, 0f, Color.BLACK)
        textOverlay.setTypeface(null, Typeface.BOLD)
    }

    private fun saveTextOverlay() {
        if (textContent.isEmpty()) {
            Toast.makeText(this, "Please add text first", Toast.LENGTH_SHORT).show()
            return
        }

        // Return text settings to preview
        val intent = Intent().apply {
            putExtra("TEXT_CONTENT", textContent)
            putExtra("TEXT_COLOR", textColor)
            putExtra("TEXT_SIZE", textSize)
            putExtra("TEXT_POSITION", textPosition)
            putExtra("VIDEO_URI", videoUri)
        }
        setResult(RESULT_OK, intent)
        
        Toast.makeText(this, "✅ Text added to video!", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            videoView.pause()
            isPlaying = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
}
