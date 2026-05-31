package com.data.offline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max

/**
 * Stores article assets in app-private storage so downloaded articles remain usable offline.
 */
class OfflineAssetStore(
    context: Context
) {
    private val appContext = context.applicationContext
    private val imageDir: File by lazy {
        File(appContext.filesDir, "offline_article_images").apply { mkdirs() }
    }

    suspend fun saveCoverImage(articleUrl: String, imageUrl: String?, quality: ImageQuality): StoredAsset? {
        val safeImageUrl = imageUrl?.trim().orEmpty()
        if (quality == ImageQuality.TEXT_ONLY || safeImageUrl.isBlank()) return null
        if (!safeImageUrl.startsWith("https://", ignoreCase = true)) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val output = File(imageDir, "${sha256(articleUrl)}.jpg")

                if (!output.exists() || output.length() == 0L) {
                    val connection = (URL(safeImageUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 12_000
                        readTimeout = 15_000
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "NewsReaderAndroid/1.0")
                    }

                    try {
                        val code = connection.responseCode
                        if (code !in 200..299) return@runCatching null

                        val saved = when (quality) {
                            ImageQuality.LITE -> saveCompressedLiteImage(connection, output)
                            ImageQuality.FULL -> saveFullImageWithoutTruncation(connection, output, FULL_IMAGE_MAX_BYTES)
                            ImageQuality.TEXT_ONLY -> false
                        }
                        if (!saved) output.delete()
                    } finally {
                        connection.disconnect()
                    }
                }

                if (output.exists() && output.length() > 0L) {
                    StoredAsset(Uri.fromFile(output).toString(), output.length())
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    private fun saveCompressedLiteImage(connection: HttpURLConnection, output: File): Boolean {
        val bitmap = connection.inputStream.use { input -> BitmapFactory.decodeStream(input) } ?: return false
        val scaled = scaleDown(bitmap, maxDimension = LITE_MAX_DIMENSION)
        return output.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, LITE_JPEG_QUALITY, out)
        }.also {
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }
    }

    private fun saveFullImageWithoutTruncation(
        connection: HttpURLConnection,
        output: File,
        maxBytes: Long
    ): Boolean {
        val contentLength = connection.contentLengthLong
        if (contentLength > maxBytes) {
            return saveCompressedLiteImage(connection, output)
        }

        val temp = File(output.parentFile, "${output.name}.tmp")
        var copied = 0L
        connection.inputStream.use { input ->
            temp.outputStream().use { outputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    copied += read
                    if (copied > maxBytes) {
                        temp.delete()
                        return false
                    }
                    outputStream.write(buffer, 0, read)
                }
            }
        }

        if (copied <= 0L) {
            temp.delete()
            return false
        }
        if (output.exists()) output.delete()
        return temp.renameTo(output)
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / longestSide.toFloat()
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    suspend fun deleteCoverForArticle(articleUrl: String) = withContext(Dispatchers.IO) {
        if (articleUrl.isBlank()) return@withContext
        imageDir.listFiles { file -> file.nameWithoutExtension == sha256(articleUrl) }
            ?.forEach { it.delete() }
    }

    suspend fun deleteAllOfflineImages() = withContext(Dispatchers.IO) {
        imageDir.listFiles()?.forEach { it.delete() }
    }

    fun offlineImageBytes(): Long {
        return imageDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    enum class ImageQuality { FULL, LITE, TEXT_ONLY }

    data class StoredAsset(
        val uri: String,
        val sizeBytes: Long
    )

    companion object {
        private const val FULL_IMAGE_MAX_BYTES = 2_500_000L
        private const val LITE_MAX_DIMENSION = 720
        private const val LITE_JPEG_QUALITY = 76
    }
}
