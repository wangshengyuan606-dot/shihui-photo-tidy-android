package com.example.phototidy.data

import android.content.Context
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.PhotoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PhotoLocationExtractor(
    private val context: Context,
) {
    suspend fun extractLocations(
        media: List<PhotoItem>,
        maxItems: Int = 160,
    ): Map<String, PhotoLocation> = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = linkedMapOf<String, PhotoLocation>()

        media
            .asSequence()
            .filter { it.mediaType == MediaType.PHOTO }
            .take(maxItems)
            .forEach { item ->
                coroutineContext.ensureActive()
                val coordinates = readCoordinates(item.uri) ?: return@forEach
                val address = reverseGeocode(
                    geocoder = geocoder,
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                )
                results[item.stableId] = PhotoLocation(
                    mediaId = item.stableId,
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                    country = address?.countryName,
                    province = address?.adminArea,
                    city = address?.locality ?: address?.subAdminArea,
                    district = address?.subLocality,
                    poiName = address?.featureName,
                )
            }

        results
    }

    private fun readCoordinates(uriString: String): Pair<Double, Double>? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val preferredUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
        } else {
            uri
        }

        return readCoordinatesFromUri(preferredUri) ?: readCoordinatesFromUri(uri)
    }

    private fun readCoordinatesFromUri(uri: Uri): Pair<Double, Double>? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    latLong[0].toDouble() to latLong[1].toDouble()
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private suspend fun reverseGeocode(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCoroutine { continuation ->
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                continuation.resume(addresses.firstOrNull())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        runCatching {
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }.getOrNull()
    }
}
