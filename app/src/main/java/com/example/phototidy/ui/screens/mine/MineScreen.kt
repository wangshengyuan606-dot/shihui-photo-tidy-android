package com.example.phototidy.ui.screens.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototidy.PhotoAccessStatus
import com.example.phototidy.PhotoLibraryUiState
import com.example.phototidy.model.ReviewGestureMode
import com.example.phototidy.ui.screens.home.formatBytes
import com.example.phototidy.ui.theme.PhotoTidyAndroidTheme

@Composable
fun MineScreen(
    uiState: PhotoLibraryUiState,
    onOpenRecycleBin: () -> Unit,
    onRequestMorePhotos: () -> Unit,
    onClearLocalRecords: () -> Unit,
    reviewGestureMode: ReviewGestureMode,
    onReviewGestureModeChange: (ReviewGestureMode) -> Unit,
    memoryBatchSize: Int,
    onMemoryBatchSizeChange: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "我的",
                color = Color(0xFF282828),
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        item {
            SettingCard(
                icon = Icons.Default.Delete,
                title = "回收站",
                subtitle = "${uiState.recycleBinCount} 项 · ${formatBytes(uiState.recycleBinBytes)}",
                actionText = "打开",
                onAction = onOpenRecycleBin,
            )
        }

        item {
            GestureModeCard(
                reviewGestureMode = reviewGestureMode,
                onReviewGestureModeChange = onReviewGestureModeChange,
            )
        }

        item {
            MemoryBatchSizeCard(
                memoryBatchSize = memoryBatchSize,
                onMemoryBatchSizeChange = onMemoryBatchSizeChange,
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            SettingCard(
                icon = Icons.Default.Info,
                title = "关于 App",
                subtitle = "Photo Tidy Demo · Kotlin + Jetpack Compose · 本地优先",
                actionText = null,
                onAction = {},
            )
        }

        item {
            SettingCard(
                icon = Icons.Default.Lock,
                title = "权限状态",
                subtitle = accessLabel(uiState.accessStatus),
                actionText = "选择更多",
                onAction = onRequestMorePhotos,
            )
        }
    }
}

@Composable
private fun GestureModeCard(
    reviewGestureMode: ReviewGestureMode,
    onReviewGestureModeChange: (ReviewGestureMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingLabel(
                icon = Icons.Default.Palette,
                title = "手势习惯",
                subtitle = "选择大图整理页的保留和删除方式",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GestureModeOption(
                    selected = reviewGestureMode == ReviewGestureMode.Classic,
                    title = "左右滑动",
                    subtitle = "左滑保留\n右滑删除",
                    onClick = { onReviewGestureModeChange(ReviewGestureMode.Classic) },
                    modifier = Modifier.weight(1f),
                )
                GestureModeOption(
                    selected = reviewGestureMode == ReviewGestureMode.UpDeleteLeftKeep,
                    title = "上滑删除",
                    subtitle = "上滑删除\n左滑保留",
                    onClick = { onReviewGestureModeChange(ReviewGestureMode.UpDeleteLeftKeep) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GestureModeOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) Color(0xFF171717) else Color.White.copy(alpha = 0.62f)
    val background = if (selected) Color.White else Color.White.copy(alpha = 0.48f)

    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF171717),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6D6964),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MemoryBatchSizeCard(
    memoryBatchSize: Int,
    onMemoryBatchSizeChange: (Int) -> Unit,
) {
    val options = listOf(15, 30, 60, 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingLabel(
                icon = Icons.Default.Refresh,
                title = "每组照片数量",
                subtitle = "控制首页时光盲盒一次抽取的照片数量",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    BatchSizeOption(
                        label = if (option == 0) "无限" else option.toString(),
                        selected = memoryBatchSize == option,
                        onClick = { onMemoryBatchSizeChange(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchSizeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color.White else Color.White.copy(alpha = 0.46f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.4.dp, Color(0xFF171717)) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color(0xFF171717) else Color(0xFF6D6964),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SettingLabel(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String?,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingLabel(
                icon = icon,
                title = title,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            if (actionText != null) {
                Button(onClick = onAction) {
                    Text(actionText)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun accessLabel(status: PhotoAccessStatus): String {
    return when (status) {
        PhotoAccessStatus.Full -> "可访问全部照片和视频"
        PhotoAccessStatus.Partial -> "只访问用户选择的部分媒体"
        PhotoAccessStatus.None -> "尚未授权"
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 800)
@Composable
private fun MineScreenPreview() {
    PhotoTidyAndroidTheme {
        MineScreen(
            uiState = PhotoLibraryUiState(
                accessStatus = PhotoAccessStatus.Full,
                recycleBinIds = setOf("PHOTO:1", "PHOTO:2"),
            ),
            onOpenRecycleBin = {},
            onRequestMorePhotos = {},
            onClearLocalRecords = {},
            reviewGestureMode = ReviewGestureMode.Classic,
            onReviewGestureModeChange = {},
            memoryBatchSize = 30,
            onMemoryBatchSizeChange = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
