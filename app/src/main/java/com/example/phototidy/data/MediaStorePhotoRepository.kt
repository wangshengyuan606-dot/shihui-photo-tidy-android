package com.example.phototidy.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MediaStore query flow adapted from PhotoSwipe / image-sorter-app (MIT License).
 * Local grouping fields are extended for Photo Tidy's Date and Album workspace.
 */
class MediaStorePhotoRepository(
    private val context: Context,
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun loadMedia(
        limit: Int = 500,
        includeImages: Boolean = true,
        includeVideos: Boolean = true,
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PhotoItem>()
        if (includeImages) {
            results += runCatching { queryImages(limit) }.getOrDefault(emptyList())
        }
        if (includeVideos) {
            results += runCatching { queryVideos(limit) }.getOrDefault(emptyList())
        }
        results
            .sortedByDescending { it.dateTakenMillis }
            .take(limit)
    }

    private fun queryImages(limit: Int): List<PhotoItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val photos = mutableListOf<PhotoItem>()

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext() && photos.size < limit) {
                val id = cursor.getLong(idColumn)
                val dateTakenMillis = cursor.getLongOrNull(dateTakenColumn)
                val dateAddedSeconds = cursor.getLongOrNull(dateAddedColumn)
                val displayDateMillis = dateTakenMillis?.takeIf { it > 0L }
                    ?: dateAddedSeconds?.times(1000)
                    ?: 0L

                photos += PhotoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = cursor.getStringOrNull(nameColumn).orEmpty(),
                    sizeBytes = cursor.getLongOrNull(sizeColumn) ?: 0L,
                    dateLabel = formatDate(displayDateMillis),
                    width = cursor.getIntOrNull(widthColumn) ?: 0,
                    height = cursor.getIntOrNull(heightColumn) ?: 0,
                    mimeType = cursor.getStringOrNull(mimeTypeColumn).orEmpty(),
                    dateTakenMillis = displayDateMillis,
                    mediaType = MediaType.PHOTO,
                    bucketId = cursor.getStringOrNull(bucketIdColumn).orEmpty(),
                    bucketName = cursor.getStringOrNull(bucketNameColumn).orEmpty().ifBlank { "其他" },
                )
            }
        }

        return photos
    }

    private fun queryVideos(limit: Int): List<PhotoItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val videos = mutableListOf<PhotoItem>()

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext() && videos.size < limit) {
                val id = cursor.getLong(idColumn)
                val dateTakenMillis = cursor.getLongOrNull(dateTakenColumn)
                val dateAddedSeconds = cursor.getLongOrNull(dateAddedColumn)
                val displayDateMillis = dateTakenMillis?.takeIf { it > 0L }
                    ?: dateAddedSeconds?.times(1000)
                    ?: 0L

                videos += PhotoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = cursor.getStringOrNull(nameColumn).orEmpty(),
                    sizeBytes = cursor.getLongOrNull(sizeColumn) ?: 0L,
                    dateLabel = formatDate(displayDateMillis),
                    width = cursor.getIntOrNull(widthColumn) ?: 0,
                    height = cursor.getIntOrNull(heightColumn) ?: 0,
                    mimeType = cursor.getStringOrNull(mimeTypeColumn).orEmpty(),
                    dateTakenMillis = displayDateMillis,
                    mediaType = MediaType.VIDEO,
                    durationMillis = cursor.getLongOrNull(durationColumn),
                    bucketId = cursor.getStringOrNull(bucketIdColumn).orEmpty(),
                    bucketName = cursor.getStringOrNull(bucketNameColumn).orEmpty().ifBlank { "Videos" },
                )
            }
        }

        return videos
    }

    private fun formatDate(millis: Long): String {
        return if (millis > 0L) dateFormatter.format(Date(millis)) else "未知日期"
    }
}

private fun Cursor.getStringOrNull(columnIndex: Int): String? {
    return if (isNull(columnIndex)) null else getString(columnIndex)
}

private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
    return if (isNull(columnIndex)) null else getLong(columnIndex)
}

private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
    return if (isNull(columnIndex)) null else getInt(columnIndex)
}
