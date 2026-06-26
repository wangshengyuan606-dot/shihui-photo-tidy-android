package com.example.phototidy.ui.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.phototidy.PhotoAccessStatus
import com.example.phototidy.PhotoLibraryUiState
import com.example.phototidy.R
import com.example.phototidy.data.SampleData
import com.example.phototidy.model.AlbumGroup
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PersonCluster
import com.example.phototidy.model.PhotoContentAnalysis
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.PhotoLocation
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private val HomeInk = Color(0xFF282828)
private val HomeSubtle = Color(0xFF7D7370)
private val HomeAccent = Color(0xFFFFA14F)
private val HomeCardBorder = Color.White.copy(alpha = 0.94f)

@Composable
fun HomeScreen(
    uiState: PhotoLibraryUiState,
    onOpenGallery: () -> Unit,
    onStartTidy: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenAlbum: (AlbumGroup) -> Unit = {},
    onKeep: (PhotoItem) -> Unit,
    onMoveToRecycleBin: (PhotoItem) -> Unit,
    onAddTag: (PhotoItem, String) -> Unit,
    memoryBatchSize: Int,
    memoryRefreshToken: Int,
    onRefreshMemoryBox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleMedia = uiState.visibleMedia
    val photos = visibleMedia
        .filter { it.mediaType == MediaType.PHOTO && !it.isScreenshotLike() }
        .ifEmpty { visibleMedia.filter { it.mediaType == MediaType.PHOTO } }
        .ifEmpty { visibleMedia }
    val albumGroups = uiState.albumGroups
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val searchResults = remember(
        searchQuery,
        visibleMedia,
        uiState.photoTags,
        uiState.photoLocations,
        uiState.personClusters,
        uiState.contentAnalysis,
    ) {
        visibleMedia.searchByKeyword(
            query = searchQuery,
            tags = uiState.photoTags,
            locations = uiState.photoLocations,
            personClusters = uiState.personClusters,
            contentAnalysis = uiState.contentAnalysis,
        ).take(12)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.app_paper_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HomeHeader(onOpenMessages = onOpenRecycleBin)
            }

            item {
                HomeSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                )
            }

            if (searchQuery.isNotBlank()) {
                item {
                    SearchResultsPanel(
                        query = searchQuery,
                        results = searchResults,
                        isIndexing = uiState.isScanningLocations || uiState.isScanningContent,
                        onOpenGallery = onOpenGallery,
                    )
                }
            }

            item {
                HomeAlbumChips(
                    albums = albumGroups,
                    fallbackPhotos = photos,
                    onOpenAlbum = onOpenAlbum,
                    modifier = Modifier.requiredWidth(screenWidth - 16.dp),
                )
            }

            item {
                SectionTitle(
                    title = "照片盲盒",
                    trailing = {
                        RoundResourceButton(
                            onClick = onRefreshMemoryBox,
                            iconRes = R.drawable.home_refresh_frame4,
                            contentDescription = "刷新照片盲盒",
                            size = 36.dp,
                            iconSize = 20.dp,
                        )
                    },
                )
            }

            item {
                TimeBlindBox(
                    photos = photos,
                    isLoading = uiState.isLoading,
                    memoryBatchSize = memoryBatchSize,
                    memoryRefreshToken = memoryRefreshToken,
                    onKeep = onKeep,
                    onMoveToRecycleBin = onMoveToRecycleBin,
                    onAddTag = onAddTag,
                )
            }

            item {
                PhotoOverviewPanel(
                    uiState = uiState,
                    onStartTidy = onStartTidy,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenMessages: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "欢迎回来！",
            color = HomeInk,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        RoundResourceButton(
            onClick = onOpenMessages,
            iconRes = R.drawable.home_message_frame4,
            contentDescription = "消息",
            size = 44.dp,
            iconSize = 24.dp,
        )
    }
}

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF282828)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = HomeInk,
                fontSize = 14.sp,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "寻找你的回忆",
                        color = Color(0xFFA2A2A2),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun SearchResultsPanel(
    query: String,
    results: List<PhotoItem>,
    isIndexing: Boolean,
    onOpenGallery: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when {
                    results.isNotEmpty() -> "找到 ${results.size} 张相关照片"
                    isIndexing -> "正在补充智能索引，稍后可以搜风景、宠物、人物和地点"
                    else -> "没有找到和“$query”相关的照片"
                },
                color = HomeSubtle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (results.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.stableId }) { item ->
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.name,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenGallery),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAlbumChips(
    albums: List<AlbumGroup>,
    fallbackPhotos: List<PhotoItem>,
    onOpenAlbum: (AlbumGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(albums.take(3), key = { it.bucketId }) { album ->
            HomeAlbumChip(
                label = album.bucketName,
                photoUri = album.coverUri,
                onClick = { onOpenAlbum(album) },
            )
        }
        if (albums.isEmpty()) {
            items(fallbackPhotos.take(3), key = { it.stableId }) { item ->
                HomeAlbumChip(
                    label = item.bucketName.ifBlank { "相册" },
                    photoUri = item.uri,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun HomeAlbumChip(
    label: String,
    photoUri: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .width(109.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (photoUri.isNotBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE6CA)),
            )
        }
        Text(
            text = label,
            color = HomeInk.copy(alpha = 0.62f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = HomeInk,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        trailing()
    }
}

@Composable
private fun RoundResourceButton(
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun TimeBlindBox(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    memoryBatchSize: Int,
    memoryRefreshToken: Int,
    onKeep: (PhotoItem) -> Unit,
    onMoveToRecycleBin: (PhotoItem) -> Unit,
    onAddTag: (PhotoItem, String) -> Unit,
) {
    val idsKey = remember(photos) { photos.joinToString(separator = "|") { it.stableId } }
    val memoryItems = remember(idsKey, memoryBatchSize, memoryRefreshToken) {
        val limit = if (memoryBatchSize <= 0) photos.size else memoryBatchSize.coerceAtMost(photos.size)
        if (limit <= 0) {
            emptyList()
        } else {
            val seed = idsKey.hashCode() xor (memoryRefreshToken + 1) * 1103
            photos.shuffled(Random(seed)).take(limit)
        }
    }
    var currentIndex by rememberSaveable(memoryRefreshToken, idsKey) { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(0f) }
    var tagTarget by remember { mutableStateOf<PhotoItem?>(null) }

    LaunchedEffect(memoryItems.size) {
        if (memoryItems.isNotEmpty() && currentIndex >= memoryItems.size) {
            currentIndex = 0
        }
    }

    fun moveBy(delta: Int) {
        if (memoryItems.isEmpty()) return
        currentIndex = (currentIndex + delta + memoryItems.size) % memoryItems.size
        dragOffset = 0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .pointerInput(memoryItems, currentIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragOffset <= -72f -> moveBy(1)
                                dragOffset >= 72f -> moveBy(-1)
                                else -> dragOffset = 0f
                            }
                        },
                        onDragCancel = { dragOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount).coerceIn(-140f, 140f)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (memoryItems.isEmpty()) {
                EmptyMemoryCard(isLoading = isLoading)
            } else {
                val left = memoryItems[(currentIndex - 1 + memoryItems.size) % memoryItems.size]
                val right = memoryItems[(currentIndex + 1) % memoryItems.size]
                val current = memoryItems[currentIndex]

                MemorySideCard(
                    photo = left,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-72).dp, y = 10.dp)
                        .zIndex(1f),
                    rotation = -10f,
                )
                MemorySideCard(
                    photo = right,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 72.dp, y = 12.dp)
                        .zIndex(1f),
                    rotation = 10f,
                )
                MemoryMainCard(
                    photo = current,
                    dragOffset = dragOffset,
                    onAddTag = { tagTarget = current },
                    onKeep = {
                        onKeep(current)
                        moveBy(1)
                    },
                    onDelete = {
                        onMoveToRecycleBin(current)
                        moveBy(1)
                    },
                    modifier = Modifier.zIndex(2f),
                )
            }
        }

        if (memoryItems.isNotEmpty()) {
            BlindBoxPageIndicator(
                currentIndex = currentIndex,
                pageCount = memoryItems.size,
            )
        }
    }

    tagTarget?.let { photo ->
        AddTagDialog(
            photo = photo,
            onDismiss = { tagTarget = null },
            onConfirm = { tag ->
                onAddTag(photo, tag)
                tagTarget = null
            },
        )
    }
}

