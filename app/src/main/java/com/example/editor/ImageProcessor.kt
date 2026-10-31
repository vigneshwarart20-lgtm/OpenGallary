package com.example.editor

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.EditParameters
import com.example.data.model.FilterPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object ImageProcessor {

    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val firstStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(firstStream, null, options)
            firstStream.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inMutable = true

            val secondStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(secondStream, null, options)
            secondStream.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    suspend fun applyEdits(
        source: Bitmap,
        params: EditParameters
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Geometric transforms (Crop, Rotate, Flip)
        var transformed = applyTransformations(source, params)

        // Step 2: Color and Light Matrix adjustments
        transformed = applyColorAndLightAdjustments(transformed, params)

        // Step 3: Vignette & Sharpen if applicable
        if (params.vignette > 0f) {
            transformed = applyVignette(transformed, params.vignette)
        }
        if (params.sharpen > 0f) {
            transformed = applySharpen(transformed, params.sharpen)
        }

        transformed
    }

    private fun applyTransformations(source: Bitmap, params: EditParameters): Bitmap {
        val matrix = Matrix()

        // 1. Rotation and flips
        val totalRotation = (params.rotationDegrees + params.fineRotationAngle)
        if (totalRotation != 0f) {
            matrix.postRotate(totalRotation, source.width / 2f, source.height / 2f)
        }

        val scaleX = if (params.flipHorizontal) -1f else 1f
        val scaleY = if (params.flipVertical) -1f else 1f
        if (scaleX != 1f || scaleY != 1f) {
            matrix.postScale(scaleX, scaleY, source.width / 2f, source.height / 2f)
        }

        var result = Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )

        // 2. Crop
        params.cropRect?.let { crop ->
            val left = (crop.left * result.width).coerceIn(0f, result.width.toFloat()).toInt()
            val top = (crop.top * result.height).coerceIn(0f, result.height.toFloat()).toInt()
            val right = (crop.right * result.width).coerceIn(left + 1f, result.width.toFloat()).toInt()
            val bottom = (crop.bottom * result.height).coerceIn(top + 1f, result.height.toFloat()).toInt()
            val cropWidth = max(1, right - left)
            val cropHeight = max(1, bottom - top)

            val cropped = Bitmap.createBitmap(result, left, top, cropWidth, cropHeight)
            if (result != source && result != cropped) {
                result.recycle()
            }
            result = cropped
        }

        return result
    }

    private fun applyColorAndLightAdjustments(source: Bitmap, params: EditParameters): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Build combined ColorMatrix
        val cm = ColorMatrix()

        // 1. Brightness & Exposure: Brightness is additive offset, Exposure scales RGB
        val bOffset = (params.brightness + params.exposure) * 1.5f
        if (bOffset != 0f) {
            val brightnessMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, bOffset,
                    0f, 1f, 0f, 0f, bOffset,
                    0f, 0f, 1f, 0f, bOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(brightnessMatrix)
        }

        // 2. Contrast: scale around 128
        if (params.contrast != 0f) {
            val c = (params.contrast + 100f) / 100f
            val cOffset = 128f * (1f - c)
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, cOffset,
                    0f, c, 0f, 0f, cOffset,
                    0f, 0f, c, 0f, cOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(contrastMatrix)
        }

        // 3. Saturation & Vibrance
        val satValue = ((params.saturation + params.vibrance) + 100f) / 100f
        if (satValue != 1f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(satValue.coerceIn(0f, 3f))
            cm.postConcat(satMatrix)
        }

        // 4. Temperature & Tint
        // Warm/Cool: adjust Red and Blue
        val temp = params.temperature / 100f // warm > 0 (boost red, drop blue)
        val tint = params.tint / 100f        // tint > 0 (boost magenta, drop green)
        if (temp != 0f || tint != 0f) {
            val rScale = 1f + temp * 0.25f
            val gScale = 1f - tint * 0.25f
            val bScale = 1f - temp * 0.25f
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    rScale, 0f, 0f, 0f, 0f,
                    0f, gScale, 0f, 0f, 0f,
                    0f, 0f, bScale, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(tempMatrix)
        }

        // 5. Preset Filters
        if (params.filter != FilterPreset.ORIGINAL) {
            val filterMatrix = getFilterMatrix(params.filter, params.filterIntensity)
            cm.postConcat(filterMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    private fun getFilterMatrix(preset: FilterPreset, intensity: Float): ColorMatrix {
        val target = ColorMatrix()
        when (preset) {
            FilterPreset.VIVID -> {
                target.setSaturation(1.4f)
                val c = 1.15f
                target.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, 10f,
                    0f, c, 0f, 0f, 10f,
                    0f, 0f, c, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.WARM -> {
                target.set(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 15f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.85f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.COOL -> {
                target.set(floatArrayOf(
                    0.85f, 0f, 0f, 0f, -10f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.25f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.VINTAGE -> {
                target.set(floatArrayOf(
                    0.9f, 0.2f, 0.1f, 0f, 20f,
                    0.1f, 0.85f, 0.15f, 0f, 10f,
                    0.1f, 0.15f, 0.7f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.RETRO -> {
                target.set(floatArrayOf(
                    1.1f, 0.1f, 0.1f, 0f, 15f,
                    0.05f, 0.95f, 0.1f, 0f, 10f,
                    0.05f, 0.1f, 0.75f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.BW -> {
                target.setSaturation(0f)
            }
            FilterPreset.DRAMATIC -> {
                target.setSaturation(0.7f)
                val c = 1.35f
                val offset = 128f * (1f - c)
                target.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, offset,
                    0f, c, 0f, 0f, offset,
                    0f, 0f, c, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.SOFT -> {
                val c = 0.9f
                target.set(floatArrayOf(
                    c, 0f, 0f, 0f, 25f,
                    0f, c, 0f, 0f, 25f,
                    0f, 0f, c, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.FADE -> {
                target.setSaturation(0.6f)
                target.postConcat(ColorMatrix(floatArrayOf(
                    0.9f, 0f, 0f, 0f, 30f,
                    0f, 0.9f, 0f, 0f, 30f,
                    0f, 0f, 0.9f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.MONOCHROME -> {
                target.setSaturation(0f)
                val c = 1.3f
                val offset = 128f * (1f - c)
                target.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, offset,
                    0f, c, 0f, 0f, offset,
                    0f, 0f, c, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.HIGH_CONTRAST -> {
                val c = 1.45f
                val offset = 128f * (1f - c)
                target.set(floatArrayOf(
                    c, 0f, 0f, 0f, offset,
                    0f, c, 0f, 0f, offset,
                    0f, 0f, c, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.NATURAL -> {
                target.setSaturation(1.15f)
                target.postConcat(ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 5f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 1.05f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.SEPIA -> {
                target.set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterPreset.ORIGINAL -> {}
        }

        if (intensity >= 0.99f) return target

        // Interpolate between Identity and Target matrix based on intensity
        val identity = ColorMatrix()
        val idArr = identity.array
        val targetArr = target.array
        val blendedArr = FloatArray(20)
        for (i in 0 until 20) {
            blendedArr[i] = idArr[i] * (1f - intensity) + targetArr[i] * intensity
        }
        return ColorMatrix(blendedArr)
    }

    private fun applyVignette(source: Bitmap, vignetteStrength: Float): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, 0f, 0f, null)

        val radius = max(width, height) * 0.75f
        val alpha = (vignetteStrength / 100f * 200).toInt().coerceIn(0, 230)
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f,
                height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        return output
    }

    private fun applySharpen(source: Bitmap, sharpenAmount: Float): Bitmap {
        // High-speed 3x3 unsharp mask approximation
        val factor = (sharpenAmount / 100f) * 0.7f
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, 0f, 0f, null)

        // Subtle high-contrast blend
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1f + factor, 0f, 0f, 0f, -factor * 60f,
                0f, 1f + factor, 0f, 0f, -factor * 60f,
                0f, 0f, 1f + factor, 0f, -factor * 60f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        canvas.drawBitmap(output, 0f, 0f, paint)
        return output
    }

    suspend fun saveEditedImage(
        context: Context,
        originalUri: Uri,
        params: EditParameters
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Decode full-resolution image (with safe bounds for device memory)
            val fullBitmap = decodeSampledBitmapFromUri(context, originalUri, 3840, 2160)
                ?: return@withContext Result.failure(Exception("Could not load original image"))

            val processed = applyEdits(fullBitmap, params)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "PureGallery_$timeStamp.jpg"

            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PureGallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val savedUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            contentResolver.openOutputStream(savedUri)?.use { outStream ->
                processed.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(savedUri, contentValues, null, null)
            }

            Result.success(savedUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
