package com.example.phototidy.ui.screens.tidy

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.phototidy.R
import com.example.phototidy.PhotoAccessStatus
import com.example.phototidy.PhotoLibraryUiState
import com.example.phototidy.model.AlbumGroup
import com.example.phototidy.model.DateGroup
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.ReviewGestureMode
import com.example.phototidy.model.SimilarGroup
import com.example.phototidy.ui.components.edgeHorizontalBackGesture
import com.example.phototidy.ui.screens.home.formatBytes
import com.example.phototidy.ui.theme.PhotoTidyAndroidTheme
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TidyMode {
    Date,
    Album,
    Similar,
}

private enum class MediaFilter {
    All,
    Similar,
    Photo,
    Video,
    Screenshot,
    Other,
}

private enum class MonthDisplayMode {
    Calendar,
    Stack,
}

@Composable
fun TidyScreen(
    uiState: PhotoLibraryUiState,
    initialAlbumId: String? = null,
    onInitialAlbumHandled: () -> Unit = {},
    onKeep: (PhotoItem) -> Unit,
    onMoveToRecycleBin: (PhotoItem) -> Unit,
    onToggleSelection: (PhotoItem) -> Unit,
    onSelectOnly: (PhotoItem) -> Unit,
    onSelectItems: (List<PhotoItem>) -> Unit = { items -> items.firstOrNull()?.let(onSelectOnly) },
    onClearSelection: () -> Unit,
    onCompressSelected: () -> Unit,
    onMoveSelectedToRecycleBin: () -> Unit,
    reviewGestureMode: ReviewGestureMode = ReviewGestureMode.Classic,
    onArchiveToTag: (PhotoItem, String) -> Unit = { _, _ -> },
    contentPadding: PaddingValues,
    onChromeHiddenChange: (Boolean) -> Unit = {},
) {
    var mode by rememberSaveable { mutableStateOf(TidyMode.Date) }
    var filter by rememberSaveable { mutableStateOf(MediaFilter.All) }
    var openedDateTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var openedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewSourceTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var similarReviewGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewStartIndex by rememberSaveable { mutableIntStateOf(0) }

    val visibleMedia = uiState.visibleMedia.applyFilter(filter)
    val organizedIds = uiState.keptIds + uiState.recycleBinIds + uiState.permanentlyDeletedIds
    val dateGroups = visibleMedia.toDateGroups(organizedIds)
    val albumGroups = visibleMedia.toAlbumGroups()
    val similarGroups = uiState.toSimilarPhotoClusters(filter)
    val tagFolders = remember(uiState.photoTags) {
        (listOf("收藏", "家庭", "旅行", "工作", "待整理") + uiState.photoTags.values)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)
    }
    val openedDate = dateGroups.firstOrNull { it.title == openedDateTitle }
    val openedAlbum = albumGroups.firstOrNull { it.bucketId == openedAlbumId }
    val openedSimilarReview = similarGroups.firstOrNull { it.groupId == similarReviewGroupId }
    val isMonthDetail = openedDate != null
    val groupMedia = when {
        openedDate != null -> uiState.mediaForDateGroup(openedDate).applyFilter(filter)
        openedAlbum != null -> uiState.mediaForAlbumGroup(openedAlbum).applyFilter(filter)
        else -> null
    }
    val reviewMedia = openedSimilarReview?.items ?: groupMedia

    LaunchedEffect(initialAlbumId, albumGroups) {
        val targetAlbumId = initialAlbumId ?: return@LaunchedEffect
        if (albumGroups.any { it.bucketId == targetAlbumId }) {
            mode = TidyMode.Album
            openedDateTitle = null
            openedAlbumId = targetAlbumId
            onInitialAlbumHandled()
        }
    }

    LaunchedEffect(isMonthDetail) {
        onChromeHiddenChange(isMonthDetail)
    }

    DisposableEffect(Unit) {
        onDispose { onChromeHiddenChange(false) }
    }

    fun closeOpenedGroup() {
        onClearSelection()
        openedDateTitle = null
        openedAlbumId = null
    }

    fun closeReview() {
        reviewSourceTitle = null
        similarReviewGroupId = null
    }

    if (reviewSourceTitle != null && reviewMedia != null) {
        BackHandler {
            closeReview()
        }
        Dialog(
            onDismissRequest = { closeReview() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            PhotoReviewScreen(
                title = reviewSourceTitle.orEmpty(),
                media = reviewMedia,
                startIndex = reviewStartIndex.coerceIn(0, reviewMedia.lastIndex.coerceAtLeast(0)),
                onClose = { closeReview() },
                onKeep = onKeep,
                onMoveToRecycleBin = onMoveToRecycleBin,
                gestureMode = reviewGestureMode,
                tagFolders = tagFolders,
                onArchiveToTag = onArchiveToTag,
            )
        }
        return
    }

    if (groupMedia != null) {
        val groupSelectedIds = uiState.selectedIds.intersect(groupMedia.map { it.stableId }.toSet())
        BackHandler {
            if (groupSelectedIds.isNotEmpty()) {
                onClearSelection()
            } else {
                closeOpenedGroup()
            }
        }
        if (openedDate != null) {
            val monthMediaByTitle = dateGroups.associate { dateGroup ->
                dateGroup.title to uiState.mediaForDateGroup(dateGroup).applyFilter(filter)
            }
            MonthDetailScreen(
                group = openedDate,
                dateGroups = dateGroups,
                monthMediaByTitle = monthMediaByTitle,
                selectedIds = uiState.selectedIds,
                onBack = {
                    closeOpenedGroup()
                },
                onOpenDateGroup = { nextGroup ->
                    onClearSelection()
                    openedDateTitle = nextGroup.title
                },
                onOpenReview = { dateGroup, index ->
                    openedDateTitle = dateGroup.title
                    reviewSourceTitle = dateGroup.title
                    reviewStartIndex = index
                },
                onToggleSelection = onToggleSelection,
                onBeginSelection = onSelectOnly,
                onClearSelection = onClearSelection,
                onCompressSelected = onCompressSelected,
                onDeleteSelected = onMoveSelectedToRecycleBin,
            )
            return
        }
        MediaGroupScreen(
            title = openedDate?.title ?: openedAlbum?.bucketName.orEmpty(),
            subtitle = "${groupMedia.size} 项 · ${formatBytes(groupMedia.sumOf { it.sizeBytes })}",
            media = groupMedia,
            selectedIds = groupSelectedIds,
            onBack = {
                closeOpenedGroup()
            },
            onOpenReview = { index ->
                reviewSourceTitle = openedDate?.title ?: openedAlbum?.bucketName.orEmpty()
                reviewStartIndex = index
            },
            onToggleSelection = onToggleSelection,
            onBeginSelection = onSelectOnly,
            onClearSelection = onClearSelection,
            onCompressSelected = onCompressSelected,
            onDeleteSelected = onMoveSelectedToRecycleBin,
            contentPadding = contentPadding,
        )
        return
    }

    TidyOverviewScreen(
        uiState = uiState,
        mode = mode,
        filter = filter,
        dateGroups = dateGroups,
        albumGroups = albumGroups,
        similarGroups = similarGroups,
        dateMediaByTitle = dateGroups.associate { group ->
            group.title to uiState.mediaForDateGroup(group).applyFilter(filter)
        },
        selectedCount = uiState.selectedMedia.size,
        selectedBytes = uiState.selectedBytes,
        onModeChange = { mode = it },
        onAdvanceMode = { mode = mode.next() },
        onFilterChange = { filter = it },
        onOpenDate = { openedDateTitle = it.title },
        onOpenAlbum = { openedAlbumId = it.bucketId },
        onSelectDate = { group ->
            val items = uiState.mediaForDateGroup(group).applyFilter(filter)
            if (items.isNotEmpty()) onSelectItems(items)
        },
        onSelectAlbum = { album ->
            val items = uiState.mediaForAlbumGroup(album).applyFilter(filter)
            if (items.isNotEmpty()) onSelectItems(items)
        },
        onOpenSimilarReview = { cluster, index ->
            mode = TidyMode.Similar
            openedDateTitle = null
            openedAlbumId = null
            similarReviewGroupId = cluster.groupId
            reviewSourceTitle = "相似照片"
            reviewStartIndex = index
        },
        onClearSelection = onClearSelection,
        onCompressSelected = onCompressSelected,
        onDeleteSelected = onMoveSelectedToRecycleBin,
        contentPadding = contentPadding,
    )
}