@Composable
private fun BlindBoxPageIndicator(
    currentIndex: Int,
    pageCount: Int,
) {
    val progress = if (pageCount <= 1) 0f else currentIndex.toFloat() / (pageCount - 1).toFloat()
    Box(
        modifier = Modifier
            .width(187.dp)
            .height(13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFC8C2BD).copy(alpha = 0.52f)),
        )
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(3.dp)
                .offset(x = ((progress - 0.5f) * 54).dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF8D8883).copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun MemorySideCard(
    photo: PhotoItem,
    modifier: Modifier = Modifier,
    rotation: Float,
) {
    AsyncImage(
        model = photo.uri,
        contentDescription = null,
        modifier = modifier
            .width(190.dp)
            .height(250.dp)
            .graphicsLayer {
                rotationZ = rotation
                alpha = 0.50f
            }
            .blur(0.8.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(4.dp, HomeCardBorder.copy(alpha = 0.68f), RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun MemoryMainCard(
    photo: PhotoItem,
    dragOffset: Float,
    onAddTag: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedX by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.78f),
        label = "memory-card-drag",
    )
    val actionAlpha = (abs(dragOffset) / 90f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(230.dp)
            .height(304.dp)
            .offset { IntOffset(animatedX.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = 4f
                scaleX = 1f - (abs(dragOffset) / 620f).coerceAtMost(0.05f)
                scaleY = 1f - (abs(dragOffset) / 620f).coerceAtMost(0.05f)
            }
            .shadow(18.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .border(4.dp, HomeCardBorder, RoundedCornerShape(24.dp)),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .padding(start = 14.dp, top = 14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(HomeAccent)
                .clickable(onClick = onAddTag)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add+",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemoryActionButton(
                text = "保留",
                color = HomeAccent,
                background = HomeAccent,
                onClick = onKeep,
            )
            MemoryActionButton(
                text = "删除",
                color = Color.White,
                background = Color(0xFF242424),
                onClick = onDelete,
            )
        }

        if (actionAlpha > 0f) {
            val keepHint = dragOffset < 0
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (keepHint) {
                            Brush.horizontalGradient(listOf(Color(0x55FFB45D), Color.Transparent))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color(0x55242424)))
                        },
                    )
                    .graphicsLayer { alpha = actionAlpha * 0.58f },
            )
        }
    }
}

