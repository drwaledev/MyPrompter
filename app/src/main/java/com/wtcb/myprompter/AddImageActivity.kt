package com.wtcb.myprompter

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.InputStream

class AddImageActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var imageOverlay: ImageView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var spinnerSize: Spinner
    private lateinit var spinnerPosition: Spinner
    private lateinit var spinnerOpacity: Spinner
    private lateinit var btnApply: MaterialButton
    private lateinit var btnSave: MaterialButton

    private var videoUri: Uri? = null
    private var imageUri: Uri? = null
    private var isPlaying = false

    // Image properties
    private var imageSize = 1.0f // 0.5 = small, 1.0 = medium, 1.5 = large
    private var imagePosition = Gravity.BOTTOM or Gravity.END
    private var imageOpacity = 1.0f // 0.5 = 50%, 1.0 = 100%

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            if (imageUri != null) {
                loadAndDisplayImage()
                Toast.makeText(this, "Image selected! Adjust settings below", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_image)

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
        imageOverlay = findViewById(R.id.imageOverlay)
        btnBack = findViewById(R.id.btnBack)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        spinnerSize = findViewById(R.id.spinnerSize)
        spinnerPosition = findViewById(R.id.spinnerPosition)
        spinnerOpacity = findViewById(R.id.spinnerOpacity)
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

        // Select image button
        btnSelectImage.setOnClickListener {
            selectImage()
        }

        // Apply button - Preview image
        btnApply.setOnClickListener {
            applyImageSettings()
        }

        // Save button
        btnSave.setOnClickListener {
            saveImageOverlay()
        }
    }

    private fun setupSpinners() {
        // Size spinner
        val sizes = arrayOf("Small", "Medium", "Large")
        val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        spinnerSize.adapter = sizeAdapter
        spinnerSize.setSelection(1) // Default to Medium
        
        spinnerSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                imageSize = when (position) {
                    0 -> 0.5f  // Small
                    1 -> 1.0f  // Medium
                    2 -> 1.5f  // Large
                    else -> 1.0f
                }
                updateImageOverlay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Position spinner
        val positions = arrayOf(
            "Top Left", "Top Center", "Top Right",
            "Middle Left", "Center", "Middle Right",
            "Bottom Left", "Bottom Center", "Bottom Right"
        )
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positions)
        spinnerPosition.adapter = positionAdapter
        spinnerPosition.setSelection(8) // Default to Bottom Right
        
        spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                imagePosition = when (position) {
                    0 -> Gravity.TOP or Gravity.START
                    1 -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    2 -> Gravity.TOP or Gravity.END
                    3 -> Gravity.CENTER_VERTICAL or Gravity.START
                    4 -> Gravity.CENTER
                    5 -> Gravity.CENTER_VERTICAL or Gravity.END
                    6 -> Gravity.BOTTOM or Gravity.START
                    7 -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    8 -> Gravity.BOTTOM or Gravity.END
                    else -> Gravity.BOTTOM or Gravity.END
                }
                updateImageOverlay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Opacity spinner
        val opacities = arrayOf("25%", "50%", "75%", "100%")
        val opacityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opacities)
        spinnerOpacity.adapter = opacityAdapter
        spinnerOpacity.setSelection(3) // Default to 100%
        
        spinnerOpacity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                imageOpacity = when (position) {
                    0 -> 0.25f  // 25%
                    1 -> 0.5f   // 50%
                    2 -> 0.75f  // 75%
                    3 -> 1.0f   // 100%
                    else -> 1.0f
                }
                updateImageOverlay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    private fun loadAndDisplayImage() {
        try {
            imageUri?.let { uri ->
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                imageOverlay.setImageBitmap(bitmap)
                updateImageOverlay()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyImageSettings() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            selectImage()
            return
        }

        updateImageOverlay()
        Toast.makeText(this, "Image settings applied! Preview on video", Toast.LENGTH_SHORT).show()
    }

    private fun updateImageOverlay() {
        if (imageUri == null) return

        // Calculate size (base size is 100dp)
        val baseSizeDp = 100
        val sizePx = (baseSizeDp * imageSize * resources.displayMetrics.density).toInt()
        
        val params = imageOverlay.layoutParams as FrameLayout.LayoutParams
        params.width = sizePx
        params.height = sizePx
        params.gravity = imagePosition
        
        // Add padding based on position
        val padding = (16 * resources.displayMetrics.density).toInt()
        params.setMargins(padding, padding, padding, padding)
        
        imageOverlay.layoutParams = params
        imageOverlay.alpha = imageOpacity
        imageOverlay.visibility = android.view.View.VISIBLE
    }

    private fun saveImageOverlay() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        // Return image settings to preview
        val intent = Intent().apply {
            putExtra("IMAGE_URI", imageUri.toString())
            putExtra("IMAGE_SIZE", imageSize)
            putExtra("IMAGE_POSITION", imagePosition)
            putExtra("IMAGE_OPACITY", imageOpacity)
            putExtra("VIDEO_URI", videoUri)
        }
        setResult(RESULT_OK, intent)
        
        Toast.makeText(this, "✅ Image/Logo added to video!", Toast.LENGTH_LONG).show()
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