@Composable
private fun TidyOverviewScreen(
    uiState: PhotoLibraryUiState,
    mode: TidyMode,
    filter: MediaFilter,
    dateGroups: List<DateGroup>,
    albumGroups: List<AlbumGroup>,
    similarGroups: List<SimilarPhotoCluster>,
    dateMediaByTitle: Map<String, List<PhotoItem>>,
    selectedCount: Int,
    selectedBytes: Long,
    onModeChange: (TidyMode) -> Unit,
    onAdvanceMode: () -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onOpenDate: (DateGroup) -> Unit,
    onOpenAlbum: (AlbumGroup) -> Unit,
    onSelectDate: (DateGroup) -> Unit,
    onSelectAlbum: (AlbumGroup) -> Unit,
    onOpenSimilarReview: (SimilarPhotoCluster, Int) -> Unit,
    onClearSelection: () -> Unit,
    onCompressSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    contentPadding: PaddingValues,
) {
    val overviewListState = rememberLazyListState()
    val albumStartIndex = 3
    val focusedAlbumIndex by remember(overviewListState, mode, albumGroups.size) {
        derivedStateOf {
            if (mode != TidyMode.Album || albumGroups.isEmpty()) {
                0
            } else {
                val layoutInfo = overviewListState.layoutInfo
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val visibleAlbumItems = layoutInfo.visibleItemsInfo.filter { it.index >= albumStartIndex }
                if (!overviewListState.canScrollForward && visibleAlbumItems.any { it.index - albumStartIndex == albumGroups.lastIndex }) {
                    albumGroups.lastIndex
                } else {
                    visibleAlbumItems
                        .minByOrNull { itemInfo ->
                            abs(itemInfo.offset + itemInfo.size / 2 - viewportCenter)
                        }
                        ?.let { (it.index - albumStartIndex).coerceIn(0, albumGroups.lastIndex) }
                        ?: 0
                    }
            }
        }
    }
    var modeSwipeX by rememberSaveable { mutableFloatStateOf(0f) }
    val modePageOffset by animateFloatAsState(
        targetValue = modeSwipeX * 0.16f,
        animationSpec = tween(durationMillis = 260, easing = LiquidSwitchEasing),
        label = "tidy-mode-page-offset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .pointerInput(mode) {
                detectHorizontalDragGestures(
                    onDragCancel = { modeSwipeX = 0f },
                    onDragEnd = {
                        when {
                            modeSwipeX < -72f -> onModeChange(mode.next())
                            modeSwipeX > 72f -> onModeChange(mode.previous())
                        }
                        modeSwipeX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        modeSwipeX = (modeSwipeX + dragAmount).coerceIn(-160f, 160f)
                    },
                )
            }
            .padding(contentPadding),
    ) {
        LazyColumn(
            state = overviewListState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = modePageOffset },
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = if (mode == TidyMode.Album) 320.dp else 108.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = "整理",
                    color = Color(0xFF171717),
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            item {
                OverviewHeader(uiState = uiState)
            }
            item {
                SegmentedMode(
                    mode = mode,
                    onModeChange = onModeChange,
                    onAdvanceMode = onAdvanceMode,
                )
            }

            when (mode) {
                TidyMode.Date -> {
                    val byYear = dateGroups.groupBy { it.year }.toSortedMap(compareByDescending { it })
                    byYear.forEach { (year, groups) ->
                        item {
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF171717),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(
                            items = groups,
                            key = { it.title },
                        ) { group ->
                            MonthStackCard(
                                group = group,
                                color = group.monthCardColor(),
                                media = dateMediaByTitle[group.title].orEmpty(),
                                onClick = { onOpenDate(group) },
                                onLongClick = { onSelectDate(group) },
                            )
                        }
                    }
                }
                TidyMode.Album -> {
                    itemsIndexed(
                        items = albumGroups,
                        key = { _, album -> album.bucketId },
                    ) { index, album ->
                        AlbumFolderListCard(
                            album = album,
                            focusDistance = index - focusedAlbumIndex,
                            onClick = { onOpenAlbum(album) },
                            onLongClick = { onSelectAlbum(album) },
                        )
                    }
                }
                TidyMode.Similar -> {
                    if (similarGroups.isEmpty()) {
                        item {
                            EmptySimilarState(scanning = uiState.isScanningSimilarPhotos)
                        }
                    } else {
                        items(
                            items = similarGroups,
                            key = { it.groupId },
                        ) { cluster ->
                            SimilarPhotoRow(
                                cluster = cluster,
                                onOpenReview = { index -> onOpenSimilarReview(cluster, index) },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.recycleBinCount > 0) {
            FloatingRecycleSummary(
                count = uiState.recycleBinCount,
                bytes = uiState.recycleBinBytes,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 26.dp),
            )
        }

        if (selectedCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 18.dp)
                    .zIndex(5f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$selectedCount 项 · ${formatBytes(selectedBytes)}",
                    color = Color(0xFF6D6964),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.78f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                SelectionActionButton(
                    icon = Icons.Default.Settings,
                    label = "压缩",
                    color = Color(0xFF10A783),
                    onClick = onCompressSelected,
                )
                SelectionActionButton(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    color = Color(0xFFFF5B6A),
                    onClick = onDeleteSelected,
                )
                RoundIconButton(
                    icon = Icons.Default.Close,
                    onClick = onClearSelection,
                    buttonSize = 34.dp,
                    iconSize = 17.dp,
                    backgroundColor = Color(0x554D514D),
                )
            }
        }
    }
}

@Composable
private fun OverviewHeader(uiState: PhotoLibraryUiState) {
    val progress = uiState.organizeProgress.coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = uiState.reviewedCount.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFF171717),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "/${uiState.media.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF7A746D),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", uiState.organizeProgress * 100f),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF171717),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF171717),
                    modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.86f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(FigmaSelected),
            )
        }
    }
}

@Composable
private fun FilterIconRow(
    filter: MediaFilter,
    onFilterChange: (MediaFilter) -> Unit,
) {
    val filters = listOf(
        MediaFilter.All,
        MediaFilter.Similar,
        MediaFilter.Photo,
        MediaFilter.Video,
        MediaFilter.Screenshot,
        MediaFilter.Other,
    )
    val icons = listOf(
        Icons.Default.PhotoLibrary,
        Icons.Default.Star,
        Icons.Default.Photo,
        Icons.Default.VideoLibrary,
        Icons.Default.Folder,
        Icons.Default.Settings,
    )
    LiquidSwitchShell(
        selectedIndex = filters.indexOf(filter).coerceAtLeast(0),
        itemCount = filters.size,
        modifier = Modifier.fillMaxWidth(),
        height = 44.dp,
        containerColor = Color.White.copy(alpha = 0.34f),
        sliderColor = Color.White.copy(alpha = 0.92f),
        borderColor = Color.White.copy(alpha = 0.42f),
    ) {
        filters.forEachIndexed { index, item ->
            LiquidIconOption(
                icon = icons[index],
                selected = filter == item,
                onClick = { onFilterChange(item) },
            )
        }
    }
}

