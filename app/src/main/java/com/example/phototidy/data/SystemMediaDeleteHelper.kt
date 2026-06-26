package com.example.phototidy.data

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android 11+ batch delete request adapted from PhotoSwipe / image-sorter-app (MIT License).
 * The MVP keeps deletion simulated in the app recycle bin; this helper is ready for final deletion later.
 */
class SystemMediaDeleteHelper(
    private val context: Context,
) {
    fun createDeleteIntentSender(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    suspend fun deleteDirectlyBeforeAndroid11(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return@withContext 0
        uris.sumOf { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) }.getOrDefault(0)
        }
    }
}
