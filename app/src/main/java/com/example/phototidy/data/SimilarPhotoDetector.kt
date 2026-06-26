package com.example.phototidy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.SimilarGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

class SimilarPhotoDetector(
    private val context: Context,
) {
    suspend fun findSimilarGroups(
        media: List<PhotoItem>,
        maxGroups: Int = 40,
    ): List<SimilarGroup> = withContext(Dispatchers.IO) {
        val hashes = media
            .filter { it.mediaType == MediaType.PHOTO }
            .mapNotNull { item ->
                coroutineContext.ensureActive()
                val hash = runCatching { differenceHash(item.uri) }.getOrNull() ?: return@mapNotNull null
                HashedPhoto(item = item, hash = hash)
            }

        if (hashes.size < 2) return@withContext emptyList()

        val usedIds = mutableSetOf<String>()
        val groups = mutableListOf<SimilarGroup>()
        val ordered = hashes.sortedWith(
            compareByDescending<HashedPhoto> { it.item.dateTakenMillis }
                .thenByDescending { it.item.width.toLong() * it.item.height.toLong() },
        )

        for (base in ordered) {
            coroutineContext.ensureActive()
            if (base.item.stableId in usedIds) continue

            val matches = ordered
                .asSequence()
                .filter { it.item.stableId !in usedIds }
                .filter { candidate ->
                    candidate.item.stableId == base.item.stableId ||
                        areSimilar(base, candidate)
                }
                .map { it.item }
                .toList()

            if (matches.size < 2) continue

            val sorted = matches.sortedByDescending { it.recommendationScore() }
            val recommended = sorted.first()
            groups += SimilarGroup(
                groupId = "similar-${groups.size}-${base.hash.toString(16)}",
                mediaIds = sorted.map { it.stableId },
                recommendedMediaId = recommended.stableId,
            )
            usedIds += sorted.map { it.stableId }

            if (groups.size >= maxGroups) break
        }

        groups
    }

    private fun areSimilar(
        first: HashedPhoto,
        second: HashedPhoto,
    ): Boolean {
        val distance = hammingDistance(first.hash, second.hash)
        if (distance <= STRICT_DISTANCE) return true

        val sameDay = first.item.dateLabel == second.item.dateLabel
        val sameBucket = first.item.bucketId.isNotBlank() && first.item.bucketId == second.item.bucketId
        val closeTime = first.item.dateTakenMillis > 0L &&
            second.item.dateTakenMillis > 0L &&
            abs(first.item.dateTakenMillis - second.item.dateTakenMillis) <= BURST_WINDOW_MILLIS

        return when {
            distance <= SAME_CONTEXT_DISTANCE && (sameDay || closeTime) -> true
            distance <= LOOSE_DISTANCE && sameBucket && (sameDay || closeTime) -> true
            else -> false
        }
    }

    private fun differenceHash(uriString: String): Long? {
        val bitmap = decodeSampledBitmap(uriString) ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, HASH_WIDTH, HASH_HEIGHT, true)
        if (scaled != bitmap) bitmap.recycle()

        var hash = 0L
        var bit = 0
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                val left = scaled.getPixel(x, y).luma()
                val right = scaled.getPixel(x + 1, y).luma()
                if (left > right) {
                    hash = hash or (1L shl bit)
                }
                bit += 1
            }
        }
        scaled.recycle()
        return hash
    }

    private fun decodeSampledBitmap(uriString: String): Bitmap? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
    ): Int {
        var sampleSize = 1
        while (width / sampleSize > DECODE_TARGET_SIZE || height / sampleSize > DECODE_TARGET_SIZE) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun hammingDistance(
        first: Long,
        second: Long,
    ): Int = java.lang.Long.bitCount(first xor second)

    private fun Int.luma(): Int {
        val red = this shr 16 and 0xFF
        val green = this shr 8 and 0xFF
        val blue = this and 0xFF
        return (red * 299 + green * 587 + blue * 114) / 1000
    }

    private fun PhotoItem.recommendationScore(): Long {
        val resolutionScore = width.toLong().coerceAtLeast(0L) * height.toLong().coerceAtLeast(0L)
        val sizeScore = sizeBytes.coerceAtLeast(0L) / 16L
        val screenshotPenalty = if (isScreenshotLike()) 3_000_000_000L else 0L
        return resolutionScore + sizeScore + dateTakenMillis / 1_000L - screenshotPenalty
    }

    private fun PhotoItem.isScreenshotLike(): Boolean {
        return bucketName.contains("screenshot", ignoreCase = true) ||
            bucketName.contains("截屏") ||
            bucketName.contains("截图") ||
            name.contains("screenshot", ignoreCase = true)
    }

    private data class HashedPhoto(
        val item: PhotoItem,
        val hash: Long,
    )

    private companion object {
        const val HASH_WIDTH = 9
        const val HASH_HEIGHT = 8
        const val DECODE_TARGET_SIZE = 256
        const val STRICT_DISTANCE = 4
        const val SAME_CONTEXT_DISTANCE = 8
        const val LOOSE_DISTANCE = 10
        const val BURST_WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
