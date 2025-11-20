package com.vaheemand.spritelistmaker

data class SpriteSettings(
    var rowCount: Int = 1,
    var imageWidth: Int = 128,
    var imageHeight: Int = 128,
    var qualityScale: Float = 1.0f,
    var customQualityValue: Float = 1.0f,
    var useCustomQuality: Boolean = false,
    var scaleMode: ScaleMode = ScaleMode.STRETCH,
    var previewQuality: Float = 0.3f, // Низкое качество для предпросмотра
    var maintainAspectRatio: Boolean = false,
    var backgroundColor: Int = 0x00000000 // Прозрачный фон
)