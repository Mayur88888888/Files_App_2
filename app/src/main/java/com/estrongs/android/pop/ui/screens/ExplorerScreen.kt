package com.estrongs.android.pop.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onPropertiesClick: (FileItem) -> Unit,
    onChmodClick: (FileItem) -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val displayFiles = if (searchQuery.isNotBlank()) searchResults else files

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Selection Action Bar or Standard Address Bar
        if (isSelectionMode) {
            SelectionActionBar(
                selectedCount = selectedFilePaths.size,
                totalCount = displayFiles.size,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onCopy = onCopyClick,
                onCut = onCutClick,
                onDelete = {
                    val selectedItems = displayFiles.filter { selectedFilePaths.contains(it.path) }
                    onDeleteClick(selectedItems)
                },
                onCompress = onCompressClick
            )
        } else {
            // Address & Navigation Bar
            AddressBreadcrumbBar(
                currentPath = currentPath,
                activeCategory = activeCategory,
                onNavigateToPath = onNavigateToPath,
                onNavigateUp = onNavigateUp,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                onToggleSearch = {
                    isSearchExpanded = !isSearchExpanded
                    if (!isSearchExpanded) onSearchQueryChange("")
                },
                onSearchQueryChange = onSearchQueryChange
            )
        }

        // Action Toolbar (View toggle, Sort, New, Refresh, Hidden Files)
        if (!isSelectionMode) {
            ExplorerToolbar(
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
                onCreateClick = onCreateClick
            )
        }

        // Active Clipboard Notification Bar
        if (clipboard != null && clipboard.filePaths.isNotEmpty()) {
            ClipboardBar(
                clipboard = clipboard,
                onPaste = onPasteClick,
                onClear = onClearClipboard
            )
        }

        // Permission Warning Banner
        if (!hasStoragePermission) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestPermission() },
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Storage access restricted. Tap to grant permission.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "GRANT",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // File Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (displayFiles.isEmpty()) {
                EmptyFolderView(
                    isSearch = searchQuery.isNotBlank(),
                    hasStoragePermission = hasStoragePermission,
                    onRequestPermission = onRequestPermission,
                    onNavigateToPath = onNavigateToPath
                )
            } else if (viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("explorer_grid")
                ) {
                    items(displayFiles, key = { it.path }) { item ->
                        FileGridCard(
                            item = item,
                            isSelected = selectedFilePaths.contains(item.path),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelectFile(item.path)
                                } else {
                                    onOpenFile(item)
                                }
                            },
                            onLongClick = { onToggleSelectFile(item.path) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize().testTag("explorer_list")
                ) {
                    items(displayFiles, key = { it.path }) { item ->
                        FileListRow(
                            item = item,
                            isSelected = selectedFilePaths.contains(item.path),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelectFile(item.path)
                                } else {
                                    onOpenFile(item)
                                }
                            },
                            onLongClick = { onToggleSelectFile(item.path) },
                            onRename = { onRenameClick(item) },
                            onDelete = { onDeleteClick(listOf(item)) },
                            onProperties = { onPropertiesClick(item) },
                            onChmod = { onChmodClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressBreadcrumbBar(
    currentPath: String,
    activeCategory: FileCategory?,
    onNavigateToPath: (String) -> Unit,
    onNavigateUp: () -> Unit,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ESBlue,
        shadowElevation = 3.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.testTag("nav_up_button")
                ) {
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_text_input")
                    )
                } else {
                    // Breadcrumbs
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
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.testTag("nav_search_toggle")
                ) {
                    Icon(
                        if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }

            // Quick system partition & storage jump chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ESBlueDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRootSelected = currentPath == "/"
                val isZeroSelected = currentPath == "/storage/emulated/0" || currentPath == "/sdcard"
                val isSysSelected = currentPath == "/system"
                val isDataSelected = currentPath == "/data"
                val isEtcSelected = currentPath == "/etc"
                val isProcSelected = currentPath == "/proc"
                val isMntSelected = currentPath == "/mnt"

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRootSelected) ESAccentOrange else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/") }
                            .testTag("quick_jump_root")
                    ) {
                        Text(
                            text = "⚡ Root (/)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isZeroSelected) ESAccentCyan else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/storage/emulated/0") }
                            .testTag("quick_jump_internal_0")
                    ) {
                        Text(
                            text = "📱 0 (Internal)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSysSelected) ESAccentGreen else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/system") }
                            .testTag("quick_jump_system")
                    ) {
                        Text(
                            text = "⚙️ /system",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDataSelected) ESAccentPurple else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/data") }
                            .testTag("quick_jump_data")
                    ) {
                        Text(
                            text = "📦 /data",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEtcSelected) ESDocBlue else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/etc") }
                            .testTag("quick_jump_etc")
                    ) {
                        Text(
                            text = "📋 /etc",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isProcSelected) ESAccentRed else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/proc") }
                            .testTag("quick_jump_proc")
                    ) {
                        Text(
                            text = "🧠 /proc",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMntSelected) ESZipPurple else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPath("/mnt") }
                            .testTag("quick_jump_mnt")
                    ) {
                        Text(
                            text = "💾 /mnt",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerToolbar(
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
    onCreateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hidden Files Toggle
                IconButton(
                    onClick = onToggleShowHidden,
                    modifier = Modifier.testTag("toggle_hidden_files_btn")
                ) {
                    Icon(
                        imageVector = if (showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showHiddenFiles) "Hidden files shown" else "Hidden files hidden",
                        tint = if (showHiddenFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // View Mode Toggle
                IconButton(
                    onClick = onToggleViewMode,
                    modifier = Modifier.testTag("toggle_view_mode_btn")
                ) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sort Menu
                Box {
                    IconButton(
                        onClick = { onToggleSortMenu(true) },
                        modifier = Modifier.testTag("sort_menu_btn")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { onToggleSortMenu(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Name (A to Z)") },
                            onClick = { onSetSortOption(SortOption.NAME_ASC); onToggleSortMenu(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z to A)") },
                            onClick = { onSetSortOption(SortOption.NAME_DESC); onToggleSortMenu(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Date (Newest first)") },
                            onClick = { onSetSortOption(SortOption.DATE_DESC); onToggleSortMenu(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Date (Oldest first)") },
                            onClick = { onSetSortOption(SortOption.DATE_ASC); onToggleSortMenu(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Largest first)") },
                            onClick = { onSetSortOption(SortOption.SIZE_DESC); onToggleSortMenu(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Smallest first)") },
                            onClick = { onSetSortOption(SortOption.SIZE_ASC); onToggleSortMenu(false) }
                        )
                    }
                }

                // Create File/Folder
                IconButton(
                    onClick = onCreateClick,
                    modifier = Modifier.testTag("create_new_btn")
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Create New", tint = MaterialTheme.colorScheme.primary)
                }

                // Refresh
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_files_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ESBlueDark,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.testTag("selection_clear_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$selectedCount selected",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(
                    onClick = onSelectAll,
                    modifier = Modifier.testTag("select_all_btn")
                ) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White)
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.testTag("selection_copy_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                }
                IconButton(
                    onClick = onCut,
                    modifier = Modifier.testTag("selection_cut_btn")
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Cut", tint = Color.White)
                }
                IconButton(
                    onClick = onCompress,
                    modifier = Modifier.testTag("selection_compress_btn")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = "Compress", tint = Color.White)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("selection_delete_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ClipboardBar(
    clipboard: ClipboardState,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ESAccentCyan.copy(alpha = 0.15f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (clipboard.isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = ESAccentCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${clipboard.filePaths.size} item(s) in clipboard",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("clipboard_clear_btn")
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = onPaste,
                    modifier = Modifier.testTag("clipboard_paste_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ESAccentCyan)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste Here")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridCard(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileColor = getFileColor(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_grid_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ESBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(fileColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getFileIcon(item),
                        contentDescription = item.name,
                        tint = fileColor,
                        modifier = Modifier.size(32.dp)
                    )
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onProperties: () -> Unit,
    onChmod: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val fileColor = getFileColor(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_list_item_${item.name}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ESBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
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
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(fileColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getFileIcon(item),
                    contentDescription = null,
                    tint = fileColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.formattedDate.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.formattedDate,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        text = { Text("Rename") },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                        text = { Text("Permissions (chmod)") },
                        onClick = { showMenu = false; onChmod() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        text = { Text("Properties") },
                        onClick = { showMenu = false; onProperties() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFolderView(
    isSearch: Boolean,
    hasStoragePermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onNavigateToPath: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (!hasStoragePermission) Icons.Default.Lock else if (isSearch) Icons.Default.SearchOff else Icons.Default.FolderOpen,
            contentDescription = null,
            tint = if (!hasStoragePermission) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (!hasStoragePermission) "Storage Access Restricted" else if (isSearch) "No matching files found" else "This folder is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!hasStoragePermission) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android requires 'All files access' permission to read and manage this folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.testTag("empty_view_grant_perm_btn")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grant File Access")
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigateToPath("/") }) {
                    Text("Explore Root (/)")
                }
                OutlinedButton(onClick = { onNavigateToPath("/storage/emulated/0") }) {
                    Text("Internal Storage")
                }
            }
        }
    }
}
