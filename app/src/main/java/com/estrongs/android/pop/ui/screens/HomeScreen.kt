package com.estrongs.android.pop.ui.screens

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
import com.estrongs.android.pop.ui.theme.*
import com.estrongs.android.pop.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    storageVolumes: List<StorageInfo>,
    recentFiles: List<FileItem>,
    isRootMode: Boolean,
    isRootGranted: Boolean,
    hasStoragePermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onOpenTerminal: () -> Unit = {},
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
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Storage Permission Alert Card if not granted
        if (!hasStoragePermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_permission_alert_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All Files Access Required",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Android OS limits file access. Tap below to grant 'All files access' permission so ES File Explorer can browse, create, edit, and delete files on your internal storage and SD card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("grant_permission_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant File Access Now")
                        }
                    }
                }
            }
        }

        // Storage Status Cards
        item {
            val primary = storageVolumes.firstOrNull()
            if (primary != null) {
                StorageCard(
                    title = "Internal Storage",
                    path = primary.path,
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = ESBlue,
                    volume = primary,
                    onExplore = { onExploreStorage(primary.path) },
                    onAnalyze = { onNavigate(Screen.STORAGE_ANALYZER) },
                    onClean = { onNavigate(Screen.CLEANER) }
                )
            }
        }

        // Inner Filesystem & Root Storage Card
        item {
            val rootVol = storageVolumes.find { it.path == "/" } ?: storageVolumes.getOrNull(1)
            RootSystemCard(
                rootVolume = rootVol,
                isRootMode = isRootMode,
                isRootGranted = isRootGranted,
                onBrowseRoot = { onExploreStorage("/") },
                onToggleRootMode = onToggleRootMode
            )
        }

        // Category Grid (Clean & Lightweight)
        item {
            Text(
                text = "Library",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            CategoryGrid(
                onCategoryClick = onCategoryClick,
                onCleanClick = { onNavigate(Screen.CLEANER) },
                onAppsClick = { onNavigate(Screen.APP_MANAGER) }
            )
        }

        // Quick Tools Row
        item {
            QuickToolsRow(
                isRootMode = isRootMode,
                onToggleRoot = onToggleRootMode,
                onOpenTerminal = onOpenTerminal,
                onNavigate = onNavigate
            )
        }

        // Recent Files
        if (recentFiles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { onCategoryClick(FileCategory.RECENT) },
                        modifier = Modifier.testTag("home_view_all_recent")
                    ) {
                        Text("View all")
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentFiles) { file ->
                        RecentFileCard(file = file, onClick = { onOpenFile(file) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCard(
    title: String,
    path: String,
    icon: ImageVector,
    iconTint: Color,
    volume: StorageInfo,
    onExplore: () -> Unit,
    onAnalyze: () -> Unit,
    onClean: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_storage_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = path,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${(volume.usedPercent * 100).toInt()}% Used",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (volume.usedPercent > 0.85f) MaterialTheme.colorScheme.error else ESBlue
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { volume.usedPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (volume.usedPercent > 0.85f) MaterialTheme.colorScheme.error else ESBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${volume.formattedUsed} used / ${volume.formattedTotal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${volume.formattedFree} free",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExplore,
                    modifier = Modifier.weight(1f).testTag("home_explore_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ESBlue),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Explore")
                }

                OutlinedButton(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f).testTag("home_analyze_btn"),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analyze")
                }

                FilledTonalButton(
                    onClick = onClean,
                    modifier = Modifier.weight(1f).testTag("home_clean_btn"),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = ESAccentGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clean")
                }
            }
        }
    }
}

@Composable
private fun RootSystemCard(
    rootVolume: StorageInfo?,
    isRootMode: Boolean,
    isRootGranted: Boolean,
    onBrowseRoot: () -> Unit,
    onToggleRootMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_root_system_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ESAccentOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = ESAccentOrange, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Device Root (/)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isRootMode) ESAccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isRootMode) "ROOT RW" else "SYSTEM RO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isRootMode) ESAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Inner Android system: /system, /data, /etc, /proc",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isRootMode,
                    onCheckedChange = { onToggleRootMode() },
                    modifier = Modifier.testTag("toggle_root_explorer_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRootMode) "Temporary elevated mode enabled for editing & deleting" else "Standard safe mode. Toggle switch to elevate permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onBrowseRoot,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("browse_inner_root_btn")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Browse /")
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    onCategoryClick: (FileCategory) -> Unit,
    onCleanClick: () -> Unit,
    onAppsClick: () -> Unit
) {
    val items = listOf(
        CategoryItem("Images", Icons.Default.Image, ESAccentOrange) { onCategoryClick(FileCategory.IMAGES) },
        CategoryItem("Music", Icons.Default.MusicNote, ESAccentPurple) { onCategoryClick(FileCategory.MUSIC) },
        CategoryItem("Movies", Icons.Default.Movie, ESAccentRed) { onCategoryClick(FileCategory.VIDEOS) },
        CategoryItem("Documents", Icons.Default.Description, ESDocBlue) { onCategoryClick(FileCategory.DOCUMENTS) },
        CategoryItem("Apps", Icons.Default.Android, ESApkGreen) { onAppsClick() },
        CategoryItem("Archives", Icons.Default.FolderZip, ESZipPurple) { onCategoryClick(FileCategory.ARCHIVES) },
        CategoryItem("Downloads", Icons.Default.Download, ESAccentCyan) { onCategoryClick(FileCategory.DOWNLOADS) },
        CategoryItem("Cleaner", Icons.Default.CleaningServices, ESAccentGreen) { onCleanClick() }
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (rowIndex in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (colIndex in 0 until 4) {
                    val item = items[rowIndex * 4 + colIndex]
                    CategoryButton(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cat_btn_${item.name.lowercase()}")
                    )
                }
            }
        }
    }
}

