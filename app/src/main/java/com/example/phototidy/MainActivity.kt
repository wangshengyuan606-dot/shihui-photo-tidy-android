package com.example.phototidy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.phototidy.model.MediaType
import com.example.phototidy.model.PhotoItem
import com.example.phototidy.model.ReviewGestureMode
import com.example.phototidy.navigation.Screen
import com.example.phototidy.ui.components.AppPaperBackground
import com.example.phototidy.ui.components.edgeHorizontalBackGesture
import com.example.phototidy.ui.screens.compress.CompressScreen
import com.example.phototidy.ui.screens.home.HomeScreen
import com.example.phototidy.ui.screens.home.formatBytes
import com.example.phototidy.ui.screens.mine.MineScreen
import com.example.phototidy.ui.screens.tidy.TidyScreen
import com.example.phototidy.ui.screens.tidystatus.TidyStatusScreen
import com.example.phototidy.ui.theme.PhotoTidyAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoTidyAndroidTheme(darkTheme = false) {
                PhotoTidyApp()
            }
        }
    }
}

@Composable
private fun PhotoTidyApp(
    viewModel: PhotoLibraryViewModel = viewModel(),
) {
    val context = LocalContext.current
    var accessStatus by remember { mutableStateOf(context.photoAccessStatus()) }
    val uiState by viewModel.uiState.collectAsState()
    var routeHistory by rememberSaveable { mutableStateOf(listOf(Screen.Home.route)) }
    var hideScaffoldChrome by rememberSaveable { mutableStateOf(false) }
    var reviewGestureMode by rememberSaveable { mutableStateOf(ReviewGestureMode.Classic) }
    var memoryBatchSize by rememberSaveable { mutableStateOf(30) }
    var memoryRefreshToken by rememberSaveable { mutableStateOf(0) }
    var pendingAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedScreen = routeHistory.lastOrNull() ?: Screen.Home.route

    fun navigateTo(route: String) {
        routeHistory = if (Screen.bottomNavItems().any { it.route == route }) {
            listOf(route)
        } else {
            val currentRoute = routeHistory.lastOrNull()
            if (currentRoute == route) routeHistory else routeHistory + route
        }
    }

    fun navigateBack() {
        if (routeHistory.size > 1) {
            routeHistory = routeHistory.dropLast(1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        accessStatus = context.photoAccessStatus()
        routeHistory = listOf(Screen.Home.route)
        viewModel.refresh(accessStatus)
    }

    LaunchedEffect(accessStatus) {
        viewModel.refresh(accessStatus)
    }

    LaunchedEffect(selectedScreen) {
        if (selectedScreen != Screen.Tidy.route) {
            hideScaffoldChrome = false
        }
    }

    if (accessStatus == PhotoAccessStatus.None) {
        PermissionScreen(
            onRequestPermission = {
                permissionLauncher.launch(photoPermissionRequest())
            },
        )
        return
    }

    MainScaffold(
        selectedRoute = selectedScreen,
        uiState = uiState,
        viewModel = viewModel,
        hideChrome = hideScaffoldChrome,
        reviewGestureMode = reviewGestureMode,
        memoryBatchSize = memoryBatchSize,
        memoryRefreshToken = memoryRefreshToken,
        pendingAlbumId = pendingAlbumId,
        canNavigateBack = routeHistory.size > 1,
        onSelectedRouteChange = ::navigateTo,
        onNavigateBack = ::navigateBack,
        onReviewGestureModeChange = { reviewGestureMode = it },
        onMemoryBatchSizeChange = { memoryBatchSize = it },
        onRefreshMemoryBox = { memoryRefreshToken += 1 },
        onOpenAlbumFromHome = { albumId ->
            pendingAlbumId = albumId
            navigateTo(Screen.Tidy.route)
        },
        onInitialAlbumHandled = { pendingAlbumId = null },
        onTidyChromeHiddenChange = { hideScaffoldChrome = it },
        onRequestMorePhotos = {
            permissionLauncher.launch(photoPermissionRequest())
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    selectedRoute: String,
    uiState: PhotoLibraryUiState,
    viewModel: PhotoLibraryViewModel,
    hideChrome: Boolean,
    reviewGestureMode: ReviewGestureMode,
    memoryBatchSize: Int,
    memoryRefreshToken: Int,
    pendingAlbumId: String?,
    canNavigateBack: Boolean,
    onSelectedRouteChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onReviewGestureModeChange: (ReviewGestureMode) -> Unit,
    onMemoryBatchSizeChange: (Int) -> Unit,
    onRefreshMemoryBox: () -> Unit,
    onOpenAlbumFromHome: (String) -> Unit,
    onInitialAlbumHandled: () -> Unit,
    onTidyChromeHiddenChange: (Boolean) -> Unit,
    onRequestMorePhotos: () -> Unit,
) {
    val currentScreen = Screen.fromRoute(selectedRoute)
    BackHandler(enabled = canNavigateBack) {
        onNavigateBack()
    }

    AppPaperBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.edgeHorizontalBackGesture(
            enabled = canNavigateBack,
            onBack = onNavigateBack,
        ),
        containerColor = Color.Transparent,
        topBar = {
            if (!hideChrome && currentScreen != Screen.Home && currentScreen != Screen.Tidy && currentScreen != Screen.Mine && currentScreen != Screen.RecycleBin && currentScreen != Screen.Compress) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = if (currentScreen == Screen.Tidy) 28.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                            lineHeight = if (currentScreen == Screen.Tidy) 34.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                            fontWeight = if (currentScreen == Screen.Tidy) FontWeight.Bold else null,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            }
        },
        bottomBar = {
            if (!hideChrome && currentScreen != Screen.RecycleBin && currentScreen != Screen.Compress) {
                HomeStyleBottomBar(
                    selectedRoute = selectedRoute,
                    onSelectedRouteChange = onSelectedRouteChange,
                )
            }
        },
    ) { innerPadding ->
        when (selectedRoute) {
            Screen.Tidy.route -> TidyScreen(
                uiState = uiState,
                initialAlbumId = pendingAlbumId,
                onInitialAlbumHandled = onInitialAlbumHandled,
                onKeep = viewModel::markKept,
                onMoveToRecycleBin = viewModel::moveToRecycleBin,
                onToggleSelection = viewModel::toggleSelection,
                onSelectOnly = { item -> viewModel.selectOnly(listOf(item)) },
                onSelectItems = viewModel::selectOnly,
                onClearSelection = viewModel::clearSelection,
                onCompressSelected = { onSelectedRouteChange(Screen.Compress.route) },
                onMoveSelectedToRecycleBin = viewModel::moveSelectedToRecycleBin,
                reviewGestureMode = reviewGestureMode,
                onArchiveToTag = viewModel::addTag,
                onChromeHiddenChange = onTidyChromeHiddenChange,
                contentPadding = innerPadding,
            )

            Screen.Mine.route -> MineScreen(
                uiState = uiState,
                onOpenRecycleBin = { onSelectedRouteChange(Screen.RecycleBin.route) },
                onRequestMorePhotos = onRequestMorePhotos,
                onClearLocalRecords = viewModel::clearLocalRecords,
                reviewGestureMode = reviewGestureMode,
                onReviewGestureModeChange = onReviewGestureModeChange,
                memoryBatchSize = memoryBatchSize,
                onMemoryBatchSizeChange = onMemoryBatchSizeChange,
                contentPadding = innerPadding,
            )

            Screen.Gallery.route -> GalleryScreen(
                media = uiState.visibleMedia,
                isLoading = uiState.isLoading,
                contentPadding = innerPadding,
            )

            Screen.RecycleBin.route -> TidyStatusScreen(
                recycleBinMedia = uiState.recycleBinMedia,
                recycleBinCount = uiState.recycleBinCount,
                recycleBinBytes = uiState.recycleBinBytes,
                onBack = onNavigateBack,
                onRestore = viewModel::restoreFromRecycleBin,
                onClearRecycleBin = viewModel::clearRecycleBin,
                onConfirmPermanentDelete = viewModel::confirmPermanentDeleteFromRecycleBin,
                onConfirmPermanentDeleteSelected = viewModel::confirmPermanentDeleteSelectedFromRecycleBin,
                contentPadding = PaddingValues(0.dp),
            )

            Screen.Compress.route -> CompressScreen(
                selectedMedia = uiState.selectedMedia,
                onCancel = { onSelectedRouteChange(Screen.Tidy.route) },
                onCompress = {
                    viewModel.clearSelection()
                    onSelectedRouteChange(Screen.Tidy.route)
                },
                contentPadding = PaddingValues(0.dp),
            )

            else -> HomeScreen(
                uiState = uiState,
                onOpenGallery = { onSelectedRouteChange(Screen.Gallery.route) },
                onStartTidy = { onSelectedRouteChange(Screen.Tidy.route) },
                onOpenRecycleBin = {},
                onOpenAlbum = { album -> onOpenAlbumFromHome(album.bucketId) },
                onKeep = viewModel::markKept,
                onMoveToRecycleBin = viewModel::moveToRecycleBin,
                onAddTag = viewModel::addTag,
                memoryBatchSize = memoryBatchSize,
                memoryRefreshToken = memoryRefreshToken,
                onRefreshMemoryBox = onRefreshMemoryBox,
                modifier = Modifier.padding(innerPadding),
            )
        }

        if (!hideChrome && uiState.accessStatus == PhotoAccessStatus.Partial) {
            PartialAccessBanner(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                onRequestMorePhotos = onRequestMorePhotos,
            )
        }
    }
    }
}

@Composable
private fun HomeStyleBottomBar(
    selectedRoute: String,
    onSelectedRouteChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        color = Color.White,
        shadowElevation = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp)
                .navigationBarsPadding()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            Screen.bottomNavItems().forEach { screen ->
                val selected = selectedRoute == screen.route
                val tint = if (selected) Color(0xFFFFA14F) else Color(0xFFA4A4A4)
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onSelectedRouteChange(screen.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selected) {
                                    Color(0xFFFFE6CA).copy(alpha = 0.72f)
                                } else {
                                    Color.Transparent
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = tint,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = screen.title,
                        color = tint,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(22.dp)
                    .size(54.dp),
            )
        }
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = "整理相册，从本地开始",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = "需要访问照片和视频，用于本地浏览和整理。默认不会上传到服务器。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("开启相册权限")
        }
    }
}