@Composable
private fun MemoryActionButton(
    text: String,
    color: Color,
    background: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (background == HomeAccent) Color.White else color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyMemoryCard(
    isLoading: Boolean,
) {
    Box(
        modifier = Modifier
            .width(230.dp)
            .height(304.dp)
            .graphicsLayer { rotationZ = 4f }
            .shadow(18.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .border(4.dp, HomeCardBorder, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isLoading) "正在读取照片" else "暂无照片",
            color = HomeSubtle,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AddTagDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var tag by remember(photo.stableId) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "添加标签") },
        text = {
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                singleLine = true,
                label = { Text("标签名称") },
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tag) },
                enabled = tag.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun PhotoOverviewPanel(
    uiState: PhotoLibraryUiState,
    onStartTidy: () -> Unit,
) {
    val media = uiState.visibleMedia
    val photos = media.filter { it.mediaType == MediaType.PHOTO }
    val videos = media.filter { it.mediaType == MediaType.VIDEO }
    val screenshots = media.filter { it.isScreenshotLike() }
    val mediaById = media.associateBy { it.stableId }
    val similarPhotoGroups = uiState.similarGroups
        .map { group -> group.mediaIds.mapNotNull { mediaById[it] } }
        .filter { it.size >= 2 }
    val duplicateCount = media.groupBy { it.name.lowercase(Locale.getDefault()) }
        .values
        .sumOf { group -> if (group.size > 1) group.size else 0 }
    val similarCount = similarPhotoGroups.size
    val similarBytes = similarPhotoGroups.sumOf { group -> group.sumOf { it.sizeBytes } }
    val similarPreview = similarPhotoGroups.firstOrNull()?.take(3) ?: photos.take(3)
    val largeVideos = videos.filter { it.sizeBytes >= 100L * 1024L * 1024L }
    val pendingCleanBytes = similarBytes +
        largeVideos.sumOf { it.sizeBytes } +
        screenshots.sumOf { it.sizeBytes }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "照片概览",
            color = HomeInk,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        StorageOverviewCard(
            totalBytes = uiState.totalBytes,
            mediaCount = uiState.totalMediaCount,
            pendingCleanBytes = pendingCleanBytes,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            OverviewCategoryCard(
                title = "相似照片",
                value = "${similarCount}组",
                detail = formatBytes(similarBytes),
                photos = similarPreview,
                onClick = onStartTidy,
                modifier = Modifier.weight(1f),
            )
            OverviewCategoryCard(
                title = "重复照片",
                value = "${duplicateCount}张",
                detail = formatBytes(media.groupBy { it.name }.values.flatten().take(duplicateCount).sumOf { it.sizeBytes }),
                photos = photos.drop(2).take(3),
                onClick = onStartTidy,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            OverviewCategoryCard(
                title = "大型视频",
                value = "${largeVideos.size}个",
                detail = formatBytes(largeVideos.sumOf { it.sizeBytes }),
                photos = largeVideos.take(3).ifEmpty { media.take(3) },
                onClick = onStartTidy,
                modifier = Modifier.weight(1f),
            )
            OverviewCategoryCard(
                title = "截图",
                value = "${screenshots.size}张",
                detail = formatBytes(screenshots.sumOf { it.sizeBytes }),
                photos = screenshots.take(3).ifEmpty { photos.takeLast(3) },
                onClick = onStartTidy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StorageOverviewCard(
    totalBytes: Long,
    mediaCount: Int,
    pendingCleanBytes: Long,
) {
    val totalCapacity = 251.89 * 1024.0 * 1024.0 * 1024.0
    val progress = (totalBytes.toDouble() / totalCapacity).coerceIn(0.04, 1.0).toFloat()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = "存储",
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 12.sp,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                        append(formatBytes(totalBytes))
                    }
                    withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.5f), fontSize = 16.sp)) {
                        append("/251.89GB")
                    }
                },
                maxLines = 1,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${mediaCount}个文件",
                    color = Color(0xFFFFB65F),
                    fontSize = 12.sp,
                )
                Text(
                    text = "${formatBytes(pendingCleanBytes)}待清理",
                    color = Color(0xFFFFB65F),
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFECECEC)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFC484), Color(0xFFFF8C66)),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun OverviewCategoryCard(
    title: String,
    value: String,
    detail: String,
    photos: List<PhotoItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(105.dp)
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 12.dp, end = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = title,
                        color = HomeInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFB6AAA5),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = value,
                    color = HomeInk,
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = detail,
                    color = HomeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MiniPhotoStack(photos = photos)
        }
    }
}

