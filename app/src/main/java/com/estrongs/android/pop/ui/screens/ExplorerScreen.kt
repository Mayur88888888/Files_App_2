package com.estrongs.android.pop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.estrongs.android.pop.data.model.FileCategory
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.*
import com.estrongs.android.pop.ui.viewmodel.ClipboardState
import com.estrongs.android.pop.ui.viewmodel.SortOption
import com.estrongs.android.pop.ui.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    currentPath: String,
    activeCategory: FileCategory?,
    files: List<FileItem>,
    selectedFilePaths: Set<String>,
    isSelectionMode: Boolean,
    viewMode: ViewMode,
    sortOption: SortOption,
    searchQuery: String,
    searchResults: List<FileItem>,
    clipboard: ClipboardState?,
    showHiddenFiles: Boolean = false,
    hasStoragePermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onNavigateToPath: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onGoHome: () -> Unit = {},
    onOpenFile: (FileItem) -> Unit,
    onToggleSelectFile: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleShowHidden: () -> Unit = {},
    onSetSortOption: (SortOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateClick: () -> Unit,
    onCopyClick: () -> Unit,
    onCutClick: () -> Unit,
    onPasteClick: () -> Unit,
    onClearClipboard: () -> Unit,
    onDeleteClick: (List<FileItem>) -> Unit,
    onCompressClick: () -> Unit,
    onRenameClick: (FileItem) -> Unit,
    onBatchRenameClick: (List<FileItem>) -> Unit = {},
    onChecksumClick: (FileItem) -> Unit = {},
    onEncryptClick: (FileItem) -> Unit = {},
    onNewNoteClick: () -> Unit = {},
    onPropertiesClick: (FileItem) -> Unit,
    onChmodClick: (FileItem) -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val displayFiles = if (searchQuery.isNotBlank()) searchResults else files
    val selectedItems = remember(selectedFilePaths, displayFiles) {
        displayFiles.filter { selectedFilePaths.contains(it.path) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simplified Modern Address & Breadcrumbs Bar
            CleanAddressBar(
                currentPath = currentPath,
                activeCategory = activeCategory,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                onNavigateToPath = onNavigateToPath,
                onNavigateUp = onNavigateUp,
                onGoHome = onGoHome,
                onToggleSearch = {
                    isSearchExpanded = !isSearchExpanded
                    if (!isSearchExpanded) onSearchQueryChange("")
                },
                onSearchQueryChange = onSearchQueryChange
            )

            // Simplified Sub-Toolbar
            CleanToolbar(
                itemCount = displayFiles.size,
                viewMode = viewMode,
                sortOption = sortOption,
                showHiddenFiles = showHiddenFiles,
                showSortMenu = showSortMenu,
                onToggleSortMenu = { showSortMenu = it },
                onToggleViewMode = onToggleViewMode,
                onToggleShowHidden = onToggleShowHidden,
                onSetSortOption = onSetSortOption,
                onRefresh = onRefresh,
                onCreateClick = onCreateClick,
                onNewNoteClick = onNewNoteClick
            )

            // Active Clipboard Bar
            if (clipboard != null && clipboard.filePaths.isNotEmpty()) {
                CleanClipboardBar(
                    clipboard = clipboard,
                    onPaste = onPasteClick,
                    onClear = onClearClipboard
                )
            }

            // File Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (displayFiles.isEmpty()) {
                    CleanEmptyView(
                        isSearch = searchQuery.isNotBlank(),
                        hasStoragePermission = hasStoragePermission,
                        onRequestPermission = onRequestPermission
                    )
                } else if (viewMode == ViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().testTag("explorer_grid")
                    ) {
                        items(displayFiles, key = { it.path }) { item ->
                            CleanFileGridCard(
                                item = item,
                                isSelected = selectedFilePaths.contains(item.path),
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) onToggleSelectFile(item.path)
                                    else onOpenFile(item)
                                },
                                onLongClick = { onToggleSelectFile(item.path) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize().testTag("explorer_list")
                    ) {
                        items(displayFiles, key = { it.path }) { item ->
                            CleanFileListRow(
                                item = item,
                                isSelected = selectedFilePaths.contains(item.path),
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) onToggleSelectFile(item.path)
                                    else onOpenFile(item)
                                },
                                onLongClick = { onToggleSelectFile(item.path) },
                                onRename = { onRenameClick(item) },
                                onDelete = { onDeleteClick(listOf(item)) },
                                onProperties = { onPropertiesClick(item) },
                                onChecksum = { onChecksumClick(item) },
                                onEncrypt = { onEncryptClick(item) },
                                onBatchRename = { onBatchRenameClick(listOf(item)) },
                                onChmod = { onChmodClick(item) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Selection Bar when files are selected
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ESBlueDark,
                shadowElevation = 8.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onClearSelection, modifier = Modifier.size(36.dp).testTag("selection_clear_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = "${selectedFilePaths.size} selected",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.3f))

                    IconButton(onClick = onSelectAll, modifier = Modifier.size(36.dp).testTag("select_all_btn")) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onBatchRenameClick(selectedItems) }, modifier = Modifier.size(36.dp).testTag("selection_batch_rename_btn")) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Batch Rename", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(36.dp).testTag("selection_copy_btn")) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCutClick, modifier = Modifier.size(36.dp).testTag("selection_cut_btn")) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Cut", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCompressClick, modifier = Modifier.size(36.dp).testTag("selection_compress_btn")) {
                        Icon(Icons.Default.FolderZip, contentDescription = "Compress", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDeleteClick(selectedItems) }, modifier = Modifier.size(36.dp).testTag("selection_delete_btn")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanAddressBar(
    currentPath: String,
    activeCategory: FileCategory?,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onNavigateToPath: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onGoHome: () -> Unit = {},
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ESBlue,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onGoHome, modifier = Modifier.testTag("nav_home_button")) {
                    Icon(Icons.Default.Home, contentDescription = "Instant Home", tint = Color.White)
                }

                IconButton(onClick = onNavigateUp, modifier = Modifier.testTag("nav_up_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Up Directory", tint = Color.White)
                }

                if (isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search files...", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.6f)
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("search_text_input")
                    )
                } else {
                    val segments = remember(currentPath, activeCategory) {
                        if (activeCategory != null) {
                            listOf("Library" to "", activeCategory.label to "")
                        } else {
                            val parts = currentPath.split("/").filter { it.isNotEmpty() }
                            val list = mutableListOf<Pair<String, String>>()
                            list.add("Root" to "/")
                            var accum = ""
                            for (p in parts) {
                                accum += "/$p"
                                list.add(p to accum)
                            }
                            list
                        }
                    }

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(segments) { (name, path) ->
                            Text(
                                text = name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { if (path.isNotEmpty()) onNavigateToPath(path) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onToggleSearch, modifier = Modifier.testTag("nav_search_toggle")) {
                    Icon(
                        if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }

            // Quick Jump Chips (Root, 0, Android/data Shizuku bypass, System)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    QuickChip("⚡ Root (/)", currentPath == "/") { onNavigateToPath("/") }
                }
                item {
                    QuickChip("📱 0 (/sdcard)", currentPath.startsWith("/storage/emulated/0")) { onNavigateToPath("/storage/emulated/0") }
                }
                item {
                    QuickChip("⚡ /Android/data", currentPath.contains("/Android/data")) { onNavigateToPath("/storage/emulated/0/Android/data") }
                }
                item {
                    QuickChip("⚙️ /system", currentPath.startsWith("/system")) { onNavigateToPath("/system") }
                }
                item {
                    QuickChip("📦 /data", currentPath.startsWith("/data")) { onNavigateToPath("/data") }
                }
                item {
                    QuickChip("📋 /etc", currentPath.startsWith("/etc")) { onNavigateToPath("/etc") }
                }
                item {
                    QuickChip("💾 /mnt", currentPath.startsWith("/mnt")) { onNavigateToPath("/mnt") }
                }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CleanToolbar(
    itemCount: Int,
    viewMode: ViewMode,
    sortOption: SortOption,
    showHiddenFiles: Boolean,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleShowHidden: () -> Unit,
    onSetSortOption: (SortOption) -> Unit,
    onRefresh: () -> Unit,
    onCreateClick: () -> Unit,
    onNewNoteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNewNoteClick, modifier = Modifier.size(36.dp).testTag("toolbar_new_note_btn")) {
                    Icon(Icons.Default.EditNote, contentDescription = "New Note", tint = ESDocBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleShowHidden, modifier = Modifier.size(36.dp).testTag("toggle_hidden_files_btn")) {
                    Icon(
                        if (showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Hidden files",
                        tint = if (showHiddenFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onToggleViewMode, modifier = Modifier.size(36.dp).testTag("toggle_view_mode_btn")) {
                    Icon(
                        if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(onClick = { onToggleSortMenu(true) }, modifier = Modifier.size(36.dp).testTag("sort_menu_btn")) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { onToggleSortMenu(false) }) {
                        DropdownMenuItem(text = { Text("Name (A to Z)") }, onClick = { onSetSortOption(SortOption.NAME_ASC); onToggleSortMenu(false) })
                        DropdownMenuItem(text = { Text("Name (Z to A)") }, onClick = { onSetSortOption(SortOption.NAME_DESC); onToggleSortMenu(false) })
                        DropdownMenuItem(text = { Text("Date (Newest first)") }, onClick = { onSetSortOption(SortOption.DATE_DESC); onToggleSortMenu(false) })
                        DropdownMenuItem(text = { Text("Date (Oldest first)") }, onClick = { onSetSortOption(SortOption.DATE_ASC); onToggleSortMenu(false) })
                        DropdownMenuItem(text = { Text("Size (Largest first)") }, onClick = { onSetSortOption(SortOption.SIZE_DESC); onToggleSortMenu(false) })
                        DropdownMenuItem(text = { Text("Size (Smallest first)") }, onClick = { onSetSortOption(SortOption.SIZE_ASC); onToggleSortMenu(false) })
                    }
                }

                IconButton(onClick = onCreateClick, modifier = Modifier.size(36.dp).testTag("create_new_btn")) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Create New", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp).testTag("refresh_files_btn")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CleanClipboardBar(
    clipboard: ClipboardState,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${clipboard.filePaths.size} item(s) in clipboard (${if (clipboard.isCut) "Cut" else "Copy"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
            Row {
                TextButton(onClick = onClear) { Text("Clear", fontSize = 12.sp) }
                Button(onClick = onPaste, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Paste Here", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanFileListRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onProperties: () -> Unit,
    onChecksum: () -> Unit = {},
    onEncrypt: () -> Unit = {},
    onBatchRename: () -> Unit = {},
    onChmod: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val fileColor = getFileColorClean(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("file_list_item_${item.name}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ESBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() }, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }

            val isPreviewable = !item.isDirectory && (
                item.category == FileCategory.IMAGES ||
                item.category == FileCategory.VIDEOS ||
                item.name.endsWith(".png", true) ||
                item.name.endsWith(".jpg", true) ||
                item.name.endsWith(".jpeg", true) ||
                item.name.endsWith(".webp", true) ||
                item.name.endsWith(".gif", true) ||
                item.name.endsWith(".mp4", true) ||
                item.name.endsWith(".mkv", true)
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fileColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewable && java.io.File(item.path).exists()) {
                    AsyncImage(
                        model = java.io.File(item.path),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(getFileIconClean(item), contentDescription = null, tint = fileColor, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.formattedDate.isNotEmpty()) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.formattedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }, text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = ESBlue) }, text = { Text("Batch Rename...") }, onClick = { showMenu = false; onBatchRename() })
                    if (!item.isDirectory) {
                        DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ESBlue) }, text = { Text("Checksum / Hash") }, onClick = { showMenu = false; onChecksum() })
                        DropdownMenuItem(
                            leadingIcon = { Icon(if (item.name.endsWith(".eslock")) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, tint = ESAccentGreen) },
                            text = { Text(if (item.name.endsWith(".eslock")) "Decrypt Vault (.eslock)" else "Encrypt File (.eslock)") },
                            onClick = { showMenu = false; onEncrypt() }
                        )
                    }
                    DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }, text = { Text("Permissions (chmod)") }, onClick = { showMenu = false; onChmod() })
                    DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }, text = { Text("Properties") }, onClick = { showMenu = false; onProperties() })
                    DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }, text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun CleanFileGridCard(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileColor = getFileColorClean(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("file_grid_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ESBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isPreviewable = !item.isDirectory && (
                item.category == FileCategory.IMAGES ||
                item.category == FileCategory.VIDEOS ||
                item.name.endsWith(".png", true) ||
                item.name.endsWith(".jpg", true) ||
                item.name.endsWith(".jpeg", true) ||
                item.name.endsWith(".webp", true) ||
                item.name.endsWith(".gif", true) ||
                item.name.endsWith(".mp4", true) ||
                item.name.endsWith(".mkv", true)
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fileColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewable && java.io.File(item.path).exists()) {
                    AsyncImage(
                        model = java.io.File(item.path),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(getFileIconClean(item), contentDescription = item.name, tint = fileColor, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.formattedSize,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CleanEmptyView(
    isSearch: Boolean,
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearch) Icons.Default.SearchOff else Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isSearch) "No matching files found" else "Folder is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getFileIconClean(item: FileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    return when (item.category) {
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

private fun getFileColorClean(item: FileItem): Color {
    if (item.isDirectory) return ESFolderYellow
    return when (item.category) {
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
