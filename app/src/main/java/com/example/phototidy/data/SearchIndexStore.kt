package com.example.phototidy.data

import android.content.Context
import com.example.phototidy.model.PersonCluster
import com.example.phototidy.model.PhotoContentAnalysis
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.PhotoLocation
import org.json.JSONArray
import org.json.JSONObject

class SearchIndexStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadContentAnalysis(media: List<PhotoItem>): Map<String, PhotoContentAnalysis> {
        val mediaById = media.associateBy { it.stableId }
        val root = readObject(KEY_CONTENT)
        val results = linkedMapOf<String, PhotoContentAnalysis>()

        root.keys().forEach { mediaId ->
            val item = mediaById[mediaId] ?: return@forEach
            val entry = root.optJSONObject(mediaId) ?: return@forEach
            if (entry.optString(FIELD_SIGNATURE) != item.indexSignature()) return@forEach

            results[mediaId] = PhotoContentAnalysis(
                mediaId = mediaId,
                labels = entry.optJSONArray(FIELD_LABELS).toStringList(),
                aliases = entry.optJSONArray(FIELD_ALIASES).toStringList(),
                faceCount = entry.optInt(FIELD_FACE_COUNT, 0),
            )
        }

        return results
    }

    fun saveContentAnalysis(
        media: List<PhotoItem>,
        analysis: Map<String, PhotoContentAnalysis>,
    ) {
        val mediaById = media.associateBy { it.stableId }
        val root = JSONObject()
        analysis.forEach { (mediaId, itemAnalysis) ->
            val item = mediaById[mediaId] ?: return@forEach
            root.put(
                mediaId,
                JSONObject()
                    .put(FIELD_SIGNATURE, item.indexSignature())
                    .put(FIELD_LABELS, itemAnalysis.labels.toJsonArray())
                    .put(FIELD_ALIASES, itemAnalysis.aliases.toJsonArray())
                    .put(FIELD_FACE_COUNT, itemAnalysis.faceCount),
            )
        }
        prefs.edit().putString(KEY_CONTENT, root.toString()).apply()
    }

    fun loadLocationCache(media: List<PhotoItem>): LocationCache {
        val mediaById = media.associateBy { it.stableId }
        val root = readObject(KEY_LOCATIONS)
        val locations = linkedMapOf<String, PhotoLocation>()
        val indexedIds = linkedSetOf<String>()

        root.keys().forEach { mediaId ->
            val item = mediaById[mediaId] ?: return@forEach
            val entry = root.optJSONObject(mediaId) ?: return@forEach
            if (entry.optString(FIELD_SIGNATURE) != item.indexSignature()) return@forEach

            indexedIds += mediaId
            if (!entry.optBoolean(FIELD_HAS_LOCATION, false)) return@forEach

            val latitude = entry.optDouble(FIELD_LATITUDE, Double.NaN)
            val longitude = entry.optDouble(FIELD_LONGITUDE, Double.NaN)
            if (latitude.isNaN() || longitude.isNaN()) return@forEach

            locations[mediaId] = PhotoLocation(
                mediaId = mediaId,
                latitude = latitude,
                longitude = longitude,
                country = entry.optNullableString(FIELD_COUNTRY),
                province = entry.optNullableString(FIELD_PROVINCE),
                city = entry.optNullableString(FIELD_CITY),
                district = entry.optNullableString(FIELD_DISTRICT),
                poiName = entry.optNullableString(FIELD_POI),
            )
        }

        return LocationCache(
            locations = locations,
            indexedIds = indexedIds,
        )
    }

    fun saveLocationCache(
        media: List<PhotoItem>,
        indexedIds: Set<String>,
        locations: Map<String, PhotoLocation>,
    ) {
        val mediaById = media.associateBy { it.stableId }
        val root = JSONObject()
        indexedIds.forEach { mediaId ->
            val item = mediaById[mediaId] ?: return@forEach
            val location = locations[mediaId]
            val entry = JSONObject()
                .put(FIELD_SIGNATURE, item.indexSignature())
                .put(FIELD_HAS_LOCATION, location != null)
            if (location != null) {
                entry
                    .put(FIELD_LATITUDE, location.latitude)
                    .put(FIELD_LONGITUDE, location.longitude)
                    .put(FIELD_COUNTRY, location.country)
                    .put(FIELD_PROVINCE, location.province)
                    .put(FIELD_CITY, location.city)
                    .put(FIELD_DISTRICT, location.district)
                    .put(FIELD_POI, location.poiName)
            }
            root.put(mediaId, entry)
        }
        prefs.edit().putString(KEY_LOCATIONS, root.toString()).apply()
    }

    fun loadPersonClusters(media: List<PhotoItem>): List<PersonCluster> {
        val availableIds = media.map { it.stableId }.toSet()
        val array = readArray(KEY_PEOPLE)
        return buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val mediaIds = entry.optJSONArray(FIELD_MEDIA_IDS)
                    .toStringList()
                    .filter { it in availableIds }
                if (mediaIds.isEmpty()) continue
                val coverMediaId = entry.optString(FIELD_COVER_MEDIA_ID).takeIf { it in availableIds }
                    ?: mediaIds.first()
                add(
                    PersonCluster(
                        personId = entry.optString(FIELD_PERSON_ID),
                        displayName = entry.optNullableString(FIELD_DISPLAY_NAME),
                        coverMediaId = coverMediaId,
                        mediaIds = mediaIds,
                        faceCount = entry.optInt(FIELD_FACE_COUNT, mediaIds.size),
                    ),
                )
            }
        }
    }

    fun savePersonClusters(clusters: List<PersonCluster>) {
        val array = JSONArray()
        clusters
            .filter { !it.displayName.isNullOrBlank() }
            .forEach { cluster ->
                array.put(
                    JSONObject()
                        .put(FIELD_PERSON_ID, cluster.personId)
                        .put(FIELD_DISPLAY_NAME, cluster.displayName)
                        .put(FIELD_COVER_MEDIA_ID, cluster.coverMediaId)
                        .put(FIELD_MEDIA_IDS, cluster.mediaIds.toJsonArray())
                        .put(FIELD_FACE_COUNT, cluster.faceCount),
                )
            }
        prefs.edit().putString(KEY_PEOPLE, array.toString()).apply()
    }

    data class LocationCache(
        val locations: Map<String, PhotoLocation>,
        val indexedIds: Set<String>,
    )

    private fun PhotoItem.indexSignature(): String {
        return listOf(stableId, sizeBytes, dateTakenMillis, width, height, mimeType).joinToString("|")
    }

    private fun List<String>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { array.put(it) }
        return array
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun readObject(key: String): JSONObject {
        return runCatching {
            JSONObject(prefs.getString(key, "{}").orEmpty())
        }.getOrDefault(JSONObject())
    }

    private fun readArray(key: String): JSONArray {
        return runCatching {
            JSONArray(prefs.getString(key, "[]").orEmpty())
        }.getOrDefault(JSONArray())
    }

    private companion object {
        const val PREFS_NAME = "photo_tidy_search_index"
        const val KEY_CONTENT = "content_analysis"
        const val KEY_LOCATIONS = "photo_locations"
        const val KEY_PEOPLE = "person_clusters"

        const val FIELD_SIGNATURE = "signature"
        const val FIELD_LABELS = "labels"
        const val FIELD_ALIASES = "aliases"
        const val FIELD_FACE_COUNT = "faceCount"
        const val FIELD_HAS_LOCATION = "hasLocation"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_COUNTRY = "country"
        const val FIELD_PROVINCE = "province"
        const val FIELD_CITY = "city"
        const val FIELD_DISTRICT = "district"
        const val FIELD_POI = "poiName"
        const val FIELD_PERSON_ID = "personId"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_COVER_MEDIA_ID = "coverMediaId"
        const val FIELD_MEDIA_IDS = "mediaIds"
    }
}
