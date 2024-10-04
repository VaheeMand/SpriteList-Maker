package com.vaheemand.spritelistmaker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
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
    private lateinit var rowCountLabel: TextView
    private lateinit var imageSizeLabel: TextView

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

        importButton.setOnClickListener { importImages() }
        exportButton.setOnClickListener { saveSpriteList() }

        setupSeekBarListeners()
    }

    private fun setupSeekBarListeners() {
        rowCountSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rowCountEditText.setText(progress.toString())
                combineImagesIntoSpriteList()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        imageSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                imageSizeEditText.setText(progress.toString())
                combineImagesIntoSpriteList()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
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
        val imageSize = imageSizeEditText.text.toString().toIntOrNull() ?: 256

        val width = imageSize * imageUris.size
        val height = imageSize * rowCount

        val spriteListBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(spriteListBitmap)

        for (i in imageUris.indices) {
            val uri = imageUris[i]
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)

            val x = i * imageSize
            val y = 0 // You can adjust y if you want to stack images vertically

            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
        }

        previewImageView.setImageBitmap(spriteListBitmap)
    }

    private fun saveSpriteList() {
        val rowCount = rowCountEditText.text.toString().toIntOrNull() ?: 1
        val imageSize = imageSizeEditText.text.toString().toIntOrNull() ?: 256

        val width = imageSize * imageUris.size
        val height = imageSize * rowCount

        val spriteListBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(spriteListBitmap)

        for (i in imageUris.indices) {
            val uri = imageUris[i]
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)

            val x = i * imageSize
            val y = 0 // You can adjust y if you want to stack images vertically

            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
        }

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