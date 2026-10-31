package com.example.data.model

import android.graphics.RectF

enum class AspectRatioOption(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    FREE("Free", null),
    SQUARE("1:1", 1.0f),
    FOUR_THREE("4:3", 4f / 3f),
    THREE_FOUR("3:4", 3f / 4f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

enum class FilterPreset(val displayName: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    WARM("Warm Sunset"),
    COOL("Cool Mist"),
    VINTAGE("Vintage 70s"),
    RETRO("Retro"),
    BW("B&W Classic"),
    DRAMATIC("Dramatic"),
    SOFT("Soft Glow"),
    FADE("Fade Film"),
    MONOCHROME("Monochrome"),
    HIGH_CONTRAST("High Contrast"),
    NATURAL("Natural Boost"),
    SEPIA("Sepia")
}

data class EditParameters(
    // Transform
    val cropRect: RectF? = null,
    val aspectRatio: AspectRatioOption = AspectRatioOption.ORIGINAL,
    val rotationDegrees: Int = 0,
    val fineRotationAngle: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,

    // Light
    val brightness: Float = 0f, // -100f to 100f
    val exposure: Float = 0f,   // -100f to 100f
    val contrast: Float = 0f,   // -100f to 100f
    val highlights: Float = 0f, // -100f to 100f
    val shadows: Float = 0f,    // -100f to 100f

    // Color
    val saturation: Float = 0f, // -100f to 100f
    val temperature: Float = 0f, // -100f to 100f (warm/cool)
    val tint: Float = 0f,        // -100f to 100f (green/magenta)
    val vibrance: Float = 0f,    // -100f to 100f

    // Filters
    val filter: FilterPreset = FilterPreset.ORIGINAL,
    val filterIntensity: Float = 1.0f, // 0.0f to 1.0f

    // Details & Effects
    val vignette: Float = 0f,  // 0f to 100f
    val sharpen: Float = 0f    // 0f to 100f
)
