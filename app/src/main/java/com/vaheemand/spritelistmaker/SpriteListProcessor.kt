package com.vaheemand.spritelistmaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class SpriteListProcessor(private val context: Context) {
    
    private var currentPreviewBitmap: Bitmap? = null
    
    suspend fun createSpriteList(
        imageUris: List<Uri>,
        settings: SpriteSettings,
        isPreview: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) return@withContext null
        
        try {
            val actualQuality = if (isPreview) {
                settings.previewQuality
            } else if (settings.useCustomQuality) {
                settings.customQualityValue
            } else {
                settings.qualityScale
            }
            
            if (!isPreview && !ImageUtils.canProcessImages(
                    context, imageUris.size, settings.imageWidth, settings.imageHeight, actualQuality
                )) {
                Log.w("SpriteProcessor", "Too many images, processing in batches")
                return@withContext processInBatches(imageUris, settings, actualQuality, isPreview)
            }
            
            processBatch(imageUris, 0, imageUris.size, settings, actualQuality, isPreview)
        } catch (e: Exception) {
            Log.e("SpriteProcessor", "Error creating sprite list", e)
            null
        }
    }
    
    private suspend fun processInBatches(
        imageUris: List<Uri>,
        settings: SpriteSettings,
        quality: Float,
        isPreview: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        val batchSize = ImageUtils.getOptimalBatchSize(
            context, imageUris.size, settings.imageWidth, settings.imageHeight, quality
        )
        var resultBitmap: Bitmap? = null
        
        for (i in imageUris.indices step batchSize) {
            val end = min(i + batchSize, imageUris.size)
            val batch = imageUris.subList(i, end)
            val batchBitmap = processBatch(batch, i, imageUris.size, settings, quality, isPreview)
            
            if (resultBitmap == null) {
                resultBitmap = batchBitmap
            } else {
                // Объединяем батчи или используем первый
                batchBitmap?.recycle()
            }
            
            if (i + batchSize < imageUris.size) {
                System.gc()
            }
        }
        
        resultBitmap
    }
    
    private fun processBatch(
        batchUris: List<Uri>,
        startIndex: Int,
        totalImages: Int,
        settings: SpriteSettings,
        quality: Float,
        isPreview: Boolean
    ): Bitmap? {
        val rowCount = settings.rowCount
        val imageWidth = (settings.imageWidth * quality).toInt()
        val imageHeight = (settings.imageHeight * quality).toInt()
        
        val imagesInRow = (batchUris.size + rowCount - 1) / rowCount
        val width = imageWidth * imagesInRow
        val height = imageHeight * rowCount
        
        // Создаем новый bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(settings.backgroundColor)
        
        for (i in batchUris.indices) {
            val uri = batchUris[i]
            val decodedBitmap = ImageUtils.decodeAndScaleBitmap(
                context, uri, settings.imageWidth, settings.imageHeight,
                settings.scaleMode, settings.maintainAspectRatio, quality
            )
            
            if (decodedBitmap != null) {
                val row = i / imagesInRow
                val col = i % imagesInRow
                val x = col * imageWidth
                val y = row * imageHeight
                
                canvas.drawBitmap(decodedBitmap, x.toFloat(), y.toFloat(), null)
                
                // Перерабатываем декодированный bitmap, так как он больше не нужен
                // Но только если это не тот же bitmap, что и оригинал
                if (decodedBitmap != bitmap) {
                    decodedBitmap.recycle()
                }
            }
        }
        
        // Для предпросмотра создаем уменьшенную версию
        return if (isPreview) {
            val previewBitmap = ImageUtils.createPreviewBitmap(bitmap)
            // Перерабатываем исходный bitmap, так как он больше не нужен
            if (previewBitmap != bitmap) {
                bitmap.recycle()
            }
            previewBitmap
        } else {
            bitmap
        }
    }
    
    fun cleanup() {
        currentPreviewBitmap?.recycle()
        currentPreviewBitmap = null
    }
}