@Composable
private fun MiniPhotoStack(
    photos: List<PhotoItem>,
) {
    Box(
        modifier = Modifier
            .width(58.dp)
            .height(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        val stack = photos.take(3)
        if (stack.isEmpty()) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = ((index - 1) * 8).dp, y = ((2 - index) * 3).dp)
                        .graphicsLayer { rotationZ = (index - 1) * 8f }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFE6CA)),
                )
            }
        } else {
            stack.forEachIndexed { index, photo ->
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = ((index - 1) * 9).dp, y = ((2 - index) * 3).dp)
                        .graphicsLayer { rotationZ = (index - 1) * 9f }
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0L).toDouble()
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        value >= gb -> String.format(Locale.US, "%.2fGB", value / gb)
        value >= mb -> String.format(Locale.US, "%.1fMB", value / mb)
        value >= kb -> String.format(Locale.US, "%.1fKB", value / kb)
        else -> "${bytes.coerceAtLeast(0L)}B"
    }
}

private fun PhotoItem.isScreenshotLike(): Boolean {
    val text = "$name $bucketName $mimeType".lowercase(Locale.getDefault())
    return text.contains("screenshot") ||
        text.contains("screen") ||
        text.contains("截屏") ||
        text.contains("截图")
}

private fun List<PhotoItem>.searchByKeyword(
    query: String,
    tags: Map<String, String>,
    locations: Map<String, PhotoLocation>,
    personClusters: List<PersonCluster>,
    contentAnalysis: Map<String, PhotoContentAnalysis>,
): List<PhotoItem> {
    val tokens = query
        .trim()
        .lowercase(Locale.getDefault())
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyList()

    val peopleByMediaId = personClusters
        .filter { !it.displayName.isNullOrBlank() }
        .flatMap { person ->
            person.mediaIds.map { mediaId -> mediaId to person.displayName.orEmpty() }
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second },
        )

    return mapNotNull { item ->
        val haystack = item.searchHaystack(
            tag = tags[item.stableId].orEmpty(),
            location = locations[item.stableId],
            people = peopleByMediaId[item.stableId].orEmpty(),
            analysis = contentAnalysis[item.stableId],
        )
        val matchCount = tokens.count { token -> haystack.contains(token) }
        if (matchCount == tokens.size) {
            SearchHit(item = item, score = item.searchScore(tokens, haystack))
        } else {
            null
        }
    }
        .sortedWith(compareByDescending<SearchHit> { it.score }.thenByDescending { it.item.dateTakenMillis })
        .map { it.item }
}

