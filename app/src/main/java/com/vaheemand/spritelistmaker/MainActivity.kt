package com.vaheemand.spritelistmaker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    
    private lateinit var importButton: Button
    private lateinit var exportButton: Button
    private lateinit var previewImageView: ImageView
    private lateinit var rowCountSeekBar: SeekBar
    private lateinit var imageWidthSeekBar: SeekBar
    private lateinit var imageHeightSeekBar: SeekBar
    private lateinit var qualitySeekBar: SeekBar
    private lateinit var rowCountEditText: EditText
    private lateinit var imageWidthEditText: EditText
    private lateinit var imageHeightEditText: EditText
    private lateinit var qualityEditText: EditText
    private lateinit var customQualityEditText: EditText
    private lateinit var qualitySpinner: Spinner
    private lateinit var scaleModeSpinner: Spinner
    private lateinit var useCustomQualityCheckbox: CheckBox
    private lateinit var maintainAspectRatioCheckbox: CheckBox
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var lockAspectRatioButton: Button

    private val imageUris = mutableListOf<Uri>()
    private val settings = SpriteSettings()
    private val spriteProcessor by lazy { SpriteListProcessor(this) }
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var previewUpdateRunnable: Runnable? = null
    private val PREVIEW_UPDATE_DELAY = 500L
    
    private var currentPreviewBitmap: Bitmap? = null
    
    private val PICK_IMAGES = 1
    
    // Объявляем как lateinit, инициализируем в onCreate
    private lateinit var qualityPresets: List<Pair<String, Float>>
    private lateinit var scaleModes: List<Pair<String, ScaleMode>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализируем списки после super.onCreate()
        qualityPresets = listOf(
            getString(R.string.quality_x0_1) to 0.1f,
            getString(R.string.quality_x0_25) to 0.25f,
            getString(R.string.quality_x0_5) to 0.5f,
            getString(R.string.quality_x0_75) to 0.75f,
            getString(R.string.quality_x1_0) to 1.0f,
            getString(R.string.quality_x1_5) to 1.5f,
            getString(R.string.quality_x2_0) to 2.0f,
            getString(R.string.quality_custom) to 1.0f
        )
        
        scaleModes = listOf(
            getString(R.string.stretch_mode) to ScaleMode.STRETCH,
            getString(R.string.fit_mode) to ScaleMode.FIT,
            getString(R.string.crop_mode) to ScaleMode.CROP,
            getString(R.string.custom_ratio_mode) to ScaleMode.CUSTOM_RATIO
        )

        initializeViews()
        setupSpinners()
        setupListeners()
        setDefaultValues()
    }

    private fun initializeViews() {
        importButton = findViewById(R.id.importButton)
        exportButton = findViewById(R.id.exportButton)
        previewImageView = findViewById(R.id.previewImageView)
        rowCountSeekBar = findViewById(R.id.rowCountSeekBar)
        imageWidthSeekBar = findViewById(R.id.imageWidthSeekBar)
        imageHeightSeekBar = findViewById(R.id.imageHeightSeekBar)
        qualitySeekBar = findViewById(R.id.qualitySeekBar)
        rowCountEditText = findViewById(R.id.rowCountEditText)
        imageWidthEditText = findViewById(R.id.imageWidthEditText)
        imageHeightEditText = findViewById(R.id.imageHeightEditText)
        qualityEditText = findViewById(R.id.qualityEditText)
        customQualityEditText = findViewById(R.id.customQualityEditText)
        qualitySpinner = findViewById(R.id.qualitySpinner)
        scaleModeSpinner = findViewById(R.id.scaleModeSpinner)
        useCustomQualityCheckbox = findViewById(R.id.useCustomQualityCheckbox)
        maintainAspectRatioCheckbox = findViewById(R.id.maintainAspectRatioCheckbox)
        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)
        lockAspectRatioButton = findViewById(R.id.lockAspectRatioButton)
        
        // Установка текстов из strings.xml
        importButton.text = getString(R.string.import_images)
        exportButton.text = getString(R.string.export_sprite_list)
        findViewById<TextView>(R.id.previewLabel).text = getString(R.string.preview)
        findViewById<TextView>(R.id.rowCountLabel).text = getString(R.string.row_count)
        findViewById<TextView>(R.id.imageWidthLabel).text = getString(R.string.image_width)
        findViewById<TextView>(R.id.imageHeightLabel).text = getString(R.string.image_height)
        findViewById<TextView>(R.id.qualityLabel).text = getString(R.string.quality_scale)
        findViewById<TextView>(R.id.scaleModeLabel).text = getString(R.string.scale_mode)
        useCustomQualityCheckbox.text = getString(R.string.use_custom_quality)
        maintainAspectRatioCheckbox.text = getString(R.string.maintain_aspect_ratio)
        lockAspectRatioButton.text = getString(R.string.lock_aspect_ratio)
        customQualityEditText.hint = getString(R.string.custom_quality_hint)
        
        // Установка начального статуса
        statusTextView.text = getString(R.string.no_images_loaded)
    }

    private fun setupSpinners() {
        val presetNames = qualityPresets.map { it.first }
        val qualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetNames)
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qualitySpinner.adapter = qualityAdapter
        
        qualitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position != qualityPresets.size - 1) {
                    settings.qualityScale = qualityPresets[position].second
                    qualityEditText.setText(String.format("%.2f", settings.qualityScale))
                    useCustomQualityCheckbox.isChecked = false
                    updateQualityControls()
                }
                schedulePreviewUpdate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        val scaleModeNames = scaleModes.map { it.first }
        val scaleModeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, scaleModeNames)
        scaleModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        scaleModeSpinner.adapter = scaleModeAdapter
        
        scaleModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                settings.scaleMode = scaleModes[position].second
                maintainAspectRatioCheckbox.isEnabled = settings.scaleMode == ScaleMode.CUSTOM_RATIO
                schedulePreviewUpdate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        importButton.setOnClickListener { importImages() }
        exportButton.setOnClickListener { saveSpriteList() }
        
        useCustomQualityCheckbox.setOnCheckedChangeListener { _, isChecked ->
            settings.useCustomQuality = isChecked
            updateQualityControls()
            schedulePreviewUpdate()
        }
        
        maintainAspectRatioCheckbox.setOnCheckedChangeListener { _, isChecked ->
            settings.maintainAspectRatio = isChecked
            schedulePreviewUpdate()
        }
        
        lockAspectRatioButton.setOnClickListener {
            val width = settings.imageWidth
            if (width > 0) {
                settings.imageHeight = width
                imageHeightEditText.setText(width.toString())
                imageHeightSeekBar.progress = min(width, imageHeightSeekBar.max)
                schedulePreviewUpdate()
            }
        }

        setupSeekBarListeners()
        setupEditTextListeners()
    }

    private fun updateQualityControls() {
        customQualityEditText.isEnabled = settings.useCustomQuality
        qualitySpinner.isEnabled = !settings.useCustomQuality
        qualitySeekBar.isEnabled = !settings.useCustomQuality
        qualityEditText.isEnabled = !settings.useCustomQuality
    }

    private fun setupSeekBarListeners() {
        rowCountSeekBar.setOnSeekBarChangeListener(createSeekBarListener(
            onProgressChanged = { progress ->
                if (progress > 0) {
                    rowCountEditText.setText(progress.toString())
                }
            },
            onStopTrackingTouch = { schedulePreviewUpdate() }
        ))

        imageWidthSeekBar.setOnSeekBarChangeListener(createSeekBarListener(
            onProgressChanged = { progress ->
                if (progress > 0) {
                    imageWidthEditText.setText(progress.toString())
                }
            },
            onStopTrackingTouch = { schedulePreviewUpdate() }
        ))

        imageHeightSeekBar.setOnSeekBarChangeListener(createSeekBarListener(
            onProgressChanged = { progress ->
                if (progress > 0) {
                    imageHeightEditText.setText(progress.toString())
                }
            },
            onStopTrackingTouch = { schedulePreviewUpdate() }
        ))

        qualitySeekBar.setOnSeekBarChangeListener(createSeekBarListener(
            onProgressChanged = { progress ->
                val quality = progress / 100.0f
                settings.qualityScale = quality
                qualityEditText.setText(String.format("%.2f", quality))
            },
            onStopTrackingTouch = { schedulePreviewUpdate() }
        ))
    }

    private fun createSeekBarListener(
        onProgressChanged: (Int) -> Unit = {},
        onStopTrackingTouch: () -> Unit = {}
    ): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onProgressChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { onStopTrackingTouch() }
        }
    }

    private fun setupEditTextListeners() {
        setupEditTextListener(rowCountEditText) { text ->
            val value = text.toIntOrNull() ?: 1
            settings.rowCount = value.coerceAtLeast(1)
            rowCountSeekBar.progress = min(settings.rowCount, rowCountSeekBar.max)
        }

        setupEditTextListener(imageWidthEditText) { text ->
            val value = text.toIntOrNull() ?: 128
            settings.imageWidth = value.coerceAtLeast(1)
            imageWidthSeekBar.progress = min(settings.imageWidth, imageWidthSeekBar.max)
        }

        setupEditTextListener(imageHeightEditText) { text ->
            val value = text.toIntOrNull() ?: 128
            settings.imageHeight = value.coerceAtLeast(1)
            imageHeightSeekBar.progress = min(settings.imageHeight, imageHeightSeekBar.max)
        }

        setupEditTextListener(qualityEditText) { text ->
            val value = text.toFloatOrNull() ?: 1.0f
            settings.qualityScale = value.coerceIn(0.1f, 2.0f)
            qualitySeekBar.progress = (settings.qualityScale * 100).toInt()
        }

        setupEditTextListener(customQualityEditText) { text ->
            val value = text.toFloatOrNull() ?: 1.0f
            settings.customQualityValue = value.coerceIn(0.1f, 4.0f)
        }
    }

    private fun setupEditTextListener(editText: EditText, onTextChanged: (String) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s.toString())
                schedulePreviewUpdate()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setDefaultValues() {
        rowCountSeekBar.progress = 1
        imageWidthSeekBar.progress = 128
        imageHeightSeekBar.progress = 128
        qualitySeekBar.progress = 100
        rowCountEditText.setText("1")
        imageWidthEditText.setText("128")
        imageHeightEditText.setText("128")
        qualityEditText.setText("1.00")
        customQualityEditText.setText("1.00")
        qualitySpinner.setSelection(4) // x1.0
        scaleModeSpinner.setSelection(0) // Stretch
        maintainAspectRatioCheckbox.isChecked = false
    }

    private fun schedulePreviewUpdate() {
        previewUpdateRunnable?.let { handler.removeCallbacks(it) }
        
        previewUpdateRunnable = Runnable {
            combineImagesIntoSpriteList()
        }
        handler.postDelayed(previewUpdateRunnable!!, PREVIEW_UPDATE_DELAY)
    }

    private fun importImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES && resultCode == Activity.RESULT_OK) {
            data?.let { handleSelectedImages(it) }
        }
    }

    private fun handleSelectedImages(data: Intent) {
        imageUris.clear()
        if (data.clipData != null) {
            val count = data.clipData!!.itemCount
            for (i in 0 until count) {
                imageUris.add(data.clipData!!.getItemAt(i).uri)
            }
        } else {
            data.data?.let { imageUris.add(it) }
        }
        updateStatus(getString(R.string.loaded_images_count, imageUris.size))
        schedulePreviewUpdate()
    }

    private fun combineImagesIntoSpriteList() {
        if (imageUris.isEmpty()) {
            currentPreviewBitmap?.recycle()
            currentPreviewBitmap = null
            previewImageView.setImageBitmap(null)
            updateStatus(getString(R.string.no_images_loaded))
            return
        }

        coroutineScope.launch {
            showProgress(true)
            try {
                val spriteBitmap = spriteProcessor.createSpriteList(imageUris, settings, isPreview = true)
                spriteBitmap?.let {
                    currentPreviewBitmap?.recycle()
                    currentPreviewBitmap = it
                    previewImageView.setImageBitmap(it)
                    updateStatus(getString(R.string.preview_generated, it.width, it.height, imageUris.size))
                } ?: updateStatus(getString(R.string.error_memory_preview))
            } catch (e: Exception) {
                updateStatus(getString(R.string.error_generating_preview, e.message))
            } finally {
                showProgress(false)
            }
        }
    }

    private fun saveSpriteList() {
        if (imageUris.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_images_to_export), Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            showProgress(true)
            try {
                val spriteBitmap = spriteProcessor.createSpriteList(imageUris, settings, isPreview = false)
                if (spriteBitmap != null) {
                    saveBitmapToFile(spriteBitmap)
                    spriteBitmap.recycle()
                    updateStatus(getString(R.string.sprite_saved_success))
                    Toast.makeText(this@MainActivity, getString(R.string.sprite_saved_to_pictures), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.error_memory_save), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                updateStatus(getString(R.string.error_saving_sprite, e.message))
                Toast.makeText(this@MainActivity, getString(R.string.error_saving_file), Toast.LENGTH_SHORT).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap) {
        val fileName = String.format("%04d%02d%02d_%06d_spritelistmaker.png",
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            (0..999999).random()
        )

        val picturesDir = File("/storage/emulated/0/Pictures")
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }

        val file = File(picturesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            throw e
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        importButton.isEnabled = !show
        exportButton.isEnabled = !show
    }

    private fun updateStatus(message: String) {
        statusTextView.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        previewUpdateRunnable?.let { handler.removeCallbacks(it) }
        
        currentPreviewBitmap?.recycle()
        currentPreviewBitmap = null
        previewImageView.setImageBitmap(null)
        
        spriteProcessor.cleanup()
    }
}