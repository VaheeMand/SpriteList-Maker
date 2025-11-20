package com.vaheemand.spritelistmaker

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import java.io.IOException
import kotlin.math.min
import kotlin.math.max

object ImageUtils {
    
    private const val MAX_MEMORY_RATIO = 0.3
    private const val TAG = "ImageUtils"
    private const val PREVIEW_MAX_SIZE = 800
    
    fun calculateOptimalInSampleSize(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            var inSampleSize = 1
            if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                
                while (halfHeight / inSampleSize >= targetHeight &&
                    halfWidth / inSampleSize >= targetWidth) {
                    inSampleSize *= 2
                }
            }
            inSampleSize
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating sample size", e)
            1
        }
    }
    
    fun decodeAndScaleBitmap(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        scaleMode: ScaleMode,
        maintainAspectRatio: Boolean = false,
        qualityScale: Float = 1.0f
    ): Bitmap? {
        return try {
            val finalWidth = (targetWidth * qualityScale).toInt()
            val finalHeight = (targetHeight * qualityScale).toInt()
            
            val options = BitmapFactory.Options()
            options.inSampleSize = calculateOptimalInSampleSize(context, uri, finalWidth, finalHeight)
            
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val originalBitmap = BitmapFactory.decodeStream(stream, null, options)
                originalBitmap?.let { 
                    scaleBitmap(it, finalWidth, finalHeight, scaleMode, maintainAspectRatio) 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap", e)
            null
        }
    }
    
    fun scaleBitmap(
        original: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        scaleMode: ScaleMode,
        maintainAspectRatio: Boolean = false
    ): Bitmap {
        return when (scaleMode) {
            ScaleMode.STRETCH -> {
                Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
            }
            ScaleMode.FIT -> {
                // Без отступов - растягиваем до заполнения
                val scaleX = targetWidth.toFloat() / original.width
                val scaleY = targetHeight.toFloat() / original.height
                val scale = max(scaleX, scaleY) // Заполняем всю область
                
                val scaledWidth = (original.width * scale).toInt()
                val scaledHeight = (original.height * scale).toInt()
                
                val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
                val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)
                
                // Центрируем и обрезаем если нужно
                val left = (targetWidth - scaledWidth) / 2
                val top = (targetHeight - scaledHeight) / 2
                
                // Рисуем без отступов - обрезаем если выходит за границы
                val srcRect = Rect(
                    max(0, -left),
                    max(0, -top),
                    min(scaledWidth, targetWidth - left),
                    min(scaledHeight, targetHeight - top)
                )
                
                val dstRect = Rect(
                    max(0, left),
                    max(0, top),
                    min(targetWidth, left + scaledWidth),
                    min(targetHeight, top + scaledHeight)
                )
                
                canvas.drawBitmap(scaledBitmap, srcRect, dstRect, null)
                
                // Перерабатываем временный bitmap
                if (scaledBitmap != original) {
                    scaledBitmap.recycle()
                }
                resultBitmap
            }
            ScaleMode.CROP -> {
                val scale = max(
                    targetWidth.toFloat() / original.width,
                    targetHeight.toFloat() / original.height
                )
                
                val scaledWidth = (original.width * scale).toInt()
                val scaledHeight = (original.height * scale).toInt()
                
                val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
                val x = (scaledWidth - targetWidth) / 2
                val y = (scaledHeight - targetHeight) / 2
                
                val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)
                canvas.drawBitmap(
                    scaledBitmap, 
                    Rect(x, y, x + targetWidth, y + targetHeight),
                    Rect(0, 0, targetWidth, targetHeight),
                    null
                )
                
                if (scaledBitmap != original) {
                    scaledBitmap.recycle()
                }
                resultBitmap
            }
            ScaleMode.CUSTOM_RATIO -> {
                if (maintainAspectRatio) {
                    val scale = min(
                        targetWidth.toFloat() / original.width,
                        targetHeight.toFloat() / original.height
                    ).coerceAtMost(1.0f)
                    
                    val scaledWidth = (original.width * scale).toInt()
                    val scaledHeight = (original.height * scale).toInt()
                    
                    val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
                    val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(resultBitmap)
                    
                    // Без отступов - центрируем
                    val left = (targetWidth - scaledWidth) / 2
                    val top = (targetHeight - scaledHeight) / 2
                    canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)
                    
                    resultBitmap
                } else {
                    Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
                }
            }
        }
    }
    
    fun createPreviewBitmap(original: Bitmap, maxSize: Int = PREVIEW_MAX_SIZE): Bitmap {
        val width = original.width
        val height = original.height
        
        return if (width > maxSize || height > maxSize) {
            val scale = min(
                maxSize.toFloat() / width,
                maxSize.toFloat() / height
            )
            val previewWidth = (width * scale).toInt()
            val previewHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(original, previewWidth, previewHeight, true)
        } else {
            original.copy(original.config, true)
        }
    }
    
    fun canProcessImages(
        context: Context,
        imageCount: Int,
        imageWidth: Int,
        imageHeight: Int,
        qualityScale: Float
    ): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = runtime.maxMemory() - usedMemory
        val maxMemory = runtime.maxMemory()
        
        val estimatedMemory = imageCount * (imageWidth * imageHeight * 4) * qualityScale * qualityScale
        val safeMemoryLimit = maxMemory * MAX_MEMORY_RATIO
        
        Log.d(TAG, "Available memory: ${availableMemory / (1024 * 1024)}MB")
        Log.d(TAG, "Estimated memory: ${estimatedMemory / (1024 * 1024)}MB")
        Log.d(TAG, "Safe limit: ${safeMemoryLimit / (1024 * 1024)}MB")
        
        return estimatedMemory < safeMemoryLimit
    }
    
    fun getOptimalBatchSize(
        context: Context,
        totalImages: Int,
        imageWidth: Int,
        imageHeight: Int,
        qualityScale: Float
    ): Int {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val safeMemoryPerImage = (maxMemory * MAX_MEMORY_RATIO) / totalImages
        val requiredMemoryPerImage = (imageWidth * imageHeight * 4) * qualityScale * qualityScale
        
        if (requiredMemoryPerImage > safeMemoryPerImage) {
            return 1
        }
        
        return min(totalImages, (safeMemoryPerImage / requiredMemoryPerImage).toInt())
    }
}