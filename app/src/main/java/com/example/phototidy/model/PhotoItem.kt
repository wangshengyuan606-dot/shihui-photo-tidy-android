package com.example.phototidy.model

enum class MediaType {
    PHOTO,
    VIDEO,
}

enum class ReviewGestureMode {
    Classic,
    UpDeleteLeftKeep,
}

data class PhotoItem(
    val id: Long,
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val dateLabel: String,
    val width: Int,
    val height: Int,
    val mimeType: String = "",
    val dateTakenMillis: Long = 0L,
    val mediaType: MediaType = MediaType.PHOTO,
    val durationMillis: Long? = null,
    val bucketId: String = "",
    val bucketName: String = "其他",
) {
    val stableId: String get() = "${mediaType.name}:$id"
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO
    val dateAdded: String get() = dateLabel
}

data class AlbumGroup(
    val bucketId: String,
    val bucketName: String,
    val mediaCount: Int,
    val totalSizeBytes: Long,
    val coverUri: String,
)

data class DateGroup(
    val year: Int,
    val month: Int,
    val mediaCount: Int,
    val organizedCount: Int,
    val progress: Float,
    val totalSizeBytes: Long,
) {
    val title: String get() = "%04d-%02d".format(year, month)
}

data class SimilarGroup(
    val groupId: String,
    val mediaIds: List<String>,
    val recommendedMediaId: String?,
)

data class PersonCluster(
    val personId: String,
    val displayName: String?,
    val coverMediaId: String,
    val mediaIds: List<String>,
    val faceCount: Int,
)

data class PhotoLocation(
    val mediaId: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,
    val poiName: String? = null,
)

data class PhotoContentAnalysis(
    val mediaId: String,
    val labels: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val faceCount: Int = 0,
    val hasPeople: Boolean = faceCount > 0,
)

data class RecycleBinItem(
    val mediaId: String,
    val deletedAt: Long,
    val sourcePage: String,
)

data class StorageStats(
    val totalPhotos: Int,
    val totalSizeBytes: Long,
    val duplicates: Int,
    val reviewedToday: Int,
    val deletedToday: Int,
)
