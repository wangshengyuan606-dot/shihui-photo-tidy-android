package com.example.phototidy.data

import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.StorageStats

object SampleData {
    val storageStats = StorageStats(
        totalPhotos = 1284,
        totalSizeBytes = 4_892_000_000L,
        duplicates = 23,
        reviewedToday = 47,
        deletedToday = 12,
    )

    val photos = listOf(
        PhotoItem(1, "https://picsum.photos/seed/1/800/1200", "IMG_20250601.jpg", 2_400_000, "2025-06-01", 4032, 3024),
        PhotoItem(2, "https://picsum.photos/seed/2/800/1200", "IMG_20250602.jpg", 1_800_000, "2025-06-02", 3024, 4032),
        PhotoItem(3, "https://picsum.photos/seed/3/800/1200", "IMG_20250603.jpg", 3_100_000, "2025-06-03", 4032, 3024),
        PhotoItem(4, "https://picsum.photos/seed/4/800/1200", "IMG_20250604.jpg", 980_000, "2025-06-04", 1920, 1080),
        PhotoItem(5, "https://picsum.photos/seed/5/800/1200", "IMG_20250605.jpg", 2_200_000, "2025-06-05", 4032, 3024),
        PhotoItem(6, "https://picsum.photos/seed/6/800/1200", "IMG_20250606.jpg", 1_500_000, "2025-06-06", 3024, 4032),
        PhotoItem(7, "https://picsum.photos/seed/7/800/1200", "IMG_20250607.jpg", 4_500_000, "2025-06-07", 4032, 3024),
        PhotoItem(8, "https://picsum.photos/seed/8/800/1200", "IMG_20250608.jpg", 760_000, "2025-06-08", 1920, 1080),
        PhotoItem(9, "https://picsum.photos/seed/9/800/1200", "IMG_20250609.jpg", 2_800_000, "2025-06-09", 4032, 3024),
        PhotoItem(10, "https://picsum.photos/seed/10/800/1200", "IMG_20250610.jpg", 1_100_000, "2025-06-10", 3024, 4032),
        PhotoItem(11, "https://picsum.photos/seed/11/800/1200", "IMG_20250611.jpg", 3_400_000, "2025-06-11", 4032, 3024),
        PhotoItem(12, "https://picsum.photos/seed/12/800/1200", "IMG_20250612.jpg", 890_000, "2025-06-12", 1920, 1080),
    )

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}
