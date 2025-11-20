package com.vaheemand.spritelistmaker

enum class ScaleMode {
    STRETCH,      // Растягивать до заданных размеров
    FIT,          // Вписать в квадрат с сохранением пропорций (без отступов)
    CROP,         // Обрезать до квадрата
    CUSTOM_RATIO  // Пользовательское соотношение сторон
}