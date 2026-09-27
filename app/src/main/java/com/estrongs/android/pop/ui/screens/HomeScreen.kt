package com.estrongs.android.pop.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrongs.android.pop.data.model.FileCategory
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.data.model.StorageInfo
import com.estrongs.android.pop.data.repository.ShizukuStatus
import com.estrongs.android.pop.ui.theme.*
import com.estrongs.android.pop.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    storageVolumes: List<StorageInfo>,
    recentFiles: List<FileItem>,
    isRootMode: Boolean,
    isRootGranted: Boolean,
    shizukuStatus: ShizukuStatus = ShizukuStatus(),
    hasStoragePermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onRequestShizuku: () -> Unit = {},
    onOpenTerminal: () -> Unit = {},
    onOpenNewNote: () -> Unit = {},
    onExploreStorage: (String) -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onToggleRootMode: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onOpenFile: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Storage Permission Alert Card if not granted
        if (!hasStoragePermission) {
            item {
                PermissionAlertBanner(onRequestPermission)
            }
        }

        // Hero Unified Storage Card (Clean, modern, clear)
        item {
            val primary = storageVolumes.firstOrNull() ?: StorageInfo(
                title = "Internal Storage",
                path = "/storage/emulated/0",
                totalBytes = 64L * 1024L * 1024L * 1024L,
                usedBytes = 28L * 1024L * 1024L * 1024L,
                freeBytes = 36L * 1024L * 1024L * 1024L,
                isPrimary = true
            )
            HeroStorageCard(
                volume = primary,
                shizukuStatus = shizukuStatus,
                isRootMode = isRootMode,
                onRequestShizuku = onRequestShizuku,
                onExplore = { onExploreStorage(primary.path) },
                onAnalyze = { onNavigate(Screen.STORAGE_ANALYZER) },
                onClean = { onNavigate(Screen.CLEANER) },
                onToggleRoot = onToggleRootMode
            )
        }

        // Simplified Category Grid (Clean 2 rows of 4 items)
        item {
            SimplifiedCategoryGrid(
                onCategoryClick = onCategoryClick,
                onNewNoteClick = onOpenNewNote,
                onAppsClick = { onNavigate(Screen.APP_MANAGER) }
            )
        }

        // Quick Tools Row (Clean, streamlined)
        item {
            SimplifiedToolsRow(
                isRootMode = isRootMode,
                onOpenTerminal = onOpenTerminal,
                onOpenNewNote = onOpenNewNote,
                onNavigate = onNavigate
            )
        }

        // Recent Files Carousel
        if (recentFiles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { onCategoryClick(FileCategory.RECENT) },
                        modifier = Modifier.testTag("home_view_all_recent")
                    ) {
                        Text("View all", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentFiles) { file ->
                        SimplifiedRecentFileCard(file = file, onClick = { onOpenFile(file) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStorageCard(
    volume: StorageInfo,
    shizukuStatus: ShizukuStatus,
    isRootMode: Boolean,
    onRequestShizuku: () -> Unit,
    onExplore: () -> Unit,
    onAnalyze: () -> Unit,
    onClean: () -> Unit,
    onToggleRoot: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_storage_hero_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ESBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = ESBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Internal Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = volume.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Shizuku / ADB Privilege Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ESAccentGreen.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clickable { onRequestShizuku() }
                        .testTag("shizuku_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = ESAccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (shizukuStatus.isInternalEngineActive) "Shizuku ADB Active" else "Shizuku Privileged",
                            color = ESAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean modern Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${volume.formattedUsed} used",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${volume.formattedFree} free of ${volume.formattedTotal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { volume.usedPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (volume.usedPercent > 0.88f) Color(0xFFEF4444) else ESBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExplore,
                    modifier = Modifier.weight(1.2f).testTag("home_hero_explore_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ESBlue),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Files", fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f).testTag("home_hero_analyze_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analyze")
                }

                FilledTonalButton(
                    onClick = onClean,
                    modifier = Modifier.weight(1f).testTag("home_hero_clean_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp), tint = ESAccentGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clean")
                }
            }
        }
    }
}

@Composable
private fun SimplifiedCategoryGrid(
    onCategoryClick: (FileCategory) -> Unit,
    onNewNoteClick: () -> Unit,
    onAppsClick: () -> Unit
) {
    val items = listOf(
        SimplifiedCat("Images", Icons.Default.Image, ESAccentOrange) { onCategoryClick(FileCategory.IMAGES) },
        SimplifiedCat("Music", Icons.Default.MusicNote, ESAccentPurple) { onCategoryClick(FileCategory.MUSIC) },
        SimplifiedCat("Videos", Icons.Default.Movie, ESAccentRed) { onCategoryClick(FileCategory.VIDEOS) },
        SimplifiedCat("Documents", Icons.Default.Description, ESDocBlue) { onCategoryClick(FileCategory.DOCUMENTS) },
        SimplifiedCat("Apps", Icons.Default.Android, ESApkGreen) { onAppsClick() },
        SimplifiedCat("Archives", Icons.Default.FolderZip, ESZipPurple) { onCategoryClick(FileCategory.ARCHIVES) },
        SimplifiedCat("Downloads", Icons.Default.Download, ESAccentCyan) { onCategoryClick(FileCategory.DOWNLOADS) },
        SimplifiedCat("Note Editor", Icons.Default.EditNote, ESDocBlue) { onNewNoteClick() }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until 4) {
                        val item = items[row * 4 + col]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { item.action() }
                                .padding(vertical = 8.dp, horizontal = 2.dp)
                                .testTag("cat_pill_${item.name.lowercase().replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(item.color.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = item.name, tint = item.color, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SimplifiedCat(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

@Composable
private fun SimplifiedToolsRow(
    isRootMode: Boolean,
    onOpenTerminal: () -> Unit,
    onOpenNewNote: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ToolItemSimple(
                icon = Icons.Default.PieChart,
                label = "Analyzer",
                color = ESAccentCyan,
                testTag = "quick_tool_analyzer",
                onClick = { onNavigate(Screen.STORAGE_ANALYZER) }
            )
            ToolItemSimple(
                icon = Icons.Default.Apps,
                label = "App Manager",
                color = ESAccentAmber,
                testTag = "quick_tool_apps",
                onClick = { onNavigate(Screen.APP_MANAGER) }
            )
            ToolItemSimple(
                icon = Icons.Default.Terminal,
                label = "Terminal",
                color = ESAccentOrange,
                testTag = "quick_tool_terminal",
                onClick = onOpenTerminal
            )
            ToolItemSimple(
                icon = Icons.Default.DeleteOutline,
                label = "Recycle Bin",
                color = ESAccentRed,
                testTag = "quick_tool_recycle_bin",
                onClick = { onNavigate(Screen.RECYCLE_BIN) }
            )
            ToolItemSimple(
                icon = Icons.Default.Settings,
                label = "Settings",
                color = MaterialTheme.colorScheme.primary,
                testTag = "quick_tool_settings",
                onClick = { onNavigate(Screen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun ToolItemSimple(
    icon: ImageVector,
    label: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SimplifiedRecentFileCard(
    file: FileItem,
    onClick: () -> Unit
) {
    val fileColor = getCategoryColor(file.category)

    Card(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("recent_file_${file.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fileColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(file.category),
                    contentDescription = null,
                    tint = fileColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.formattedSize,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionAlertBanner(onRequest: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storage_permission_alert_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Storage Access Required",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Grant All Files Access to browse and edit files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

private fun getCategoryIcon(cat: FileCategory): ImageVector {
    return when (cat) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.MUSIC -> Icons.Default.MusicNote
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.APKS -> Icons.Default.Android
        FileCategory.ARCHIVES -> Icons.Default.FolderZip
        FileCategory.DOWNLOADS -> Icons.Default.Download
        else -> Icons.Default.InsertDriveFile
    }
}

private fun getCategoryColor(cat: FileCategory): Color {
    return when (cat) {
        FileCategory.IMAGES -> ESAccentOrange
        FileCategory.MUSIC -> ESAccentPurple
        FileCategory.VIDEOS -> ESAccentRed
        FileCategory.DOCUMENTS -> ESDocBlue
        FileCategory.APKS -> ESApkGreen
        FileCategory.ARCHIVES -> ESZipPurple
        FileCategory.DOWNLOADS -> ESAccentCyan
        else -> ESBlue
    }
}