@Composable
private fun GalleryScreen(
    media: List<PhotoItem>,
    isLoading: Boolean,
    contentPadding: PaddingValues,
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        media.isEmpty() -> {
            EmptyGallery(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = media,
                    key = { it.stableId },
                ) { item ->
                    PhotoGridItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    item: PhotoItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                if (item.isVideo) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .size(22.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.name.ifBlank { "未命名媒体" },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.dateLabel} · ${formatBytes(item.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyGallery(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Photo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "没有读取到媒体",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "如果选择了部分照片，可以在“我的”里选择更多照片和视频。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PartialAccessBanner(
    onRequestMorePhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "当前为部分媒体授权",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onRequestMorePhotos) {
            Text("选择更多")
        }
    }
}

private fun Context.photoAccessStatus(): PhotoAccessStatus {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val hasImages = hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
            val hasVideos = hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
            val hasPartial = hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            when {
                hasImages && hasVideos -> PhotoAccessStatus.Full
                hasPartial || hasImages || hasVideos -> PhotoAccessStatus.Partial
                else -> PhotoAccessStatus.None
            }
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            val hasImages = hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
            val hasVideos = hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
            when {
                hasImages && hasVideos -> PhotoAccessStatus.Full
                hasImages || hasVideos -> PhotoAccessStatus.Partial
                else -> PhotoAccessStatus.None
            }
        }

        else -> {
            if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PhotoAccessStatus.Full
            } else {
                PhotoAccessStatus.None
            }
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun photoPermissionRequest(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        )

        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 800)
@Composable
private fun PermissionScreenPreview() {
    PhotoTidyAndroidTheme {
        PermissionScreen(onRequestPermission = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 800)
@Composable
private fun GalleryScreenPreview() {
    PhotoTidyAndroidTheme {
        GalleryScreen(
            media = previewMedia(),
            isLoading = false,
            contentPadding = PaddingValues(0.dp),
        )
    }
}

private fun previewMedia(): List<PhotoItem> {
    return List(18) { index ->
        PhotoItem(
            id = index.toLong(),
            uri = "https://picsum.photos/seed/photo-tidy-$index/600/600",
            name = "IMG_202606${(index % 9) + 1}.jpg",
            sizeBytes = 1_200_000L + index * 210_000L,
            dateLabel = "2026-06-${(index % 9) + 1}",
            width = 3024,
            height = 4032,
            mediaType = if (index % 5 == 0) MediaType.VIDEO else MediaType.PHOTO,
        )
    }
}
