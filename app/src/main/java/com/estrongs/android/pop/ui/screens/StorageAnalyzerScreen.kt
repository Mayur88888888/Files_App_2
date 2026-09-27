package com.estrongs.android.pop.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrongs.android.pop.data.model.*
import com.estrongs.android.pop.ui.theme.*

private enum class AnalyzerTab(val label: String) {
    CATEGORIES("Categories"),
    LARGEST("Largest Files"),
    DUPLICATES("Duplicates"),
    EMPTY_DIRS("Empty Folders"),
    PARTITIONS("Partitions & Mounts")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StorageAnalyzerScreen(
    analysisResult: StorageAnalysisResult?,
    isAnalyzing: Boolean,
    onCategoryClick: (FileCategory) -> Unit,
    onRescan: () -> Unit,
    onDeleteFile: (String) -> Unit,
    onCleanDuplicateGroup: (DuplicateGroup) -> Unit,
    onDeleteAllEmptyFolders: () -> Unit,
    onExplorePartition: (String) -> Unit = {},
    onRemountPartition: (partition: String, rw: Boolean) -> Unit = { _, _ -> },
    onOpenFile: (FileItem) -> Unit = {},
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AnalyzerTab.CATEGORIES) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analyzer_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRescan,
                        enabled = !isAnalyzing,
                        modifier = Modifier.testTag("analyzer_rescan_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan")
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
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ESBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing storage structure...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (analysisResult != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Graphical Storage Distribution Card
                item {
                    StorageDistributionCard(result = analysisResult)
                }

                // Sub-tabs
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        AnalyzerTab.values().forEach { tab ->
                            val badgeCount = when (tab) {
                                AnalyzerTab.CATEGORIES -> analysisResult.categories.size
                                AnalyzerTab.LARGEST -> analysisResult.largestFiles.size
                                AnalyzerTab.DUPLICATES -> analysisResult.duplicateGroups.size
                                AnalyzerTab.EMPTY_DIRS -> analysisResult.emptyFolders.size
                                AnalyzerTab.PARTITIONS -> analysisResult.partitions.size
                            }
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text("${tab.label} ($badgeCount)") },
                                modifier = Modifier.testTag("analyzer_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    AnalyzerTab.CATEGORIES -> {
                        items(analysisResult.categories) { cat ->
                            CategoryAnalysisRow(cat = cat, onClick = { onCategoryClick(cat.category) })
                        }
                    }
                    AnalyzerTab.LARGEST -> {
                        if (analysisResult.largestFiles.isEmpty()) {
                            item {
                                EmptyStateCard("No large files found (>10MB)")
                            }
                        } else {
                            items(analysisResult.largestFiles) { file ->
                                LargestFileRow(file = file, onDelete = { onDeleteFile(file.path) })
                            }
                        }
                    }
                    AnalyzerTab.DUPLICATES -> {
                        if (analysisResult.duplicateGroups.isEmpty()) {
                            item {
                                EmptyStateCard("No duplicate files detected")
                            }
                        } else {
                            items(analysisResult.duplicateGroups) { group ->
                                DuplicateGroupCard(group = group, onClean = { onCleanDuplicateGroup(group) })
                            }
                        }
                    }
                    AnalyzerTab.EMPTY_DIRS -> {
                        if (analysisResult.emptyFolders.isEmpty()) {
                            item {
                                EmptyStateCard("No empty folders found")
                            }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${analysisResult.emptyFolders.size} empty folders found",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(
                                        onClick = onDeleteAllEmptyFolders,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("delete_all_empty_folders_btn")
                                    ) {
                                        Text("Delete All")
                                    }
                                }
                            }
                            items(analysisResult.emptyFolders) { dir ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ESFolderYellow)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dir.name, fontWeight = FontWeight.Medium)
                                            Text(dir.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { onDeleteFile(dir.path) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    AnalyzerTab.PARTITIONS -> {
                        if (analysisResult.partitions.isEmpty()) {
                            item {
                                EmptyStateCard("No system partitions detected")
                            }
                        } else {
                            items(analysisResult.partitions) { part ->
                                PartitionAnalysisCard(
                                    part = part,
                                    onExplore = { onExplorePartition(part.mountPoint) },
                                    onRemount = { rw -> onRemountPartition(part.mountPoint, rw) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageDistributionCard(result: StorageAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Disk Usage Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${result.formattedUsed} used of ${result.formattedTotal} (${result.formattedFree} free)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${(result.usedPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ESBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment horizontal distribution bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                val nonZero = result.categories.filter { it.percent > 0.01f }
                if (nonZero.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ESBlue))
                } else {
                    for (cat in nonZero) {
                        Box(
                            modifier = Modifier
                                .weight(cat.percent)
                                .fillMaxHeight()
                                .background(Color(cat.colorHex))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend dots
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (cat in result.categories) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${cat.name}: ${cat.formattedSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryAnalysisRow(
    cat: CategoryBreakdown,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("analyzer_cat_row_${cat.name.lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(cat.colorHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${cat.fileCount} files)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cat.formattedSize,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { cat.percent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(cat.colorHex),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun LargestFileRow(
    file: FileItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ESAccentOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(getFileIcon(file), contentDescription = null, tint = ESAccentOrange, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = file.formattedSize,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ESAccentRed
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onClean: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.files.size} identical files • ${group.formattedSize} each • Wasting ${group.redundantBytes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = onClean,
                    colors = ButtonDefaults.buttonColors(containerColor = ESBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Clean Duplicate", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            for (f in group.files) {
                Text(
                    text = "• ${f.path}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PartitionAnalysisCard(
    part: PartitionInfo,
    onExplore: () -> Unit,
    onRemount: (rw: Boolean) -> Unit
) {
    val isReadOnly = part.mountOptions.contains("ro") && !part.mountOptions.contains("rw")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (part.mountPoint == "/") ESAccentOrange else ESBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = part.mountPoint,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isReadOnly) MaterialTheme.colorScheme.errorContainer else ESAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isReadOnly) "RO" else "RW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isReadOnly) MaterialTheme.colorScheme.error else ESAccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${part.deviceName} • ${part.fsType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onExplore,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Browse", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onRemount(isReadOnly) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isReadOnly) "Make RW" else "Make RO", fontSize = 11.sp)
                    }
                }
            }

            if (part.totalBytes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used: ${part.formattedUsed} / ${part.formattedTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(part.usedPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (part.usedPercent > 0.9f) MaterialTheme.colorScheme.error else ESBlue
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { part.usedPercent },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (part.usedPercent > 0.9f) MaterialTheme.colorScheme.error else ESBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