@Composable
private fun FilterIconButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(39.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.48f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF171717) else Color(0xFF7A746D),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun SegmentedMode(
    mode: TidyMode,
    onModeChange: (TidyMode) -> Unit,
    onAdvanceMode: () -> Unit,
) {
    val modes = listOf(TidyMode.Date, TidyMode.Album, TidyMode.Similar)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LiquidSwitchShell(
            selectedIndex = modes.indexOf(mode).coerceAtLeast(0),
            itemCount = modes.size,
            modifier = Modifier.weight(1f),
            height = 44.dp,
            containerColor = Color.White.copy(alpha = 0.62f),
            sliderColor = FigmaSelected.copy(alpha = 0.18f),
            borderColor = Color.White.copy(alpha = 0.42f),
        ) {
            LiquidTextOption("日期", mode == TidyMode.Date, activeColor = FigmaSelected) { onModeChange(TidyMode.Date) }
            LiquidTextOption("相册", mode == TidyMode.Album, activeColor = FigmaSelected) { onModeChange(TidyMode.Album) }
            LiquidTextOption("相似", mode == TidyMode.Similar, activeColor = FigmaSelected) { onModeChange(TidyMode.Similar) }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.62f))
                .clickable(onClick = onAdvanceMode),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF7A746D), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SegmentPill(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFFECE5DC) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF171717),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun LiquidSwitchShell(
    selectedIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    containerColor: Color = Color.White.copy(alpha = 0.48f),
    sliderColor: Color = Color.White.copy(alpha = 0.78f),
    borderColor: Color = Color.White.copy(alpha = 0.46f),
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .height(height)
                .padding(6.dp),
        ) {
            val itemWidth = maxWidth / itemCount
            val sliderX by animateDpAsState(
                targetValue = itemWidth * selectedIndex.toFloat(),
                animationSpec = tween(durationMillis = 550, easing = LiquidSwitchEasing),
                label = "liquid-switch-slider",
            )
            Box(
                modifier = Modifier
                    .offset(x = sliderX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(sliderColor),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun RowScope.LiquidIconOption(
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color = Color(0xFF171717),
    inactiveColor: Color = Color(0xFF7A746D),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "liquid-icon-press",
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun RowScope.LiquidTextOption(
    text: String,
    selected: Boolean,
    activeColor: Color = Color(0xFF171717),
    inactiveColor: Color = Color(0xFF7A746D),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "liquid-text-press",
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) activeColor else inactiveColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthStackCard(
    group: DateGroup,
    color: Color,
    media: List<PhotoItem>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.035f else 1f,
        label = "month-card-scale",
    )
    val lift by animateFloatAsState(
        targetValue = if (isPressed) -12f else 0f,
        label = "month-card-lift",
    )
    val progress = group.progress.coerceIn(0f, 1f)
    val photoCount = media.count { it.mediaType == MediaType.PHOTO }
    val videoCount = media.count { it.mediaType == MediaType.VIDEO }
    val cardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .height(176.dp)
            .padding(top = 0.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (media.isEmpty()) {
            MonthFolderSheet(
                color = color.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(42.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 2.dp),
            )
            MonthFolderSheet(
                color = color.copy(alpha = 0.62f),
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(48.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 14.dp),
            )
        } else {
            MonthPhotoGhosts(media = media, color = color)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .zIndex(10f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = lift
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            shape = cardShape,
            color = Color.Transparent,
            shadowElevation = if (isPressed) 18.dp else 8.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            0f to color.copy(alpha = 0.36f),
                            0.32f to color.copy(alpha = 0.66f),
                            0.58f to color.copy(alpha = 0.92f),
                            0.70f to color.copy(alpha = 1f),
                            1f to color,
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.38f), cardShape),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 22.dp, top = 18.dp, end = 92.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${group.month}月",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "${photoCount}张照片  ${videoCount}个视频",
                        color = Color.White.copy(alpha = 0.94f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "已整理 ${group.organizedCount}/${group.mediaCount} · ${formatBytes(group.totalSizeBytes)}",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MonthProgressRing(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun MonthFolderSheet(
    color: Color,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(color),
    )
}

@Composable
private fun BoxScope.MonthPhotoGhosts(
    media: List<PhotoItem>,
    color: Color,
) {
    val placements = listOf(
        StackPlacement(Alignment.TopStart, 20, -14, 86, 146, -7f),
        StackPlacement(Alignment.TopCenter, -58, -22, 86, 150, 3f),
        StackPlacement(Alignment.TopCenter, 18, -18, 86, 148, -2f),
        StackPlacement(Alignment.TopEnd, -20, -12, 86, 146, 7f),
    )
    media.take(4).forEachIndexed { index, item ->
        val placement = placements[index % placements.size]
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(placement.alignment)
                .offset(x = placement.x.dp, y = placement.y.dp)
                .size(placement.width.dp, placement.height.dp)
                .zIndex(index.toFloat())
                .graphicsLayer {
                    rotationZ = placement.rotation
                    shadowElevation = 8.dp.toPx()
                }
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.28f))
                .border(2.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(18.dp)),
        )
    }
}

@Composable
private fun MonthProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = Color.White.copy(alpha = 0.30f),
            strokeWidth = 5.dp,
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
            strokeWidth = 5.dp,
        )
        Text(
            text = "${(progress * 100).roundToInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AlbumDarkRow(
    album: AlbumGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = album.coverUri,
            contentDescription = album.bucketName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE7E1D8)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.bucketName,
                color = Color(0xFF171717),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${album.mediaCount} 项 · ${formatBytes(album.totalSizeBytes)}",
                color = Color(0xFF6D6964),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AlbumHeroCard(
    album: AlbumGroup,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .clip(RoundedCornerShape(34.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(34.dp),
        color = Color.White.copy(alpha = 0.68f),
        shadowElevation = 10.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.bucketName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.08f),
                            0.58f to Color.Black.copy(alpha = 0.18f),
                            1f to Color.Black.copy(alpha = 0.44f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = album.bucketName.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${album.mediaCount} 项 · ${formatBytes(album.totalSizeBytes)}",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AlbumFolderCarousel(
    albums: List<AlbumGroup>,
    onOpenAlbum: (AlbumGroup) -> Unit,
) {
    if (albums.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.72f),
        ) {
            Text(
                text = "还没有可展示的相册",
                color = Color(0xFF6D6964),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(18.dp),
            )
        }
        return
    }

    var centerIndex by rememberSaveable(albums.size) { mutableIntStateOf(0) }
    LaunchedEffect(albums.size) {
        centerIndex = centerIndex.coerceIn(0, albums.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
            .pointerInput(albums.size, centerIndex) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDragCancel = { dragY = 0f },
                    onDragEnd = {
                        when {
                            dragY < -46f -> centerIndex = (centerIndex + 1).coerceAtMost(albums.lastIndex)
                            dragY > 46f -> centerIndex = (centerIndex - 1).coerceAtLeast(0)
                        }
                        dragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        dragY += dragAmount.y
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        (-2..2).forEach { offset ->
            val album = albums.getOrNull(centerIndex + offset)
            if (album != null) {
                AlbumFolderCard(
                    album = album,
                    offset = offset,
                    onClick = {
                        if (offset == 0) {
                            onOpenAlbum(album)
                        } else {
                            centerIndex = centerIndex + offset
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.AlbumFolderCard(
    album: AlbumGroup,
    offset: Int,
    onClick: () -> Unit,
) {
    val absOffset = abs(offset)
    val targetWidth = when (absOffset) {
        0 -> 0.90f
        1 -> 0.74f
        else -> 0.56f
    }
    val targetHeight = when (absOffset) {
        0 -> 330f
        1 -> 70f
        else -> 50f
    }
    val targetY = when (offset) {
        -2 -> -230f
        -1 -> -160f
        0 -> 18f
        1 -> 210f
        else -> 282f
    }
    val targetAlpha = when (absOffset) {
        0 -> 1f
        1 -> 0.86f
        else -> 0.68f
    }
    val widthFraction by animateFloatAsState(targetValue = targetWidth, label = "album-folder-width")
    val heightDp by animateFloatAsState(targetValue = targetHeight, label = "album-folder-height")
    val yDp by animateFloatAsState(targetValue = targetY, label = "album-folder-y")
    val alpha by animateFloatAsState(targetValue = targetAlpha, label = "album-folder-alpha")
    val isCenter = offset == 0

    Surface(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = yDp.dp)
            .fillMaxWidth(widthFraction)
            .height(heightDp.dp)
            .zIndex((10 - absOffset).toFloat())
            .clip(if (isCenter) RoundedCornerShape(34.dp) else RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .graphicsLayer { this.alpha = alpha },
        shape = if (isCenter) RoundedCornerShape(34.dp) else RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = if (isCenter) 0.72f else 0.46f),
        shadowElevation = if (isCenter) 14.dp else 6.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.bucketName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = if (isCenter) 0.08f else 0.18f),
                            1f to Color.Black.copy(alpha = if (isCenter) 0.48f else 0.36f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCenter) 8.dp else 0.dp),
            ) {
                Text(
                    text = album.bucketName.uppercase(),
                    color = Color.White,
                    style = if (isCenter) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = if (isCenter) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isCenter) {
                    Text(
                        text = "${album.mediaCount} 项 · ${formatBytes(album.totalSizeBytes)}",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumFolderListCard(
    album: AlbumGroup,
    focusDistance: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val boundedDistance = focusDistance.coerceIn(-2, 2)
    val absDistance = abs(boundedDistance)
    val isCenter = absDistance == 0
    val motion = tween<Float>(
        durationMillis = 430,
        easing = AlbumFolderEasing,
    )
    val widthTarget = when (absDistance) {
        0 -> 0.90f
        1 -> 0.76f
        else -> 0.58f
    }
    val heightTarget = when (absDistance) {
        0 -> 318f
        1 -> 70f
        else -> 48f
    }
    val scaleTarget = when (absDistance) {
        0 -> 1f
        1 -> 0.96f
        else -> 0.90f
    }
    val alphaTarget = when (absDistance) {
        0 -> 1f
        1 -> 0.88f
        else -> 0.68f
    }

    val widthFraction by animateFloatAsState(widthTarget, animationSpec = motion, label = "album-list-width")
    val heightDp by animateFloatAsState(heightTarget, animationSpec = motion, label = "album-list-height")
    val scale by animateFloatAsState(scaleTarget, animationSpec = motion, label = "album-list-scale")
    val alpha by animateFloatAsState(alphaTarget, animationSpec = motion, label = "album-list-alpha")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(if (isCenter) RoundedCornerShape(34.dp) else RoundedCornerShape(999.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            shape = if (isCenter) RoundedCornerShape(34.dp) else RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = if (isCenter) 0.72f else 0.46f),
            shadowElevation = if (isCenter) 14.dp else 5.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = album.bucketName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = if (isCenter) 0.08f else 0.16f),
                                1f to Color.Black.copy(alpha = if (isCenter) 0.48f else 0.34f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isCenter) 8.dp else 0.dp),
                ) {
                    Text(
                        text = album.bucketName.uppercase(),
                        color = Color.White,
                        style = if (isCenter) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = if (isCenter) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isCenter) {
                        Text(
                            text = "${album.mediaCount} 项 · ${formatBytes(album.totalSizeBytes)}",
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarPhotoRow(
    cluster: SimilarPhotoCluster,
    onOpenReview: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenReview(0) }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "相似照片",
                color = Color(0xFF171717),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${cluster.items.size} 张",
                color = Color(0xFF6D6964),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = cluster.items,
                key = { it.stableId },
            ) { item ->
                val itemIndex = cluster.items.indexOfFirst { it.stableId == item.stableId }.coerceAtLeast(0)
                SimilarThumb(
                    item = item,
                    recommended = item.stableId == cluster.recommended.stableId,
                    onClick = { onOpenReview(itemIndex) },
                )
            }
        }
    }
}

@Composable
private fun SimilarThumb(
    item: PhotoItem,
    recommended: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 124.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (recommended) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
                shape = CircleShape,
                color = Color(0xFFFFD84D),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "最佳照片",
                    tint = Color(0xFF5B4300),
                    modifier = Modifier
                        .padding(5.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptySimilarState(scanning: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.72f),
    ) {
        Text(
            text = if (scanning) "正在后台识别相似照片..." else "暂时没有筛选出相似照片",
            color = Color(0xFF6D6964),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(18.dp),
        )
    }
}

@Composable
private fun FloatingRecycleSummary(
    count: Int,
    bytes: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.82f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF5F5A54), modifier = Modifier.size(15.dp))
            Text(
                text = "$count (${formatBytes(bytes)})",
                color = Color(0xFF171717),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MonthDetailScreen(
    group: DateGroup,
    dateGroups: List<DateGroup>,
    monthMediaByTitle: Map<String, List<PhotoItem>>,
    selectedIds: Set<String>,
    onBack: () -> Unit,
    onOpenDateGroup: (DateGroup) -> Unit,
    onOpenReview: (DateGroup, Int) -> Unit,
    onToggleSelection: (PhotoItem) -> Unit,
    onBeginSelection: (PhotoItem) -> Unit,
    onClearSelection: () -> Unit,
    onCompressSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    var displayMode by rememberSaveable(group.title) { mutableStateOf(MonthDisplayMode.Calendar) }
    var modeDragX by rememberSaveable(group.title) { mutableFloatStateOf(0f) }
    var edgeBackDragX by rememberSaveable(group.title) { mutableFloatStateOf(0f) }
    var isEdgeBackDrag by rememberSaveable(group.title) { mutableStateOf(false) }
    val monthSections = remember(dateGroups, monthMediaByTitle) {
        dateGroups.filter { monthMediaByTitle[it.title].orEmpty().isNotEmpty() }
    }
    val selectedItems = monthMediaByTitle.values
        .flatten()
        .distinctBy { it.stableId }
        .filter { it.stableId in selectedIds }
    val selectionMode = selectedItems.isNotEmpty()
    val currentMonthIndex = monthSections.indexOfFirst { it.title == group.title }.coerceAtLeast(0)
    val calendarListState = rememberLazyListState(initialFirstVisibleItemIndex = currentMonthIndex)
    val stackListState = rememberLazyListState(initialFirstVisibleItemIndex = currentMonthIndex)
    val availableMonths = remember(dateGroups) {
        dateGroups.sortedWith(compareByDescending<DateGroup> { it.year }.thenByDescending { it.month })
    }
    val pageDragOffset by animateFloatAsState(
        targetValue = modeDragX * 0.22f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "month-page-drag-offset",
    )

    LaunchedEffect(group.title, currentMonthIndex) {
        if (monthSections.isNotEmpty()) {
            launch { calendarListState.animateScrollToItem(currentMonthIndex) }
            launch { stackListState.animateScrollToItem(currentMonthIndex) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .pointerInput(displayMode, selectionMode) {
                val edgeWidth = 32.dp.toPx()
                val backThreshold = 78.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isEdgeBackDrag = offset.x <= edgeWidth || offset.x >= size.width - edgeWidth
                        edgeBackDragX = 0f
                        modeDragX = 0f
                    },
                    onDragCancel = {
                        modeDragX = 0f
                        edgeBackDragX = 0f
                        isEdgeBackDrag = false
                    },
                    onDragEnd = {
                        if (isEdgeBackDrag) {
                            if (abs(edgeBackDragX) >= backThreshold) {
                                if (selectionMode) {
                                    onClearSelection()
                                } else {
                                    onBack()
                                }
                            }
                        } else {
                            when {
                                modeDragX < -80f -> displayMode = MonthDisplayMode.Calendar
                                modeDragX > 80f -> displayMode = MonthDisplayMode.Stack
                            }
                        }
                        modeDragX = 0f
                        edgeBackDragX = 0f
                        isEdgeBackDrag = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (isEdgeBackDrag) {
                            edgeBackDragX = (edgeBackDragX + dragAmount).coerceIn(-180f, 180f)
                            change.consume()
                            if (abs(edgeBackDragX) >= backThreshold) {
                                isEdgeBackDrag = false
                                edgeBackDragX = 0f
                                modeDragX = 0f
                                if (selectionMode) {
                                    onClearSelection()
                                } else {
                                    onBack()
                                }
                            }
                        } else {
                            modeDragX = (modeDragX + dragAmount).coerceIn(-180f, 180f)
                        }
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = displayMode,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = pageDragOffset },
            transitionSpec = {
                val iosPageSpring = spring<IntOffset>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
                if (targetState == MonthDisplayMode.Stack) {
                    slideInHorizontally(animationSpec = iosPageSpring) { -it } togetherWith
                        slideOutHorizontally(animationSpec = iosPageSpring) { it }
                } else {
                    slideInHorizontally(animationSpec = iosPageSpring) { it } togetherWith
                        slideOutHorizontally(animationSpec = iosPageSpring) { -it }
                }
            },
            label = "month-display-mode",
        ) {
            val targetMode = it
            LazyColumn(
                state = if (targetMode == MonthDisplayMode.Calendar) calendarListState else stackListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (targetMode == MonthDisplayMode.Stack) 120.dp else 68.dp,
                    bottom = 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (targetMode == MonthDisplayMode.Calendar) {
                    items(
                        items = monthSections,
                        key = { "calendar-${it.title}" },
                    ) { monthGroup ->
                        val monthMedia = monthMediaByTitle[monthGroup.title].orEmpty()
                        MonthCalendarSection(
                            group = monthGroup,
                            media = monthMedia,
                            selectedIds = selectedIds,
                            selectionMode = selectionMode,
                            onOpenReview = { index -> onOpenReview(monthGroup, index) },
                            onToggleSelection = onToggleSelection,
                            onBeginSelection = onBeginSelection,
                        )
                    }
                } else {
                    items(
                        items = monthSections,
                        key = { "stack-${it.title}" },
                    ) { monthGroup ->
                        val monthMedia = monthMediaByTitle[monthGroup.title].orEmpty()
                        PhotoStackSection(
                            title = "${monthGroup.month}月",
                            media = monthMedia,
                            selectedIds = selectedIds,
                            selectionMode = selectionMode,
                            onOpenReview = { index -> onOpenReview(monthGroup, index) },
                            onToggleSelection = onToggleSelection,
                            onBeginSelection = onBeginSelection,
                        )
                    }
                }
            }
        }

        MonthDetailTopBar(
            currentMonth = group,
            selectionCount = selectedItems.size,
            selectionBytes = selectedItems.sumOf { it.sizeBytes },
            selectionMode = selectionMode,
            availableMonths = availableMonths,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 2.dp, end = 18.dp, top = 6.dp)
                .zIndex(20f),
            onBack = if (selectionMode) onClearSelection else onBack,
            onMonthSelected = onOpenDateGroup,
            onCompressSelected = onCompressSelected,
            onDeleteSelected = onDeleteSelected,
        )

        MonthDisplaySwitcher(
            displayMode = displayMode,
            onDisplayModeChange = { displayMode = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun MonthDetailTopBar(
    currentMonth: DateGroup,
    selectionCount: Int,
    selectionBytes: Long,
    selectionMode: Boolean,
    availableMonths: List<DateGroup>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onMonthSelected: (DateGroup) -> Unit,
    onCompressSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 120, easing = LiquidSwitchEasing),
        label = "month-select-arrow",
    )
    val monthOptions = remember(availableMonths) {
        availableMonths.sortedWith(compareByDescending<DateGroup> { it.year }.thenByDescending { it.month })
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF171717),
                    modifier = Modifier.size(25.dp),
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(104.dp)
                    .height(44.dp)
                    .clickable(enabled = monthOptions.size > 1) {
                        expanded = !expanded
                    },
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.94f),
                shadowElevation = if (expanded) 8.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "${currentMonth.month}月",
                        color = Color(0xFF171717),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF171717),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = arrowRotation },
                    )
                }
            }
            if (selectionMode) {
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionActionButton(
                        icon = Icons.Default.Settings,
                        label = "压缩",
                        color = Color(0xFF10A783),
                        onClick = onCompressSelected,
                    )
                    SelectionActionButton(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        color = Color(0xFFFF5B6A),
                        onClick = onDeleteSelected,
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded && monthOptions.size > 1,
                enter = fadeIn(animationSpec = tween(durationMillis = 100)) +
                    scaleIn(
                        initialScale = 0.95f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = tween(durationMillis = 100, easing = LiquidSwitchEasing),
                    ) +
                    slideInVertically(animationSpec = tween(durationMillis = 100, easing = LiquidSwitchEasing)) { -it / 8 },
                exit = fadeOut(animationSpec = tween(durationMillis = 100)) +
                    scaleOut(
                        targetScale = 0.95f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = tween(durationMillis = 100, easing = LiquidSwitchEasing),
                    ) +
                    slideOutVertically(animationSpec = tween(durationMillis = 100, easing = LiquidSwitchEasing)) { -it / 8 },
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(158.dp)
                        .heightIn(max = 250.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, Color(0x1A171717)),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 250.dp)
                            .padding(vertical = 4.dp),
                    ) {
                        items(
                            items = monthOptions,
                            key = { it.title },
                        ) { optionMonth ->
                            val selected = optionMonth.title == currentMonth.title
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .padding(horizontal = 5.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (selected) FigmaSelected.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable {
                                        expanded = false
                                        onMonthSelected(optionMonth)
                                    },
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${optionMonth.month}月 ${optionMonth.year}",
                                        color = if (selected) Color(0xFF171717) else Color(0xFF6F6A63),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = FigmaSelected,
                                            modifier = Modifier.size(17.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (selectionMode) {
            Text(
                text = "已选择 $selectionCount 项 · ${formatBytes(selectionBytes)}",
                color = Color(0xFF6D6964),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 54.dp, top = 4.dp),
            )
        }
    }
}
@Composable
private fun MonthCalendarSection(
    group: DateGroup,
    media: List<PhotoItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onOpenReview: (Int) -> Unit,
    onToggleSelection: (PhotoItem) -> Unit,
    onBeginSelection: (PhotoItem) -> Unit,
) {
    val cells = remember(group.title, media) { buildMonthCalendarCells(group, media) }
    val horizontalDayGap = 3.dp
    val verticalDayGap = 8.dp

    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(
            text = "${group.month}月 ${group.year}",
            color = Color(0xFF171717),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cellSize = (maxWidth - horizontalDayGap * 6f) / 7f
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(horizontalDayGap),
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "七").forEach { label ->
                        Box(
                            modifier = Modifier
                                .width(cellSize)
                                .height(22.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = Color(0xFF8C8880),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(verticalDayGap)) {
                    cells.chunked(7).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(horizontalDayGap),
                        ) {
                            row.forEach { cell ->
                                if (cell == null) {
                                    Spacer(modifier = Modifier.size(cellSize))
                                } else {
                                    MonthDayBubble(
                                        cell = cell,
                                        media = media,
                                        selectedIds = selectedIds,
                                        selectionMode = selectionMode,
                                        size = cellSize,
                                        onOpenReview = onOpenReview,
                                        onToggleSelection = onToggleSelection,
                                        onBeginSelection = onBeginSelection,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthDayBubble(
    cell: MonthDayCell,
    media: List<PhotoItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    size: Dp,
    onOpenReview: (Int) -> Unit,
    onToggleSelection: (PhotoItem) -> Unit,
    onBeginSelection: (PhotoItem) -> Unit,
) {
    val cover = cell.items.firstOrNull()
    val selected = cover?.stableId?.let { it in selectedIds } == true
    val dayShape = RoundedCornerShape(6.dp)
    val openModifier = if (cover != null) {
        Modifier.combinedClickable(
            onClick = {
                if (selectionMode) {
                    onToggleSelection(cover)
                } else {
                    val index = media.indexOfFirst { it.stableId == cover.stableId }
                    onOpenReview(index.coerceAtLeast(0))
                }
            },
            onLongClick = {
                onBeginSelection(cover)
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(dayShape)
            .background(
                when {
                    selected -> FigmaSelected.copy(alpha = 0.92f)
                    cover == null -> Color(0xFFDCD9D3)
                    else -> Color(0xFFBFC6C5)
                },
            )
            .then(openModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            AsyncImage(
                model = cover.uri,
                contentDescription = cover.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f)),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.calendar_empty_day_slashes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeonGreen.copy(alpha = 0.30f)),
            )
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(15.dp),
            )
        }
        Text(
            text = cell.day.toString(),
            color = Color.White.copy(alpha = if (cover == null) 0.90f else 1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (cover == null) FontWeight.SemiBold else FontWeight.Black,
        )
    }
}
@Composable
private fun MonthDisplaySwitcher(
    displayMode: MonthDisplayMode,
    onDisplayModeChange: (MonthDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidSwitchShell(
        selectedIndex = if (displayMode == MonthDisplayMode.Stack) 0 else 1,
        itemCount = 2,
        modifier = modifier.width(126.dp),
        height = 44.dp,
        containerColor = Color.White.copy(alpha = 0.36f),
        sliderColor = Color.White.copy(alpha = 0.62f),
        borderColor = Color.White.copy(alpha = 0.68f),
    ) {
        LiquidIconOption(
            icon = Icons.Default.PhotoLibrary,
            selected = displayMode == MonthDisplayMode.Stack,
            onClick = { onDisplayModeChange(MonthDisplayMode.Stack) },
        )
        LiquidIconOption(
            icon = Icons.Default.Photo,
            selected = displayMode == MonthDisplayMode.Calendar,
            onClick = { onDisplayModeChange(MonthDisplayMode.Calendar) },
        )
    }
}

@Composable
private fun DisplayModeButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White.copy(alpha = 0.62f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF171717) else Color(0xFF77736E),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PhotoStackSection(
    title: String,
    media: List<PhotoItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onOpenReview: (Int) -> Unit,
    onToggleSelection: (PhotoItem) -> Unit,
    onBeginSelection: (PhotoItem) -> Unit,
) {
    val cards = media.take(10)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(760.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.74f),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
        )
        if (cards.isEmpty()) {
            Text(
                text = "这个月还没有照片",
                color = Color(0xFF6D6964),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        cards.forEachIndexed { index, item ->
            val placement = StackPlacements[index % StackPlacements.size]
            StackPhotoCard(
                item = item,
                selected = item.stableId in selectedIds,
                selectionMode = selectionMode,
                modifier = Modifier
                    .align(placement.alignment)
                    .offset(x = placement.x.dp, y = placement.y.dp)
                    .size(width = placement.width.dp, height = placement.height.dp)
                    .graphicsLayer(rotationZ = placement.rotation)
                    .zIndex(index.toFloat()),
                onClick = {
                    if (selectionMode) {
                        onToggleSelection(item)
                    } else {
                        val mediaIndex = media.indexOfFirst { it.stableId == item.stableId }
                        onOpenReview(mediaIndex.coerceAtLeast(0))
                    }
                },
                onLongClick = {
                    if (selectionMode) {
                        onToggleSelection(item)
                    } else {
                        onBeginSelection(item)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StackPhotoCard(
    item: PhotoItem,
    selected: Boolean,
    selectionMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.70f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (selected) NeonGreen.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.16f),
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp),
                shape = CircleShape,
                color = if (selected) NeonGreen else Color.Black.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaGroupScreen(
    title: String,
    subtitle: String,
    media: List<PhotoItem>,
    selectedIds: Set<String>,
    onBack: () -> Unit,
    onOpenReview: (Int) -> Unit,
    onToggleSelection: (PhotoItem) -> Unit,
    onBeginSelection: (PhotoItem) -> Unit,
    onClearSelection: () -> Unit,
    onCompressSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    contentPadding: PaddingValues,
) {
    val selectionMode = selectedIds.isNotEmpty()
    val selectedItems = media.filter { it.stableId in selectedIds }
    val headerTitle = if (selectionMode) "${selectedItems.size} 项已选" else title
    val headerSubtitle = if (selectionMode) {
        formatBytes(selectedItems.sumOf { it.sizeBytes })
    } else {
        subtitle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .edgeHorizontalBackGesture(enabled = true) {
                if (selectionMode) {
                    onClearSelection()
                } else {
                    onBack()
                }
            }
            .padding(contentPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = if (selectionMode) onClearSelection else onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF171717))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerTitle,
                    color = Color(0xFF171717),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(headerSubtitle, color = Color(0xFF6D6964), style = MaterialTheme.typography.bodySmall)
            }
            if (selectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionActionButton(
                        icon = Icons.Default.Settings,
                        label = "压缩",
                        color = NeonGreen,
                        onClick = onCompressSelected,
                    )
                    SelectionActionButton(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        color = Color(0xFFFF6B6B),
                        onClick = onDeleteSelected,
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = media,
                key = { it.stableId },
            ) { item ->
                val index = media.indexOfFirst { it.stableId == item.stableId }
                MediaTile(
                    item = item,
                    selected = item.stableId in selectedIds,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            onToggleSelection(item)
                        } else {
                            onOpenReview(index.coerceAtLeast(0))
                        }
                    },
                    onLongClick = {
                        if (selectionMode) {
                            onToggleSelection(item)
                        } else {
                            onBeginSelection(item)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.58f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(
    item: PhotoItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE7E1D8))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.62f),
            ) {
                Text(
                    text = "Video",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (selected) {
                            NeonGreen.copy(alpha = 0.26f)
                        } else {
                            Color.Black.copy(alpha = 0.18f)
                        },
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp),
                shape = CircleShape,
                color = if (selected) NeonGreen else Color.Black.copy(alpha = 0.54f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) NeonGreen else Color.White.copy(alpha = 0.72f),
                ),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoReviewScreen(
    title: String,
    media: List<PhotoItem>,
    startIndex: Int,
    onClose: () -> Unit,
    onKeep: (PhotoItem) -> Unit,
    onMoveToRecycleBin: (PhotoItem) -> Unit,
    gestureMode: ReviewGestureMode,
    tagFolders: List<String>,
    onArchiveToTag: (PhotoItem, String) -> Unit,
) {
    var index by rememberSaveable(title, media.size) { mutableIntStateOf(startIndex) }
    var dragX by rememberSaveable(media.getOrNull(index)?.stableId) { mutableFloatStateOf(0f) }
    var dragY by rememberSaveable(media.getOrNull(index)?.stableId) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isCommitting by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showTagFolders by rememberSaveable(title) { mutableStateOf(true) }
    var showGestureHint by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val current = media.getOrNull(index) ?: run {
        onClose()
        return
    }
    val reviewMotion = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMedium,
    )
    val cardDragOffset by animateFloatAsState(
        targetValue = dragX,
        animationSpec = if (isDragging) snap() else reviewMotion,
        label = "review-card-drag-offset",
    )
    val cardVerticalOffset by animateFloatAsState(
        targetValue = dragY,
        animationSpec = if (isDragging) snap() else reviewMotion,
        label = "review-card-vertical-offset",
    )
    val cardTilt by animateFloatAsState(
        targetValue = 0f,
        animationSpec = if (isDragging) snap() else reviewMotion,
        label = "review-card-tilt",
    )
    val actionProgress by animateFloatAsState(
        targetValue = (max(abs(dragX), abs(dragY)) / 220f).coerceIn(0f, 1f),
        animationSpec = if (isDragging) snap() else reviewMotion,
        label = "review-action-progress",
    )
    val cardScale = 1f - actionProgress * 0.055f

    LaunchedEffect(showGestureHint) {
        if (showGestureHint) {
            delay(1800)
            showGestureHint = false
        }
    }

    fun advance() {
        dragX = 0f
        dragY = 0f
        if (index < media.lastIndex) {
            index += 1
        } else {
            onClose()
        }
    }

    fun commitSwipe(targetX: Float, targetY: Float = 0f, action: () -> Unit) {
        if (isCommitting) return
        isDragging = false
        isCommitting = true
        dragX = targetX
        dragY = targetY
        scope.launch {
            delay(110)
            action()
            advance()
            isCommitting = false
        }
    }

    fun commitClose() {
        if (isCommitting) return
        isDragging = false
        isCommitting = true
        dragY = 620f
        scope.launch {
            delay(110)
            onClose()
            isCommitting = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .edgeHorizontalBackGesture(enabled = true, onBack = onClose),
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_paper_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 44.dp)
                .zIndex(3f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = Icons.Default.Close,
                onClick = onClose,
                buttonSize = 44.dp,
                iconSize = 23.dp,
                backgroundColor = Color.White.copy(alpha = 0.68f),
                iconTint = Color(0xFF282828),
            )
            Box {
                RoundIconButton(
                    icon = Icons.Default.MoreHoriz,
                    onClick = { menuExpanded = true },
                    buttonSize = 44.dp,
                    iconSize = 23.dp,
                    backgroundColor = Color.White.copy(alpha = 0.68f),
                    iconTint = Color(0xFF282828),
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(96.dp),
                    shape = RoundedCornerShape(8.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "添加相册",
                                color = Color(0xFF282828),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            showTagFolders = true
                            menuExpanded = false
                        },
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "切换手势",
                                color = Color(0xFF282828),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            showGestureHint = true
                            menuExpanded = false
                        },
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    )
                }
            }
        }

        if (showGestureHint) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 104.dp)
                    .zIndex(4f),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.82f),
            ) {
                Text(
                    text = "在「我的」页的手势习惯里调整",
                    color = Color(0xFF171717),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        if (abs(dragX) >= abs(dragY) && dragX < -16f) {
            EdgeSwipeGlow(
                text = "保留",
                color = NeonGreen,
                fromStart = true,
                progress = (-dragX / 220f).coerceIn(0f, 1f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(2f),
            )
        }
        if (gestureMode == ReviewGestureMode.Classic && abs(dragX) >= abs(dragY) && dragX > 16f) {
            EdgeSwipeGlow(
                text = "删除",
                color = Color(0xFFFF6B6B),
                fromStart = false,
                progress = (dragX / 220f).coerceIn(0f, 1f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(2f),
            )
        }
        if (gestureMode == ReviewGestureMode.UpDeleteLeftKeep && abs(dragY) > abs(dragX) && dragY < -16f) {
            VerticalSwipeGlow(
                text = "删除",
                color = Color(0xFFFF6B6B),
                fromTop = true,
                progress = (-dragY / 220f).coerceIn(0f, 1f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f),
            )
        }
        if (abs(dragY) > abs(dragX) && dragY > 16f) {
            VerticalSwipeGlow(
                text = "返回照片库",
                color = Color.White,
                fromTop = false,
                progress = (dragY / 220f).coerceIn(0f, 1f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f),
            )
        }

        media.getOrNull(index + 2)?.let { nextItem ->
            ReviewPhotoLayer(
                item = nextItem,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 58.dp, end = 58.dp, top = 146.dp)
                    .graphicsLayer {
                        scaleX = 0.92f
                        scaleY = 0.92f
                        alpha = 0.48f
                    }
                    .zIndex(0.15f),
            )
        }
        media.getOrNull(index + 1)?.let { nextItem ->
            ReviewPhotoLayer(
                item = nextItem,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 44.dp, end = 44.dp, top = 156.dp)
                    .graphicsLayer {
                        scaleX = 0.96f
                        scaleY = 0.96f
                        alpha = 0.68f
                    }
                    .zIndex(0.3f),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 172.dp)
                .zIndex(1f)
                .offset { IntOffset(cardDragOffset.roundToInt(), cardVerticalOffset.roundToInt()) }
                .graphicsLayer {
                    rotationZ = cardTilt
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .pointerInput(current.stableId, gestureMode, isCommitting) {
                    detectDragGestures(
                        onDragStart = {
                            if (!isCommitting) {
                                isDragging = true
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragEnd = {
                            isDragging = false
                            val horizontal = abs(dragX) >= abs(dragY)
                            when {
                                dragY > 150f && !horizontal -> commitClose()
                                gestureMode == ReviewGestureMode.UpDeleteLeftKeep &&
                                    dragY < -150f && !horizontal -> commitSwipe(0f, -720f) { onMoveToRecycleBin(current) }
                                dragX < -150f && horizontal -> commitSwipe(-680f) { onKeep(current) }
                                gestureMode == ReviewGestureMode.Classic &&
                                    dragX > 150f && horizontal -> commitSwipe(680f) { onMoveToRecycleBin(current) }
                                else -> {
                                    dragX = 0f
                                    dragY = 0f
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!isCommitting) {
                                dragX = (dragX + dragAmount.x).coerceIn(-420f, 420f)
                                dragY = (dragY + dragAmount.y).coerceIn(-520f, 520f)
                                change.consume()
                            }
                        },
                    )
                },
        ) {
            AsyncImage(
                model = current.uri,
                contentDescription = current.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF252A26)),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(112.dp)
                .background(Color.White.copy(alpha = 0.88f))
                .zIndex(1.4f),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(3f)
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${formatReviewDate(current)}  ${formatReviewTime(current)}  ${formatBytes(current.sizeBytes)}",
                color = Color(0xFF4C4742),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (showTagFolders) {
                TagFolderStrip(
                    folders = tagFolders,
                    onFolderClick = { folder ->
                        commitSwipe(-680f) {
                            onArchiveToTag(current, folder)
                            onKeep(current)
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ReviewThumbnailCarousel(
                media = media,
                currentIndex = index,
                dragOffset = cardDragOffset,
                onSelect = { selectedIndex ->
                    index = selectedIndex
                    dragX = 0f
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(
                    icon = Icons.Default.Star,
                    onClick = {
                        commitSwipe(-680f) { onKeep(current) }
                    },
                    buttonSize = 44.dp,
                    iconSize = 22.dp,
                    backgroundColor = Color(0xFFFFB24A),
                    iconTint = Color.White,
                )
                RoundIconButton(
                    icon = Icons.Default.Delete,
                    onClick = {
                        if (gestureMode == ReviewGestureMode.UpDeleteLeftKeep) {
                            commitSwipe(0f, -720f) { onMoveToRecycleBin(current) }
                        } else {
                            commitSwipe(680f) { onMoveToRecycleBin(current) }
                        }
                    },
                    buttonSize = 44.dp,
                    iconSize = 22.dp,
                    backgroundColor = Color(0xFF2A2A2A),
                    iconTint = Color.White,
                )
            }
        }
    }
}

private fun formatReviewDate(item: PhotoItem): String {
    if (item.dateTakenMillis <= 0L) {
        return item.dateLabel.ifBlank { "未知日期" }.replace("-", "/")
    }
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = item.dateTakenMillis
    return "%04d/%02d/%02d".format(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
}

private fun formatReviewTime(item: PhotoItem): String {
    if (item.dateTakenMillis <= 0L) return "--:--"
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = item.dateTakenMillis
    return "%d:%02d".format(
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
    )
}

@Composable
private fun ReviewPhotoLayer(
    item: PhotoItem,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF252A26)),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f)),
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    buttonSize: Dp = 54.dp,
    iconSize: Dp = 28.dp,
    backgroundColor: Color = Color(0x664D514D),
    iconTint: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun EdgeSwipeGlow(
    text: String,
    color: Color,
    fromStart: Boolean,
    progress: Float,
    modifier: Modifier,
) {
    val edgeBrush = if (fromStart) {
        Brush.horizontalGradient(
            0f to color.copy(alpha = 0.34f * progress),
            0.54f to color.copy(alpha = 0.14f * progress),
            1f to Color.Transparent,
        )
    } else {
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.46f to color.copy(alpha = 0.14f * progress),
            1f to color.copy(alpha = 0.34f * progress),
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(176.dp)
            .background(edgeBrush),
        contentAlignment = if (fromStart) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.24f * progress))
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun VerticalSwipeGlow(
    text: String,
    color: Color,
    fromTop: Boolean,
    progress: Float,
    modifier: Modifier,
) {
    val edgeBrush = if (fromTop) {
        Brush.verticalGradient(
            0f to color.copy(alpha = 0.28f * progress),
            0.58f to color.copy(alpha = 0.10f * progress),
            1f to Color.Transparent,
        )
    } else {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.42f to color.copy(alpha = 0.10f * progress),
            1f to color.copy(alpha = 0.24f * progress),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .background(edgeBrush),
        contentAlignment = if (fromTop) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(vertical = 22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.22f * progress))
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun TagFolderStrip(
    folders: List<String>,
    onFolderClick: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it }) { folder ->
            Surface(
                modifier = Modifier.clickable { onFolderClick(folder) },
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.76f),
                border = BorderStroke(1.dp, Color(0xFFFFC39E)),
            ) {
                Text(
                    text = folder,
                    color = Color(0xFF282828),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewThumbnailCarousel(
    media: List<PhotoItem>,
    currentIndex: Int,
    dragOffset: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val carouselShift = (dragOffset * 0.12f).coerceIn(-30f, 30f)
    val carouselTilt = (dragOffset / 420f).coerceIn(-1f, 1f) * 1.6f
    val visibleItems = remember(media, currentIndex) {
        val visibleCount = 15
        if (media.size <= visibleCount) {
            media.mapIndexed { index, item -> index to item }
        } else {
            val start = (currentIndex - visibleCount / 2).coerceIn(0, media.size - visibleCount)
            (start until start + visibleCount).map { thumbIndex -> thumbIndex to media[thumbIndex] }
        }
    }

    Row(
        modifier = modifier
            .height(30.dp)
            .graphicsLayer {
                translationX = carouselShift
                rotationZ = carouselTilt * 0.2f
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        visibleItems.forEach { (thumbIndex, item) ->
            ReviewCarouselThumb(
                item = item,
                selected = thumbIndex == currentIndex,
                distance = thumbIndex - currentIndex,
                onClick = { onSelect(thumbIndex) },
                modifier = Modifier.weight(1f),
            )
        }
        repeat(15 - visibleItems.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReviewCarouselThumb(
    item: PhotoItem,
    selected: Boolean,
    distance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val boundedDistance = distance.coerceIn(-2, 2)
    val scale by animateFloatAsState(
        targetValue = when (abs(boundedDistance)) {
            0 -> 1.08f
            1 -> 1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "review-thumb-scale",
    )
    val rotation by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "review-thumb-rotation",
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.42f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "review-thumb-alpha",
    )

    Surface(
        modifier = modifier
            .height(26.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                translationY = 0f
                this.alpha = alpha
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFFD4D4D4),
        border = if (selected) BorderStroke(2.dp, Color.White) else null,
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun List<PhotoItem>.applyFilter(filter: MediaFilter): List<PhotoItem> {
    return when (filter) {
        MediaFilter.All -> this
        MediaFilter.Photo -> filter { it.mediaType == MediaType.PHOTO }
        MediaFilter.Video -> filter { it.mediaType == MediaType.VIDEO }
        MediaFilter.Screenshot -> filter { it.isScreenshotLike() }
        MediaFilter.Similar -> filter { it.isScreenshotLike() || it.name.startsWith("IMG_", ignoreCase = true) }
        MediaFilter.Other -> filter { it.mediaType == MediaType.PHOTO && it.bucketName.isBlank() }
    }
}

private fun TidyMode.next(): TidyMode {
    return when (this) {
        TidyMode.Date -> TidyMode.Album
        TidyMode.Album -> TidyMode.Similar
        TidyMode.Similar -> TidyMode.Date
    }
}

private fun TidyMode.previous(): TidyMode {
    return when (this) {
        TidyMode.Date -> TidyMode.Similar
        TidyMode.Album -> TidyMode.Date
        TidyMode.Similar -> TidyMode.Album
    }
}

private fun PhotoItem.isScreenshotLike(): Boolean {
    return bucketName.contains("screenshot", ignoreCase = true) ||
        bucketName.contains("截屏") ||
        bucketName.contains("截图")
}

private data class SimilarPhotoCluster(
    val groupId: String,
    val items: List<PhotoItem>,
    val recommended: PhotoItem,
)

private fun PhotoLibraryUiState.toSimilarPhotoClusters(filter: MediaFilter): List<SimilarPhotoCluster> {
    val visibleItemsById = visibleMedia
        .applyFilter(filter)
        .associateBy { it.stableId }

    return similarGroups.mapNotNull { group ->
        group.toUiCluster(visibleItemsById)
    }
}

private fun SimilarGroup.toUiCluster(visibleItemsById: Map<String, PhotoItem>): SimilarPhotoCluster? {
    val items = mediaIds.mapNotNull { visibleItemsById[it] }
    if (items.size < 2) return null

    val recommended = recommendedMediaId
        ?.let { visibleItemsById[it] }
        ?: items.maxBy { it.recommendationScore() }

    return SimilarPhotoCluster(
        groupId = groupId,
        items = items,
        recommended = recommended,
    )
}

private fun PhotoItem.recommendationScore(): Long {
    val resolutionScore = width.toLong().coerceAtLeast(0L) * height.toLong().coerceAtLeast(0L)
    val sizeScore = sizeBytes.coerceAtLeast(0L) / 16L
    val screenshotPenalty = if (isScreenshotLike()) 3_000_000_000L else 0L
    return resolutionScore + sizeScore + dateTakenMillis / 1_000L - screenshotPenalty
}

private fun List<PhotoItem>.toDateGroups(organizedIds: Set<String>): List<DateGroup> {
    val calendar = Calendar.getInstance()
    return filter { it.dateTakenMillis > 0L }
        .groupBy { item ->
            calendar.timeInMillis = item.dateTakenMillis
            calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH) + 1
        }
        .map { (yearMonth, items) ->
            val organizedCount = items.count { it.stableId in organizedIds }
            DateGroup(
                year = yearMonth.first,
                month = yearMonth.second,
                mediaCount = items.size,
                organizedCount = organizedCount,
                progress = if (items.isEmpty()) 0f else organizedCount.toFloat() / items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
            )
        }
        .sortedWith(compareByDescending<DateGroup> { it.year }.thenByDescending { it.month })
}

private fun List<PhotoItem>.toAlbumGroups(): List<AlbumGroup> {
    return groupBy { it.bucketId.ifBlank { it.bucketName.ifBlank { "other" } } }
        .map { (bucketId, items) ->
            val cover = items.maxByOrNull { it.dateTakenMillis } ?: items.first()
            AlbumGroup(
                bucketId = bucketId,
                bucketName = cover.bucketName.ifBlank { "鍏朵粬" },
                mediaCount = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                coverUri = cover.uri,
            )
        }
        .sortedWith(compareByDescending<AlbumGroup> { it.mediaCount }.thenBy { it.bucketName })
}

private data class MonthDayCell(
    val day: Int,
    val items: List<PhotoItem>,
)

private data class StackPlacement(
    val alignment: Alignment,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val rotation: Float,
)

private fun buildMonthCalendarCells(
    group: DateGroup,
    media: List<PhotoItem>,
): List<MonthDayCell?> {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(group.year, group.month - 1, 1)
    val firstDayOffset = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val trailingOffset = (7 - ((firstDayOffset + daysInMonth) % 7)) % 7
    val byDay = media.groupBy { item ->
        val itemCalendar = Calendar.getInstance()
        itemCalendar.timeInMillis = item.dateTakenMillis
        itemCalendar.get(Calendar.DAY_OF_MONTH)
    }

    return List(firstDayOffset) { null } +
        (1..daysInMonth).map { day ->
            MonthDayCell(
                day = day,
                items = byDay[day].orEmpty(),
            )
        } +
        List(trailingOffset) { null }
}

private fun DateGroup.monthCardColor(): Color {
    return MonthCardColors[(year * 12 + month) % MonthCardColors.size]
}

private val AppBackground = Color.Transparent
private val NeonGreen = Color(0xFF31F85B)
private val FigmaSelected = Color(0xFFFFA14F)
private val AlbumFolderEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.1f)
private val LiquidSwitchEasing = CubicBezierEasing(0.22f, 0.9f, 0.25f, 1f)
private val MonthCardColors = listOf(
    Color(0xFFF45D8D),
    Color(0xFFFFB627),
    Color(0xFF21C46B),
    Color(0xFF22B8E8),
    Color(0xFFFF6B57),
    Color(0xFF7C63FF),
    Color(0xFFFF8A34),
    Color(0xFF12BFA5),
)

private val StackPlacements = listOf(
    StackPlacement(Alignment.TopStart, 0, 92, 128, 154, -9f),
    StackPlacement(Alignment.TopEnd, -10, 62, 128, 154, 7f),
    StackPlacement(Alignment.TopCenter, 8, 170, 138, 172, -1f),
    StackPlacement(Alignment.CenterStart, -10, -28, 130, 154, -11f),
    StackPlacement(Alignment.CenterEnd, 0, 18, 130, 156, 9f),
    StackPlacement(Alignment.Center, -8, 92, 120, 148, 3f),
    StackPlacement(Alignment.BottomStart, 4, -150, 126, 150, 6f),
    StackPlacement(Alignment.BottomCenter, 60, -70, 132, 160, -8f),
    StackPlacement(Alignment.BottomEnd, -6, -160, 124, 150, 8f),
    StackPlacement(Alignment.BottomCenter, -104, -230, 116, 142, -5f),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 800)
@Composable
private fun TidyScreenPreview() {
    val previewMedia = List(30) { index ->
        PhotoItem(
            id = index.toLong(),
            uri = "https://picsum.photos/seed/tidy-$index/600/800",
            name = "IMG_202606${(index % 9) + 1}.jpg",
            sizeBytes = 1_200_000L + index * 310_000L,
            dateLabel = "2026-06-${(index % 9) + 1}",
            width = 3024,
            height = 4032,
            dateTakenMillis = 1_780_000_000_000L - index * 86_400_000L,
            mediaType = if (index % 6 == 0) MediaType.VIDEO else MediaType.PHOTO,
            bucketId = if (index % 3 == 0) "camera" else "screenshots",
            bucketName = if (index % 3 == 0) "Camera" else "Screenshots",
        )
    }
    PhotoTidyAndroidTheme {
        TidyScreen(
            uiState = PhotoLibraryUiState(
                accessStatus = PhotoAccessStatus.Full,
                media = previewMedia,
            ),
            onKeep = {},
            onMoveToRecycleBin = {},
            onToggleSelection = {},
            onSelectOnly = {},
            onClearSelection = {},
            onCompressSelected = {},
            onMoveSelectedToRecycleBin = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}


