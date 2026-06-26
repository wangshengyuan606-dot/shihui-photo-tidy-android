package com.example.phototidy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoContentAnalysis
import com.example.phototidy.model.PhotoItem
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PhotoContentAnalyzer(
    private val context: Context,
) {
    suspend fun analyze(
        media: List<PhotoItem>,
        maxItems: Int = DEFAULT_BATCH_SIZE,
    ): Map<String, PhotoContentAnalysis> = withContext(Dispatchers.IO) {
        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(LABEL_CONFIDENCE)
                .build(),
        )
        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.08f)
                .build(),
        )

        try {
            val results = linkedMapOf<String, PhotoContentAnalysis>()
            media
                .asSequence()
                .filter { it.mediaType == MediaType.PHOTO }
                .take(maxItems)
                .forEach { item ->
                    coroutineContext.ensureActive()
                    val bitmap = runCatching {
                        decodeSampledBitmap(item.uri)
                    }.getOrNull() ?: return@forEach

                    try {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val labels = runCatching { labeler.process(image).awaitTask() }.getOrDefault(emptyList())
                        val faces = runCatching { faceDetector.process(image).awaitTask() }.getOrDefault(emptyList())
                    val normalizedLabels = labels
                        .filter { it.confidence >= LABEL_CONFIDENCE }
                        .map { it.text.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    results[item.stableId] = PhotoContentAnalysis(
                        mediaId = item.stableId,
                        labels = normalizedLabels,
                        aliases = buildAliases(
                            labels = normalizedLabels,
                            faceCount = faces.size,
                        ),
                        faceCount = faces.size,
                    )
                    } finally {
                        bitmap.recycle()
                    }
                }
            results
        } finally {
            labeler.close()
            faceDetector.close()
        }
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

    private fun buildAliases(
        labels: List<String>,
        faceCount: Int,
    ): List<String> {
        val aliases = linkedSetOf<String>()

        labels.forEach { label ->
            aliases += label
            aliases += label.lowercase()
            aliases += LabelAliases[label.lowercase()].orEmpty()
        }

        if (faceCount > 0) {
            aliases += listOf("人", "人物", "人像", "人脸", "脸", "自拍", "portrait", "people", "person", "face")
        }
        if (faceCount > 1) {
            aliases += listOf("合照", "多人", "group", "group photo")
        }

        return aliases
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { throwable ->
            continuation.resumeWithException(throwable)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

    private companion object {
        const val LABEL_CONFIDENCE = 0.62f
        const val DEFAULT_BATCH_SIZE = 40
        const val DECODE_TARGET_SIZE = 768

        val LabelAliases = mapOf(
            "landscape" to listOf("风景", "景色", "自然", "户外", "山水", "风光"),
            "sky" to listOf("天空", "蓝天", "云", "风景", "户外"),
            "cloud" to listOf("云", "云朵", "天空", "风景"),
            "mountain" to listOf("山", "山景", "风景", "自然"),
            "beach" to listOf("海滩", "沙滩", "海边", "风景"),
            "sea" to listOf("海", "大海", "海边", "风景"),
            "ocean" to listOf("海", "大海", "海边", "风景"),
            "lake" to listOf("湖", "湖泊", "水边", "风景"),
            "river" to listOf("河", "河流", "水边", "风景"),
            "forest" to listOf("森林", "树林", "树", "自然", "风景"),
            "tree" to listOf("树", "树林", "植物", "自然", "风景"),
            "flower" to listOf("花", "鲜花", "植物", "自然"),
            "plant" to listOf("植物", "绿植", "花草", "自然"),
            "sunset" to listOf("夕阳", "日落", "晚霞", "风景"),
            "sunrise" to listOf("日出", "朝霞", "风景"),
            "animal" to listOf("动物", "宠物"),
            "pet" to listOf("宠物", "猫", "狗"),
            "dog" to listOf("狗", "小狗", "宠物", "动物"),
            "cat" to listOf("猫", "小猫", "宠物", "动物"),
            "bird" to listOf("鸟", "动物", "宠物"),
            "food" to listOf("食物", "美食", "吃的", "饭", "菜"),
            "meal" to listOf("饭", "餐", "食物", "美食"),
            "dessert" to listOf("甜点", "蛋糕", "食物", "美食"),
            "drink" to listOf("饮料", "喝的", "咖啡", "奶茶"),
            "car" to listOf("汽车", "车", "车辆"),
            "vehicle" to listOf("车辆", "汽车", "车"),
            "building" to listOf("建筑", "楼", "房子", "城市"),
            "city" to listOf("城市", "街景", "建筑"),
            "street" to listOf("街道", "街景", "城市"),
            "night" to listOf("夜景", "晚上", "夜晚"),
            "selfie" to listOf("自拍", "人像", "人物"),
        )
    }
}