private fun PhotoItem.searchHaystack(
    tag: String,
    location: PhotoLocation?,
    people: List<String>,
    analysis: PhotoContentAnalysis?,
): String {
    val typeWords = buildList {
        add(mediaType.name)
        if (mediaType == MediaType.PHOTO) add("照片")
        if (mediaType == MediaType.VIDEO) add("视频")
        if (isScreenshotLike()) {
            add("截图")
            add("截屏")
            add("screenshot")
        }
    }
    val locationWords = listOfNotNull(
        location?.country,
        location?.province,
        location?.city,
        location?.district,
        location?.poiName,
    )
    val contentWords = analysis?.let { content ->
        content.labels + content.aliases + buildList {
            if (content.faceCount > 0) {
                add("人")
                add("人物")
                add("人脸")
                add("人像")
                add("自拍")
            }
            if (content.faceCount > 1) {
                add("多人")
                add("合照")
            }
        }
    }.orEmpty()

    return (listOf(name, bucketName, dateLabel, mimeType, tag) + typeWords + locationWords + people + contentWords)
        .joinToString(" ")
        .lowercase(Locale.getDefault())
}

private fun PhotoItem.searchScore(tokens: List<String>, haystack: String): Int {
    var score = 0
    tokens.forEach { token ->
        if (name.lowercase(Locale.getDefault()).contains(token)) score += 6
        if (bucketName.lowercase(Locale.getDefault()).contains(token)) score += 5
        if (dateLabel.lowercase(Locale.getDefault()).contains(token)) score += 4
        if (haystack.contains(token)) score += 1
    }
    return score
}

private data class SearchHit(
    val item: PhotoItem,
    val score: Int,
)

@Preview(showBackground = true, widthDp = 375, heightDp = 814)
@Composable
private fun HomeScreenPreview() {
    val sample = PhotoLibraryUiState(
        accessStatus = PhotoAccessStatus.Full,
        media = SampleData.photos,
    )
    MaterialTheme {
        HomeScreen(
            uiState = sample,
            onOpenGallery = {},
            onStartTidy = {},
            onOpenRecycleBin = {},
            onOpenAlbum = {},
            onKeep = {},
            onMoveToRecycleBin = {},
            onAddTag = { _, _ -> },
            memoryBatchSize = 30,
            memoryRefreshToken = 0,
            onRefreshMemoryBox = {},
        )
    }
}
