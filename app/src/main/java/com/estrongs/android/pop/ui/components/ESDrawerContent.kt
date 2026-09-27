package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrongs.android.pop.data.model.FileCategory
import com.estrongs.android.pop.ui.theme.*
import com.estrongs.android.pop.ui.viewmodel.Screen

@Composable
fun ESDrawerContent(
    currentScreen: Screen,
    isRootMode: Boolean,
    onNavigate: (Screen) -> Unit,
    onOpenPath: (String) -> Unit,
    onToggleRoot: () -> Unit,
    onSelectCategory: (FileCategory) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ES Header Banner (Simple & lightweight)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ESBlue)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "ES Logo",
                                tint = ESBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ES File Explorer",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "v4.1.2.2 • Manager & Storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Navigation Items
            DrawerSectionHeader("Storage Locations")

            DrawerMenuItem(
                icon = Icons.Default.Home,
                label = "Home Dashboard",
                isSelected = currentScreen == Screen.HOME,
                tint = ESBlue,
                testTag = "drawer_nav_home",
                onClick = {
                    onNavigate(Screen.HOME)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.PhoneAndroid,
                label = "Internal Storage (0)",
                isSelected = currentScreen == Screen.EXPLORER,
                tint = ESBlueDark,
                testTag = "drawer_nav_internal",
                onClick = {
                    onOpenPath("/storage/emulated/0")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Security,
                label = "Device Root (/)",
                isSelected = false,
                tint = ESAccentOrange,
                testTag = "drawer_nav_root_system",
                onClick = {
                    onOpenPath("/")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.SettingsSuggest,
                label = "System Partition (/system)",
                isSelected = false,
                tint = ESAccentGreen,
                testTag = "drawer_nav_system",
                onClick = {
                    onOpenPath("/system")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Inventory2,
                label = "Data Partition (/data)",
                isSelected = false,
                tint = ESAccentPurple,
                testTag = "drawer_nav_data",
                onClick = {
                    onOpenPath("/data")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Memory,
                label = "Kernel Filesystem (/proc)",
                isSelected = false,
                tint = ESAccentRed,
                testTag = "drawer_nav_proc",
                onClick = {
                    onOpenPath("/proc")
                    onCloseDrawer()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            DrawerSectionHeader("Library")

            DrawerCategoryItem(Icons.Default.Image, "Images", ESAccentOrange) {
                onSelectCategory(FileCategory.IMAGES)
                onCloseDrawer()
            }
            DrawerCategoryItem(Icons.Default.MusicNote, "Music", ESAccentPurple) {
                onSelectCategory(FileCategory.MUSIC)
                onCloseDrawer()
            }
            DrawerCategoryItem(Icons.Default.Movie, "Movies & Videos", ESAccentRed) {
                onSelectCategory(FileCategory.VIDEOS)
                onCloseDrawer()
            }
            DrawerCategoryItem(Icons.Default.Description, "Documents", ESDocBlue) {
                onSelectCategory(FileCategory.DOCUMENTS)
                onCloseDrawer()
            }
            DrawerCategoryItem(Icons.Default.Android, "APKs", ESApkGreen) {
                onSelectCategory(FileCategory.APKS)
                onCloseDrawer()
            }
            DrawerCategoryItem(Icons.Default.FolderZip, "Archives", ESZipPurple) {
                onSelectCategory(FileCategory.ARCHIVES)
                onCloseDrawer()
            }
            DrawerCategoryItem(Icons.Default.Download, "Downloads", ESAccentCyan) {
                onSelectCategory(FileCategory.DOWNLOADS)
                onCloseDrawer()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            DrawerSectionHeader("Tools")

            // Classic ES Root Explorer switch directly in drawer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRoot() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isRootMode) ESAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Root Explorer",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isRootMode) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = if (isRootMode) "Elevated Mode (RW)" else "Standard Safe Mode",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isRootMode) ESAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isRootMode,
                    onCheckedChange = { onToggleRoot() },
                    modifier = Modifier.testTag("drawer_root_switch")
                )
            }

            DrawerMenuItem(
                icon = Icons.Default.PieChart,
                label = "Storage Analyzer",
                isSelected = currentScreen == Screen.STORAGE_ANALYZER,
                tint = ESAccentCyan,
                testTag = "drawer_nav_storage_analyzer",
                onClick = {
                    onNavigate(Screen.STORAGE_ANALYZER)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.CleaningServices,
                label = "Space Cleaner",
                isSelected = currentScreen == Screen.CLEANER,
                tint = ESAccentGreen,
                testTag = "drawer_nav_cleaner",
                onClick = {
                    onNavigate(Screen.CLEANER)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Apps,
                label = "App Manager",
                isSelected = currentScreen == Screen.APP_MANAGER,
                tint = ESAccentAmber,
                testTag = "drawer_nav_app_manager",
                onClick = {
                    onNavigate(Screen.APP_MANAGER)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.DeleteOutline,
                label = "Recycle Bin",
                isSelected = currentScreen == Screen.RECYCLE_BIN,
                tint = ESAccentRed,
                testTag = "drawer_nav_recycle_bin",
                onClick = {
                    onNavigate(Screen.RECYCLE_BIN)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentScreen == Screen.SETTINGS,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "drawer_nav_settings",
                onClick = {
                    onNavigate(Screen.SETTINGS)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    tint: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) },
        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .testTag(testTag)
    )
}

@Composable
private fun DrawerCategoryItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
