package com.laxiang.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * 把拍照 / 相册来的原图就地压到长边 [MAX_DIMENSION]、JPEG 质量 [QUALITY]。
 *
 * 两个容易踩的点：
 * 1. 必须按 EXIF 方向转正 —— BitmapFactory 解码不会自动应用旋转，
 *    直接把带 `Orientation=6` 的原图重编码，照片会躺倒。
 * 2. 先按 inSampleSize 采样解码再精确缩放，避免一张 4000x3000 的解码直接把内存打爆。
 */
object ImageCompressor {

    const val MAX_DIMENSION = 1600
    const val QUALITY = 85

    /** 返回压缩后的文件大小（字节）；文件不存在或解码失败时返回原大小。 */
    fun compressInPlace(
        file: File,
        maxDimension: Int = MAX_DIMENSION,
        quality: Int = QUALITY
    ): Long {
        if (!file.isFile) return 0L
        val originalSize = file.length()

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return originalSize

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return originalSize
        bitmap = applyExifOrientation(file, bitmap)
        bitmap = scaleDown(bitmap, maxDimension)

        val written = runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
        }.isSuccess

        if (!bitmap.isRecycled) bitmap.recycle()
        return if (written) file.length() else originalSize
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxDimension) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        if (matrix.isIdentity) return bitmap

        val rotated = runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrNull()
        return if (rotated != null && rotated !== bitmap) {
            bitmap.recycle()
            rotated
        } else {
            bitmap
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        val scaled = runCatching {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        }.getOrNull() ?: return bitmap
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