private data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

@Composable
private fun CategoryButton(
    item: CategoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = item.action),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.name,
                    tint = item.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun QuickToolsRow(
    isRootMode: Boolean,
    onToggleRoot: () -> Unit,
    onOpenTerminal: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ToolItem(
                icon = Icons.Default.PieChart,
                label = "Analyzer",
                color = ESAccentCyan,
                testTag = "quick_tool_analyzer",
                onClick = { onNavigate(Screen.STORAGE_ANALYZER) }
            )
            ToolItem(
                icon = Icons.Default.Apps,
                label = "Apps",
                color = ESAccentAmber,
                testTag = "quick_tool_apps",
                onClick = { onNavigate(Screen.APP_MANAGER) }
            )
            ToolItem(
                icon = Icons.Default.Terminal,
                label = "Terminal",
                color = ESAccentOrange,
                testTag = "quick_tool_terminal",
                onClick = onOpenTerminal
            )
            ToolItem(
                icon = Icons.Default.DeleteOutline,
                label = "Recycle",
                color = ESAccentRed,
                testTag = "quick_tool_recycle_bin",
                onClick = { onNavigate(Screen.RECYCLE_BIN) }
            )
            ToolItem(
                icon = Icons.Default.Security,
                label = if (isRootMode) "Root ON" else "Root",
                color = if (isRootMode) ESAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "quick_tool_root",
                onClick = onToggleRoot
            )
            ToolItem(
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
private fun ToolItem(
    icon: ImageVector,
    label: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecentFileCard(
    file: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(getFileColor(file).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getFileIcon(file),
                    contentDescription = null,
                    tint = getFileColor(file),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = file.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = file.formattedSize,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getFileIcon(file: FileItem): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    return when (file.category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.MUSIC -> Icons.Default.MusicNote
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.APKS -> Icons.Default.Android
        FileCategory.ARCHIVES -> Icons.Default.FolderZip
        else -> Icons.Default.InsertDriveFile
    }
}

fun getFileColor(file: FileItem): Color {
    if (file.isDirectory) return ESFolderYellow
    return when (file.category) {
        FileCategory.IMAGES -> ESAccentOrange
        FileCategory.MUSIC -> ESAccentPurple
        FileCategory.VIDEOS -> ESAccentRed
        FileCategory.DOCUMENTS -> ESDocBlue
        FileCategory.APKS -> ESApkGreen
        FileCategory.ARCHIVES -> ESZipPurple
        else -> Color(0xFF78909C)
    }
}
