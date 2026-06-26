package com.example.phototidy.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Tidy : Screen("tidy", "整理", Icons.Default.AutoFixHigh)
    object Mine : Screen("mine", "我的", Icons.Default.Person)
    object Gallery : Screen("gallery", "相册", Icons.Default.PhotoLibrary)
    object RecycleBin : Screen("recycle_bin", "回收站", Icons.Default.Delete)
    object Compress : Screen("compress", "压缩", Icons.Default.Settings)

    companion object {
        fun bottomNavItems(): List<Screen> = listOf(Home, Tidy, Mine)

        fun fromRoute(route: String): Screen {
            return when (route) {
                Home.route -> Home
                Tidy.route -> Tidy
                Mine.route -> Mine
                Gallery.route -> Gallery
                RecycleBin.route -> RecycleBin
                Compress.route -> Compress
                else -> Home
            }
        }
    }
}
