package com.estrongs.android.pop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrongs.android.pop.data.model.JunkItem
import com.estrongs.android.pop.data.model.JunkType
import com.estrongs.android.pop.data.model.StorageInfo
import com.estrongs.android.pop.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    junkItems: List<JunkItem>,
    isScanning: Boolean,
    isCleaning: Boolean,
    cleanedBytes: Long?,
    onToggleSelect: (String) -> Unit,
    onCleanNow: () -> Unit,
    onScanAgain: () -> Unit,
    onBack: () -> Unit
) {
    val selectedBytes = junkItems.filter { it.isSelected }.sumOf { it.totalBytes }
    val totalFoundBytes = junkItems.sumOf { it.totalBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ES Space Cleaner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("cleaner_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScanAgain, enabled = !isScanning && !isCleaning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Again")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ESBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero Status Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ESBlue)
                    .padding(bottom = 24.dp, top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isScanning || isCleaning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isScanning) "Scanning storage junk..." else "Cleaning selected files...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else if (cleanedBytes != null) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ESAccentGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cleaned ${StorageInfo.formatBytes(cleanedBytes)}!",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Storage space successfully reclaimed",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = StorageInfo.formatBytes(selectedBytes),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Safe to clean • ${junkItems.count { it.isSelected }} categories selected",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Cleaned Result Card or Category List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Junk Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(junkItems) { item ->
                    JunkCategoryRow(
                        item = item,
                        onToggle = { onToggleSelect(item.id) }
                    )
                }
            }

            // Bottom Clean Action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onCleanNow,
                        enabled = !isScanning && !isCleaning && selectedBytes > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("clean_now_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ESAccentGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedBytes > 0) "Clean Now (${StorageInfo.formatBytes(selectedBytes)})" else "Select Items to Clean",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JunkCategoryRow(
    item: JunkItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("junk_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("junk_checkbox_${item.id}")
            )

            Spacer(modifier = Modifier.width(10.dp))

            val icon = when (item.type) {
                JunkType.CACHE -> Icons.Default.Cached
                JunkType.TEMP -> Icons.Default.Description
                JunkType.EMPTY_DIRS -> Icons.Default.FolderOpen
                JunkType.LARGE_FILES -> Icons.Default.Storage
                JunkType.RESIDUAL_APKS -> Icons.Default.Android
            }

            val color = when (item.type) {
                JunkType.CACHE -> ESAccentGreen
                JunkType.TEMP -> ESAccentCyan
                JunkType.EMPTY_DIRS -> ESFolderYellow
                JunkType.LARGE_FILES -> ESAccentOrange
                JunkType.RESIDUAL_APKS -> ESApkGreen
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${item.filePaths.size} files found",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.formattedSize,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
