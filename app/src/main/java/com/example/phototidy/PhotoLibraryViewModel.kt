package com.example.phototidy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.phototidy.data.MediaStorePhotoRepository
import com.example.phototidy.data.PhotoContentAnalyzer
import com.example.phototidy.data.PhotoLocationExtractor
import com.example.phototidy.data.SearchIndexStore
import com.example.phototidy.data.SimilarPhotoDetector
import com.example.phototidy.model.AlbumGroup
import com.example.phototidy.model.DateGroup
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PersonCluster
import com.example.phototidy.model.PhotoContentAnalysis
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.PhotoLocation
import com.example.phototidy.model.SimilarGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class PhotoAccessStatus {
    None,
    Partial,
    Full,
}

data class PhotoLibraryUiState(
    val accessStatus: PhotoAccessStatus = PhotoAccessStatus.None,
    val media: List<PhotoItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val keptIds: Set<String> = emptySet(),
    val recycleBinIds: Set<String> = emptySet(),
    val permanentlyDeletedIds: Set<String> = emptySet(),
    val photoTags: Map<String, String> = emptyMap(),
    val personClusters: List<PersonCluster> = emptyList(),
    val photoLocations: Map<String, PhotoLocation> = emptyMap(),
    val contentAnalysis: Map<String, PhotoContentAnalysis> = emptyMap(),
    val similarGroups: List<SimilarGroup> = emptyList(),
    val isScanningSimilarPhotos: Boolean = false,
    val isScanningLocations: Boolean = false,
    val isScanningContent: Boolean = false,
) {
    val photos: List<PhotoItem> get() = media
    val visibleMedia: List<PhotoItem> get() = media.filter {
        it.stableId !in recycleBinIds && it.stableId !in permanentlyDeletedIds
    }
    val selectedMedia: List<PhotoItem> get() = visibleMedia.filter { it.stableId in selectedIds }
    val recycleBinMedia: List<PhotoItem> get() = media.filter { it.stableId in recycleBinIds }
    val untidyPhotos: List<PhotoItem> get() = visibleMedia.filter { it.stableId !in keptIds }

    val totalBytes: Long get() = visibleMedia.sumOf { it.sizeBytes }
    val totalMediaCount: Int get() = visibleMedia.size
    val photoCount: Int get() = visibleMedia.count { it.mediaType == MediaType.PHOTO }
    val videoCount: Int get() = visibleMedia.count { it.mediaType == MediaType.VIDEO }
    val selectedBytes: Long get() = selectedMedia.sumOf { it.sizeBytes }
    val keptCount: Int get() = keptIds.size
    val recycleBinCount: Int get() = recycleBinIds.size
    val recycleBinBytes: Long get() = recycleBinMedia.sumOf { it.sizeBytes }
    val permanentlyDeletedCount: Int get() = permanentlyDeletedIds.size
    val reviewedCount: Int get() = (keptIds + recycleBinIds + permanentlyDeletedIds).size
    val organizeProgress: Float get() {
        if (media.isEmpty()) return 0f
        return (reviewedCount.toFloat() / media.size.toFloat()).coerceIn(0f, 1f)
    }

    val albumGroups: List<AlbumGroup> get() = visibleMedia
        .groupBy { it.bucketId.ifBlank { it.bucketName.ifBlank { "other" } } }
        .map { (bucketId, items) ->
            val first = items.maxByOrNull { it.dateTakenMillis } ?: items.first()
            AlbumGroup(
                bucketId = bucketId,
                bucketName = first.bucketName.ifBlank { "其他" },
                mediaCount = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                coverUri = first.uri,
            )
        }
        .sortedWith(compareByDescending<AlbumGroup> { it.mediaCount }.thenBy { it.bucketName })

    val dateGroups: List<DateGroup> get() {
        val calendar = Calendar.getInstance()
        return visibleMedia
            .filter { it.dateTakenMillis > 0L }
            .groupBy { item ->
                calendar.timeInMillis = item.dateTakenMillis
                calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH) + 1
            }
            .map { (yearMonth, items) ->
                val organized = items.count { item ->
                    item.stableId in keptIds || item.stableId in recycleBinIds || item.stableId in permanentlyDeletedIds
                }
                DateGroup(
                    year = yearMonth.first,
                    month = yearMonth.second,
                    mediaCount = items.size,
                    organizedCount = organized,
                    progress = if (items.isEmpty()) 0f else organized.toFloat() / items.size,
                    totalSizeBytes = items.sumOf { it.sizeBytes },
                )
            }
            .sortedWith(compareByDescending<DateGroup> { it.year }.thenByDescending { it.month })
    }

    fun mediaForDateGroup(group: DateGroup): List<PhotoItem> {
        val calendar = Calendar.getInstance(Locale.getDefault())
        return visibleMedia.filter { item ->
            if (item.dateTakenMillis <= 0L) return@filter false
            calendar.timeInMillis = item.dateTakenMillis
            calendar.get(Calendar.YEAR) == group.year &&
                calendar.get(Calendar.MONTH) + 1 == group.month
        }
    }

    fun mediaForAlbumGroup(group: AlbumGroup): List<PhotoItem> {
        return visibleMedia.filter {
            val bucketId = it.bucketId.ifBlank { it.bucketName.ifBlank { "other" } }
            bucketId == group.bucketId
        }
    }
}

class PhotoLibraryViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = MediaStorePhotoRepository(application)
    private val similarPhotoDetector = SimilarPhotoDetector(application)
    private val photoLocationExtractor = PhotoLocationExtractor(application)
    private val photoContentAnalyzer = PhotoContentAnalyzer(application)
    private val searchIndexStore = SearchIndexStore(application)
    private var similarDetectionJob: Job? = null
    private var locationExtractionJob: Job? = null
    private var contentAnalysisJob: Job? = null

    private val _uiState = MutableStateFlow(PhotoLibraryUiState())
    val uiState: StateFlow<PhotoLibraryUiState> = _uiState.asStateFlow()

    fun refresh(accessStatus: PhotoAccessStatus) {
        if (accessStatus == PhotoAccessStatus.None) {
            similarDetectionJob?.cancel()
            locationExtractionJob?.cancel()
            contentAnalysisJob?.cancel()
            _uiState.value = PhotoLibraryUiState(accessStatus = accessStatus)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    accessStatus = accessStatus,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.loadMedia(
                    limit = 500,
                    includeImages = accessStatus != PhotoAccessStatus.None,
                    includeVideos = accessStatus == PhotoAccessStatus.Full,
                )
            }.onSuccess { media ->
                val existing = _uiState.value
                val availableIds = media.map { it.stableId }.toSet()
                val cachedContent = searchIndexStore.loadContentAnalysis(media)
                val cachedLocation = searchIndexStore.loadLocationCache(media)
                val cachedPeople = searchIndexStore.loadPersonClusters(media)
                val photoIds = media
                    .filter { it.mediaType == MediaType.PHOTO }
                    .map { it.stableId }
                    .toSet()
                val shouldScanSimilar = media.count { it.mediaType == MediaType.PHOTO } >= 2
                val shouldScanLocations = photoIds.any { it !in cachedLocation.indexedIds }
                val shouldScanContent = photoIds.any { it !in cachedContent.keys }
                _uiState.value = existing.copy(
                    accessStatus = accessStatus,
                    media = media,
                    isLoading = false,
                    errorMessage = null,
                    selectedIds = existing.selectedIds.intersect(availableIds),
                    keptIds = existing.keptIds.intersect(availableIds),
                    recycleBinIds = existing.recycleBinIds.intersect(availableIds),
                    permanentlyDeletedIds = existing.permanentlyDeletedIds.intersect(availableIds),
                    photoTags = existing.photoTags.filterKeys { it in availableIds },
                    personClusters = existing.personClusters.filter { cluster ->
                        cluster.mediaIds.any { it in availableIds }
                    }.mergeNamedPeople(cachedPeople),
                    photoLocations = cachedLocation.locations,
                    contentAnalysis = cachedContent,
                    similarGroups = emptyList(),
                    isScanningSimilarPhotos = shouldScanSimilar,
                    isScanningLocations = shouldScanLocations,
                    isScanningContent = shouldScanContent,
                )
                startSimilarDetection(media)
                startLocationExtraction(
                    media = media,
                    cachedLocations = cachedLocation.locations,
                    cachedIndexedIds = cachedLocation.indexedIds,
                )
                startContentAnalysis(
                    media = media,
                    cachedAnalysis = cachedContent,
                )
            }.onFailure { throwable ->
                similarDetectionJob?.cancel()
                locationExtractionJob?.cancel()
                contentAnalysisJob?.cancel()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "读取相册失败",
                        similarGroups = emptyList(),
                        isScanningSimilarPhotos = false,
                        isScanningLocations = false,
                        isScanningContent = false,
                    )
                }
            }
        }
    }

    fun toggleSelection(item: PhotoItem) {
        _uiState.update { state ->
            val id = item.stableId
            state.copy(
                selectedIds = if (id in state.selectedIds) {
                    state.selectedIds - id
                } else {
                    state.selectedIds + id
                },
            )
        }
    }

    fun selectOnly(items: List<PhotoItem>) {
        _uiState.update { state ->
            state.copy(selectedIds = items.map { it.stableId }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun addTag(item: PhotoItem, tag: String) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isEmpty()) return
        _uiState.update { state ->
            state.copy(photoTags = state.photoTags + (item.stableId to normalizedTag))
        }
    }

    fun renamePersonCluster(personId: String, displayName: String) {
        val normalizedName = displayName.trim()
        if (normalizedName.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                personClusters = state.personClusters.map { cluster ->
                    if (cluster.personId == personId) {
                        cluster.copy(displayName = normalizedName)
                    } else {
                        cluster
                    }
                },
            )
        }
        searchIndexStore.savePersonClusters(_uiState.value.personClusters)
    }

    fun markKept(item: PhotoItem) {
        _uiState.update { state ->
            state.copy(
                keptIds = state.keptIds + item.stableId,
                selectedIds = state.selectedIds - item.stableId,
            )
        }
    }

    fun moveSelectedToRecycleBin(sourcePage: String = "tidy") {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                recycleBinIds = state.recycleBinIds + ids,
                keptIds = state.keptIds - ids,
                selectedIds = emptySet(),
            )
        }
    }

    fun moveToRecycleBin(item: PhotoItem) {
        _uiState.update { state ->
            state.copy(
                recycleBinIds = state.recycleBinIds + item.stableId,
                keptIds = state.keptIds - item.stableId,
                selectedIds = state.selectedIds - item.stableId,
            )
        }
    }

    fun restoreFromRecycleBin(item: PhotoItem) {
        _uiState.update { state ->
            state.copy(recycleBinIds = state.recycleBinIds - item.stableId)
        }
    }

    fun clearRecycleBin() {
        _uiState.update { it.copy(recycleBinIds = emptySet()) }
    }

    fun confirmPermanentDeleteFromRecycleBin() {
        val ids = _uiState.value.recycleBinIds
        if (ids.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                recycleBinIds = emptySet(),
                permanentlyDeletedIds = state.permanentlyDeletedIds + ids,
                selectedIds = state.selectedIds - ids,
            )
        }
    }

    fun confirmPermanentDeleteSelectedFromRecycleBin(items: List<PhotoItem>) {
        val ids = items.map { it.stableId }.toSet()
        if (ids.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                recycleBinIds = state.recycleBinIds - ids,
                permanentlyDeletedIds = state.permanentlyDeletedIds + ids,
                selectedIds = state.selectedIds - ids,
            )
        }
    }

    fun clearLocalRecords() {
        _uiState.update {
            it.copy(
                selectedIds = emptySet(),
                keptIds = emptySet(),
                recycleBinIds = emptySet(),
                permanentlyDeletedIds = emptySet(),
                photoTags = emptyMap(),
                personClusters = emptyList(),
                photoLocations = emptyMap(),
                contentAnalysis = emptyMap(),
            )
        }
    }

    private fun startSimilarDetection(media: List<PhotoItem>) {
        similarDetectionJob?.cancel()
        if (media.count { it.mediaType == MediaType.PHOTO } < 2) {
            _uiState.update {
                it.copy(
                    similarGroups = emptyList(),
                    isScanningSimilarPhotos = false,
                )
            }
            return
        }

        val mediaIdsAtStart = media.map { it.stableId }.toSet()
        similarDetectionJob = viewModelScope.launch {
            try {
                val groups = similarPhotoDetector.findSimilarGroups(media)
                _uiState.update { state ->
                    val currentIds = state.media.map { it.stableId }.toSet()
                    if (currentIds != mediaIdsAtStart) {
                        state
                    } else {
                        state.copy(
                            similarGroups = groups.filter { group ->
                                group.mediaIds.count { it in currentIds } >= 2
                            },
                            isScanningSimilarPhotos = false,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        similarGroups = emptyList(),
                        isScanningSimilarPhotos = false,
                    )
                }
            }
        }
    }

    private fun startLocationExtraction(
        media: List<PhotoItem>,
        cachedLocations: Map<String, PhotoLocation>,
        cachedIndexedIds: Set<String>,
    ) {
        locationExtractionJob?.cancel()
        val missingMedia = media.filter {
            it.mediaType == MediaType.PHOTO && it.stableId !in cachedIndexedIds
        }
        if (missingMedia.isEmpty()) {
            _uiState.update {
                it.copy(
                    photoLocations = cachedLocations,
                    isScanningLocations = false,
                )
            }
            return
        }

        val mediaIdsAtStart = media.map { it.stableId }.toSet()
        locationExtractionJob = viewModelScope.launch {
            try {
                val newLocations = photoLocationExtractor.extractLocations(missingMedia)
                _uiState.update { state ->
                    val currentIds = state.media.map { it.stableId }.toSet()
                    if (currentIds != mediaIdsAtStart) {
                        state
                    } else {
                        val indexedIds = (cachedIndexedIds + missingMedia.map { it.stableId })
                            .filter { it in currentIds }
                            .toSet()
                        val locations = (cachedLocations + newLocations).filterKeys { it in currentIds }
                        searchIndexStore.saveLocationCache(
                            media = state.media,
                            indexedIds = indexedIds,
                            locations = locations,
                        )
                        state.copy(
                            photoLocations = locations,
                            isScanningLocations = false,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        photoLocations = cachedLocations,
                        isScanningLocations = false,
                    )
                }
            }
        }
    }

    private fun startContentAnalysis(media: List<PhotoItem>) {
        startContentAnalysis(media = media, cachedAnalysis = emptyMap())
    }

    private fun startContentAnalysis(
        media: List<PhotoItem>,
        cachedAnalysis: Map<String, PhotoContentAnalysis>,
    ) {
        contentAnalysisJob?.cancel()
        val missingMedia = media.filter {
            it.mediaType == MediaType.PHOTO && it.stableId !in cachedAnalysis
        }
        if (missingMedia.isEmpty()) {
            val faceMedia = cachedAnalysis.values
                .filter { it.faceCount > 0 }
                .map { it.mediaId }
            _uiState.update {
                it.copy(
                    contentAnalysis = cachedAnalysis,
                    personClusters = it.personClusters.withAutoPeopleCluster(
                        mediaIds = faceMedia,
                        faceCount = cachedAnalysis.values.sumOf { analysis -> analysis.faceCount },
                    ),
                    isScanningContent = false,
                )
            }
            return
        }

        val mediaIdsAtStart = media.map { it.stableId }.toSet()
        contentAnalysisJob = viewModelScope.launch {
            var latestAnalysis = cachedAnalysis
            try {
                delay(CONTENT_ANALYSIS_START_DELAY_MILLIS)

                var remainingMedia = missingMedia
                while (remainingMedia.isNotEmpty()) {
                    val batch = remainingMedia.take(CONTENT_ANALYSIS_BATCH_SIZE)
                    val newAnalysis = photoContentAnalyzer.analyze(
                        media = batch,
                        maxItems = CONTENT_ANALYSIS_BATCH_SIZE,
                    )
                    latestAnalysis += newAnalysis
                    remainingMedia = remainingMedia.drop(batch.size)

                    var shouldStop = false
                    _uiState.update { state ->
                        val currentIds = state.media.map { it.stableId }.toSet()
                        if (currentIds != mediaIdsAtStart) {
                            shouldStop = true
                            state
                        } else {
                            val existingNamedClusters = state.personClusters.filter { !it.displayName.isNullOrBlank() }
                            val filteredAnalysis = latestAnalysis.filterKeys { it in currentIds }
                            val faceMedia = filteredAnalysis.values
                                .filter { it.faceCount > 0 }
                                .map { it.mediaId }
                            val nextPeople = existingNamedClusters.withAutoPeopleCluster(
                                mediaIds = faceMedia,
                                faceCount = filteredAnalysis.values.sumOf { it.faceCount },
                            )
                            searchIndexStore.saveContentAnalysis(
                                media = state.media,
                                analysis = filteredAnalysis,
                            )
                            searchIndexStore.savePersonClusters(nextPeople)
                            state.copy(
                                contentAnalysis = filteredAnalysis,
                                personClusters = nextPeople,
                                isScanningContent = remainingMedia.isNotEmpty(),
                            )
                        }
                    }

                    if (shouldStop) return@launch
                    if (remainingMedia.isNotEmpty()) {
                        delay(CONTENT_ANALYSIS_BATCH_DELAY_MILLIS)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update { state ->
                    val cachedFaceMedia = latestAnalysis.values
                        .filter { it.faceCount > 0 }
                        .map { it.mediaId }
                    state.copy(
                        contentAnalysis = latestAnalysis,
                        personClusters = state.personClusters
                            .filter { !it.displayName.isNullOrBlank() }
                            .withAutoPeopleCluster(
                                mediaIds = cachedFaceMedia,
                                faceCount = latestAnalysis.values.sumOf { it.faceCount },
                            ),
                        isScanningContent = false,
                    )
                }
            }
        }
    }

    private companion object {
        const val CONTENT_ANALYSIS_BATCH_SIZE = 40
        const val CONTENT_ANALYSIS_START_DELAY_MILLIS = 1_200L
        const val CONTENT_ANALYSIS_BATCH_DELAY_MILLIS = 250L
    }
}

private fun List<PersonCluster>.mergeNamedPeople(cached: List<PersonCluster>): List<PersonCluster> {
    val merged = linkedMapOf<String, PersonCluster>()
    cached.filter { !it.displayName.isNullOrBlank() }.forEach { merged[it.personId] = it }
    filter { !it.displayName.isNullOrBlank() }.forEach { merged[it.personId] = it }
    return merged.values.toList()
}

private fun List<PersonCluster>.withAutoPeopleCluster(
    mediaIds: List<String>,
    faceCount: Int,
): List<PersonCluster> {
    val namedClusters = filter { !it.displayName.isNullOrBlank() }
    val distinctMediaIds = mediaIds.distinct()
    if (distinctMediaIds.isEmpty()) return namedClusters

    return namedClusters + PersonCluster(
        personId = "people-detected",
        displayName = null,
        coverMediaId = distinctMediaIds.first(),
        mediaIds = distinctMediaIds,
        faceCount = faceCount,
    )
}
