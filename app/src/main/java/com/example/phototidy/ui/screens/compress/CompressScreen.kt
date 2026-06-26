package com.example.phototidy.ui.screens.compress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
fun CompressScreen(
    selectedMedia: List<PhotoItem>,
    onCancel: () -> Unit,
    onCompress: () -> Unit,
    contentPadding: PaddingValues,
) {
    var quality by rememberSaveable { mutableStateOf("中") }
    val estimatedSaveBytes = selectedMedia.sumOf { it.sizeBytes } / when (quality) {
        "低" -> 2
        "中" -> 3
        else -> 5
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(contentPadding),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CompressTopBar(
                count = selectedMedia.size,
                estimatedSaveBytes = estimatedSaveBytes,
                onBack = onCancel,
            )
            CompressGrid(media = selectedMedia)
            Spacer(modifier = Modifier.weight(1f))
        }

        CompressSettingsPanel(
            quality = quality,
            onQualityChange = { quality = it },
            enabled = selectedMedia.isNotEmpty(),
            onCompress = onCompress,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CompressTopBar(
    count: Int,
    estimatedSaveBytes: Long,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 6.dp),
    ) {
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
                .padding(start = 52.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "压缩",
                color = Color(0xFF222222),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${count}项·预计节省${formatBytes(estimatedSaveBytes)}",
                color = Color(0xFF9B9690),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CompressGrid(media: List<PhotoItem>) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val gridHeight = maxWidth + 3.dp

        if (media.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有选择照片",
                    color = Color(0xFF9B9690),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(
                    items = media,
                    key = { _, item -> item.stableId },
                ) { index, item ->
                    CompressPhotoTile(item = item, color = CompressTileColors[index % CompressTileColors.size])
                }
            }
        }
    }
}

@Composable
private fun CompressPhotoTile(
    item: PhotoItem,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(color),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
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
    }
}

@Composable
private fun CompressSettingsPanel(
    quality: String,
    onQualityChange: (String) -> Unit,
    enabled: Boolean,
    onCompress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "压缩选择",
                color = Color(0xFF222222),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "质量",
                color = Color(0xFF222222),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("低", "中", "高").forEach { option ->
                    QualitySegment(
                        text = option,
                        selected = quality == option,
                        onClick = { onQualityChange(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                StartCompressButton(
                    enabled = enabled,
                    onClick = onCompress,
                )
            }
        }
    }
}

@Composable
private fun QualitySegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFFFFAE4A) else Color(0xFFF0F0F0))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF222222),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StartCompressButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(152.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) Color(0xFFFFAE4A) else Color(0xFFE6E2DD),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "开始压缩",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val CompressTileColors = listOf(
    Color(0xFFA796FF),
    Color(0xFFFFD979),
    Color(0xFF42D7A4),
    Color(0xFFF06E9D),
)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CompressScreenPreview() {
    val selected = List(30) { index ->
        PhotoItem(
            id = index.toLong(),
            uri = "https://picsum.photos/seed/compress-$index/300/300",
            name = "IMG_202606${index + 1}.jpg",
            sizeBytes = 696_700L,
            dateLabel = "2026-06-${index + 1}",
            width = 4032,
            height = 3024,
            mediaType = if (index == 0) MediaType.VIDEO else MediaType.PHOTO,
        )
    }
    PhotoTidyAndroidTheme {
        CompressScreen(
            selectedMedia = selected,
            onCancel = {},
            onCompress = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
