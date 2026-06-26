package com.example.phototidy.ui.screens.tidystatus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.ui.screens.home.formatBytes
import com.example.phototidy.ui.theme.PhotoTidyAndroidTheme

@Composable
fun TidyStatusScreen(
    recycleBinMedia: List<PhotoItem>,
    recycleBinCount: Int,
    recycleBinBytes: Long,
    onBack: () -> Unit,
    onRestore: (PhotoItem) -> Unit,
    onClearRecycleBin: () -> Unit,
    onConfirmPermanentDelete: () -> Unit,
    onConfirmPermanentDeleteSelected: (List<PhotoItem>) -> Unit,
    contentPadding: PaddingValues,
) {
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    val selectedItems = remember(recycleBinMedia, selectedIds) {
        recycleBinMedia.filter { it.stableId in selectedIds }
    }
    val selectionMode = selectedIds.isNotEmpty()

    LaunchedEffect(recycleBinMedia) {
        selectedIds = selectedIds.intersect(recycleBinMedia.map { it.stableId }.toSet())
    }

    BackHandler {
        if (selectionMode) {
            selectedIds = emptySet()
        } else {
            onBack()
        }
    }

    if (showDeleteAllDialog) {
        ConfirmDeleteDialog(
            count = recycleBinCount,
            bytes = recycleBinBytes,
            onConfirm = {
                showDeleteAllDialog = false
                selectedIds = emptySet()
                onConfirmPermanentDelete()
            },
            onDismiss = { showDeleteAllDialog = false },
        )
    }

    if (showDeleteSelectedDialog) {
        ConfirmDeleteDialog(
            count = selectedItems.size,
            bytes = selectedItems.sumOf { it.sizeBytes },
            onConfirm = {
                showDeleteSelectedDialog = false
                onConfirmPermanentDeleteSelected(selectedItems)
                selectedIds = emptySet()
            },
            onDismiss = { showDeleteSelectedDialog = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(contentPadding),
    ) {
        if (recycleBinMedia.isEmpty()) {
            EmptyStatus(onBack = onBack)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                RecycleBinTopBar(
                    count = recycleBinCount,
                    selectionMode = selectionMode,
                    selectedCount = selectedItems.size,
                    allSelected = selectedItems.size == recycleBinMedia.size,
                    onBack = onBack,
                    onCancelSelection = { selectedIds = emptySet() },
                    onSelectAll = {
                        selectedIds = if (selectedItems.size == recycleBinMedia.size) {
                            emptySet()
                        } else {
                            recycleBinMedia.map { it.stableId }.toSet()
                        }
                    },
                    onDeleteAll = { showDeleteAllDialog = true },
                )

                Box(modifier = Modifier.weight(1f)) {
                    RecycleBinGrid(
                        media = recycleBinMedia,
                        selectedIds = selectedIds,
                        selectionMode = selectionMode,
                        onToggleSelection = { item ->
                            selectedIds = if (item.stableId in selectedIds) {
                                selectedIds - item.stableId
                            } else {
                                selectedIds + item.stableId
                            }
                        },
                        onBeginSelection = { item ->
                            selectedIds = selectedIds + item.stableId
                        },
                    )

                    Text(
                        text = "照片和视频在删除前会显示剩余天数。之后将永久删除。",
                        color = Color(0xFF9B9690),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, bottom = 104.dp),
                    )
                }
            }

            RecycleBinActionBar(
                selectionMode = selectionMode,
                canActOnSelection = selectedItems.isNotEmpty(),
                onRestoreSelected = {
                    selectedItems.forEach(onRestore)
                    selectedIds = emptySet()
                },
                onDeleteSelected = { showDeleteSelectedDialog = true },
                onDeleteAll = { showDeleteAllDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun RecycleBinTopBar(
    count: Int,
    selectionMode: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    onBack: () -> Unit,
    onCancelSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 8.dp),
    ) {
        if (selectionMode) {
            TextButton(
                onClick = onCancelSelection,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text("取消", color = Color(0xFFFF8A33))
            }
            Text(
                text = "已选择${selectedCount}项",
                color = Color(0xFF222222),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(if (allSelected) "取消全选" else "全选", color = Color(0xFFFF8A33))
            }
        } else {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF222222),
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "回收站",
                    color = Color(0xFF222222),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${count}项",
                    color = Color(0xFF9B9690),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color(0xFF222222),
                        modifier = Modifier.size(24.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("全部删除") },
                        onClick = {
                            menuExpanded = false
                            onDeleteAll()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecycleBinGrid(
    media: List<PhotoItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onToggleSelection: (PhotoItem) -> Unit,
    onBeginSelection: (PhotoItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 188.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items(
            items = media,
            key = { it.stableId },
        ) { item ->
            RecycleBinPhotoTile(
                item = item,
                selected = item.stableId in selectedIds,
                selectionMode = selectionMode,
                onClick = {
                    if (selectionMode) onToggleSelection(item)
                },
                onLongClick = { onBeginSelection(item) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecycleBinPhotoTile(
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
            .background(Color(0xFFE1DDD5))
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

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
                    .border(2.dp, Color.White),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 6.dp),
            color = Color.Black.copy(alpha = 0.30f),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = formatBytes(item.sizeBytes),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }

        if (selectionMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                color = Color.Black.copy(alpha = 0.22f),
                shape = RoundedCornerShape(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(2.dp)
                        .size(15.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 5.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) Color(0xFFFF8A33) else Color.White.copy(alpha = 0.20f))
                    .border(1.4.dp, Color.White, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecycleBinActionBar(
    selectionMode: Boolean,
    canActOnSelection: Boolean,
    onRestoreSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 42.dp, end = 42.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = if (selectionMode) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.Top,
        ) {
            if (selectionMode) {
                BottomIconAction(
                    icon = Icons.Default.Restore,
                    label = "恢复",
                    enabled = canActOnSelection,
                    onClick = onRestoreSelected,
                )
                BottomIconAction(
                    icon = Icons.Default.DeleteForever,
                    label = "彻底删除",
                    enabled = canActOnSelection,
                    onClick = onDeleteSelected,
                )
            } else {
                BottomIconAction(
                    icon = Icons.Default.Delete,
                    label = "全部删除",
                    enabled = true,
                    onClick = onDeleteAll,
                )
            }
        }
    }
}

@Composable
private fun BottomIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Color(0xFF222222) else Color(0xFFB8B4AE),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = if (enabled) Color(0xFF222222) else Color(0xFFB8B4AE),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    count: Int,
    bytes: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("确认彻底删除 $count 项媒体") },
        text = {
            Text(
                "将从 App 内回收站移除这些项目，预计释放 ${formatBytes(bytes)}。\n\n" +
                    "后续接入系统删除时，将从系统相册中删除原始文件。",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("确认删除")
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
private fun EmptyStatus(
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 5.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF222222),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFF5B625D),
                modifier = Modifier.size(52.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "回收站是空的",
                color = Color(0xFF282828),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "在整理详情页右滑照片即可删除到这里。",
                color = Color(0xFF7A746D),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun TidyStatusScreenPreview() {
    val previewMedia = List(16) { index ->
        PhotoItem(
            id = index.toLong(),
            uri = "https://picsum.photos/seed/status-$index/240/240",
            name = "IMG_202606${(index % 9) + 1}.jpg",
            sizeBytes = 696_700L,
            dateLabel = "2026-06-${(index % 9) + 1}",
            width = 3024,
            height = 4032,
            mediaType = if (index == 0) MediaType.VIDEO else MediaType.PHOTO,
        )
    }
    PhotoTidyAndroidTheme {
        TidyStatusScreen(
            recycleBinMedia = previewMedia,
            recycleBinCount = previewMedia.size,
            recycleBinBytes = previewMedia.sumOf { it.sizeBytes },
            onBack = {},
            onRestore = {},
            onClearRecycleBin = {},
            onConfirmPermanentDelete = {},
            onConfirmPermanentDeleteSelected = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
