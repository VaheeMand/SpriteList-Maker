package com.vaheemand.spritelistmaker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var importButton: Button
    private lateinit var exportButton: Button
    private lateinit var previewImageView: ImageView
    private lateinit var rowCountSeekBar: SeekBar
    private lateinit var imageSizeSeekBar: SeekBar
    private lateinit var rowCountEditText: EditText
    private lateinit var imageSizeEditText: EditText

    private val imageUris = mutableListOf<Uri>()
    private val PICK_IMAGES = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        importButton = findViewById(R.id.importButton)
        exportButton = findViewById(R.id.exportButton)
        previewImageView = findViewById(R.id.previewImageView)
        rowCountSeekBar = findViewById(R.id.rowCountSeekBar)
        imageSizeSeekBar = findViewById(R.id.imageSizeSeekBar)
        rowCountEditText = findViewById(R.id.rowCountEditText)
        imageSizeEditText = findViewById(R.id.imageSizeEditText)

        // Установка значений по умолчанию
        rowCountSeekBar.progress = 1
        imageSizeSeekBar.progress = 128
        rowCountEditText.setText("1")
        imageSizeEditText.setText("128")

        importButton.setOnClickListener { importImages() }
        exportButton.setOnClickListener { saveSpriteList() }

        setupSeekBarListeners()
        setupEditTextListeners()
    }

    private fun setupSeekBarListeners() {
        rowCountSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress > 0) {
                    rowCountEditText.setText(progress.toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                combineImagesIntoSpriteList()
            }
        })

        imageSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress > 0) {
                    imageSizeEditText.setText(progress.toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                combineImagesIntoSpriteList()
            }
        })
    }

    private fun setupEditTextListeners() {
        rowCountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull() ?: 1
                if (value < 1) {
                    rowCountEditText.setText("1")
                }
                // Оставляем слайдер в пределах max, но текстовое поле принимает больше
                rowCountSeekBar.progress = value.coerceAtLeast(1).coerceAtMost(rowCountSeekBar.max)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        imageSizeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull() ?: 128
                if (value < 1) {
                    imageSizeEditText.setText("128")
                }
                // Слайдер будет иметь максимум, но текстовое поле принимает больше
                imageSizeSeekBar.progress = value.coerceAtLeast(1).coerceAtMost(imageSizeSeekBar.max)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun importImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES && resultCode == Activity.RESULT_OK) {
            data?.let {
                if (it.clipData != null) {
                    val count = it.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = it.clipData!!.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else {
                    data.data?.let { uri -> imageUris.add(uri) }
                }
                combineImagesIntoSpriteList()
            }
        }
    }

    private fun combineImagesIntoSpriteList() {
    if (imageUris.isEmpty()) return

    val rowCount = rowCountEditText.text.toString().toIntOrNull() ?: 1
    val imageSize = imageSizeEditText.text.toString().toIntOrNull() ?: 128

    val totalImages = imageUris.size
    val maxImagesInRow = totalImages / rowCount + if (totalImages % rowCount > 0) 1 else 0

    val width = imageSize * maxImagesInRow
    val height = imageSize * rowCount

    val spriteListBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(spriteListBitmap)

    for (i in imageUris.indices) {
        val uri = imageUris[i]
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)

        val row = i / maxImagesInRow // Определяем строку
        val col = i % maxImagesInRow // Определяем позицию в строке

        val x = col * imageSize
        val y = row * imageSize

        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
    }

        previewImageView.setImageBitmap(spriteListBitmap)
    }

    private fun saveSpriteList() {
    val rowCount = rowCountEditText.text.toString().toIntOrNull() ?: 1
    val imageSize = imageSizeEditText.text.toString().toIntOrNull() ?: 128

    val totalImages = imageUris.size
    val maxImagesInRow = totalImages / rowCount + if (totalImages % rowCount > 0) 1 else 0

    val width = imageSize * maxImagesInRow
    val height = imageSize * rowCount

    val spriteListBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(spriteListBitmap)

    for (i in imageUris.indices) {
        val uri = imageUris[i]
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)

        val row = i / maxImagesInRow // Определяем строку
        val col = i % maxImagesInRow // Определяем позицию в строке

        val x = col * imageSize
        val y = row * imageSize

        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
    }

    // Код для сохранения файла
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
            spriteListBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}
}