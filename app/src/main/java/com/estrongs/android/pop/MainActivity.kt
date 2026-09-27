package com.estrongs.android.pop

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.estrongs.android.pop.ui.components.*
import com.estrongs.android.pop.ui.screens.*
import com.estrongs.android.pop.ui.theme.ESAccentGreen
import com.estrongs.android.pop.ui.theme.ESAccentOrange
import com.estrongs.android.pop.ui.theme.ESBlue
import com.estrongs.android.pop.ui.theme.ESFileExplorerTheme
import com.estrongs.android.pop.ui.viewmodel.ExplorerViewModel
import com.estrongs.android.pop.ui.viewmodel.Screen
import com.estrongs.android.pop.util.StoragePermissionHelper
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ESFileExplorerTheme {
                ESFileExplorerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESFileExplorerApp(
    viewModel: ExplorerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission launcher for standard runtime permissions (READ/WRITE external storage, media)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StoragePermissionHelper.hasAllFilesAccess(context)) {
            viewModel.onStoragePermissionGranted()
        }
    }

    // Permission launcher for MANAGE_APP_ALL_FILES_ACCESS_PERMISSION (Android 11+)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (StoragePermissionHelper.hasAllFilesAccess(context)) {
            viewModel.onStoragePermissionGranted()
        }
    }

    val requestStoragePermissions: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                manageStorageLauncher.launch(StoragePermissionHelper.getAllFilesAccessIntent(context))
            } catch (_: Exception) {
                permissionLauncher.launch(StoragePermissionHelper.getRequiredRuntimePermissions())
            }
        } else {
            permissionLauncher.launch(StoragePermissionHelper.getRequiredRuntimePermissions())
        }
    }

    // Auto-detect when returning to app from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = StoragePermissionHelper.hasAllFilesAccess(context)
                if (hasPerm && !uiState.hasStoragePermission) {
                    viewModel.onStoragePermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Prompt for permission automatically on initial startup if not granted
    LaunchedEffect(Unit) {
        if (!StoragePermissionHelper.hasAllFilesAccess(context)) {
            requestStoragePermissions()
        }
    }

    // Listen to snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Android Hardware/Gesture Back Handler
    BackHandler {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            val handled = viewModel.navigateBack()
            if (!handled) {
                // Exit app if handled is false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.currentScreen == Screen.HOME,
        drawerContent = {
            ESDrawerContent(
                currentScreen = uiState.currentScreen,
                isRootMode = uiState.isRootMode,
                onNavigate = { screen ->
                    viewModel.navigateTo(screen)
                },
                onOpenPath = { path ->
                    viewModel.openDirectory(path)
                },
                onToggleRoot = {
                    viewModel.toggleRootMode()
                },
                onSelectCategory = { category ->
                    viewModel.openCategory(category)
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (uiState.currentScreen == Screen.HOME) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "ES File Explorer",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("home_drawer_toggle_btn")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Navigation Menu")
                            }
                        },
                        actions = {
                            // Terminal Quick Action
                            IconButton(
                                onClick = { viewModel.showTerminalDialog() },
                                modifier = Modifier.testTag("home_terminal_btn")
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = "Inner Terminal",
                                    tint = ESAccentOrange
                                )
                            }
                            // Root Indicator / Toggle
                            IconButton(
                                onClick = { viewModel.showRootSettingsDialog() },
                                modifier = Modifier.testTag("home_root_settings_btn")
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Root Settings",
                                    tint = if (uiState.isRootMode) ESAccentGreen else Color.White
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.EXPLORER) },
                                modifier = Modifier.testTag("home_top_search_btn")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search Files")
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.CLEANER) },
                                modifier = Modifier.testTag("home_top_cleaner_btn")
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "Space Cleaner")
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
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (uiState.currentScreen == Screen.HOME) padding else androidx.compose.foundation.layout.PaddingValues())
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (uiState.currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                storageVolumes = uiState.storageVolumes,
                                recentFiles = uiState.recentFiles,
                                isRootMode = uiState.isRootMode,
                                isRootGranted = uiState.isRootGranted,
                                shizukuStatus = uiState.shizukuStatus,
                                hasStoragePermission = uiState.hasStoragePermission,
                                onRequestPermission = requestStoragePermissions,
                                onRequestShizuku = { viewModel.requestShizukuPermission() },
                                onOpenTerminal = { viewModel.showTerminalDialog() },
                                onOpenNewNote = { viewModel.openNewBlankNote() },
                                onExploreStorage = { path -> viewModel.openDirectory(path) },
                                onCategoryClick = { cat -> viewModel.openCategory(cat) },
                                onToggleRootMode = { viewModel.toggleRootMode() },
                                onNavigate = { screen -> viewModel.navigateTo(screen) },
                                onOpenFile = { file -> viewModel.openFile(file) }
                            )
                        }
                        Screen.EXPLORER -> {
                            ExplorerScreen(
                                currentPath = uiState.currentPath,
                                activeCategory = uiState.activeCategory,
                                files = uiState.files,
                                selectedFilePaths = uiState.selectedFilePaths,
                                isSelectionMode = uiState.isSelectionMode,
                                viewMode = uiState.viewMode,
                                sortOption = uiState.sortOption,
                                searchQuery = uiState.searchQuery,
                                searchResults = uiState.searchResults,
                                clipboard = uiState.clipboard,
                                showHiddenFiles = uiState.showHiddenFiles,
                                hasStoragePermission = uiState.hasStoragePermission,
                                onRequestPermission = requestStoragePermissions,
                                onNavigateToPath = { path -> viewModel.openDirectory(path) },
                                onNavigateUp = { viewModel.navigateUp() },
                                onGoHome = { viewModel.navigateTo(Screen.HOME) },
                                onOpenFile = { file -> viewModel.openFile(file) },
                                onToggleSelectFile = { path -> viewModel.toggleSelectFile(path) },
                                onSelectAll = { viewModel.selectAllFiles() },
                                onClearSelection = { viewModel.clearSelection() },
                                onToggleViewMode = { viewModel.toggleViewMode() },
                                onToggleShowHidden = { viewModel.toggleHiddenFiles() },
                                onSetSortOption = { sort -> viewModel.setSortOption(sort) },
                                onSearchQueryChange = { q -> viewModel.search(q) },
                                onRefresh = { viewModel.refreshFiles() },
                                onCreateClick = { viewModel.showCreateDialog() },
                                onCopyClick = { viewModel.copySelected() },
                                onCutClick = { viewModel.cutSelected() },
                                onPasteClick = { viewModel.pasteClipboard() },
                                onClearClipboard = { viewModel.clearClipboard() },
                                onDeleteClick = { list -> viewModel.requestDeleteFiles(list) },
                                onCompressClick = { viewModel.showZipDialog() },
                                onRenameClick = { item -> viewModel.showRenameDialog(item) },
                                onBatchRenameClick = { list -> viewModel.openBatchRename(list) },
                                onChecksumClick = { item -> viewModel.openChecksum(item) },
                                onEncryptClick = { item ->
                                    if (item.name.endsWith(".eslock")) viewModel.openDecrypt(item)
                                    else viewModel.openEncrypt(item)
                                },
                                onNewNoteClick = { viewModel.openNewBlankNote() },
                                onPropertiesClick = { item -> viewModel.showProperties(item) },
                                onChmodClick = { item -> viewModel.showChmodDialog(item) }
                            )
                        }
                        Screen.CLEANER -> {
                            CleanerScreen(
                                junkItems = uiState.junkItems,
                                isScanning = uiState.isScanningJunk,
                                isCleaning = uiState.isCleaningJunk,
                                cleanedBytes = uiState.cleanedBytesResult,
                                onToggleSelect = { id -> viewModel.toggleJunkSelection(id) },
                                onCleanNow = { viewModel.cleanSelectedJunk() },
                                onScanAgain = { viewModel.startJunkScan() },
                                onBack = { viewModel.navigateTo(Screen.HOME) }
                            )
                        }
                        Screen.APP_MANAGER -> {
                            AppManagerScreen(
                                apps = uiState.installedApps,
                                isLoading = uiState.isLoadingApps,
                                includeSystemApps = uiState.includeSystemApps,
                                onToggleSystemApps = { viewModel.toggleIncludeSystemApps() },
                                onLaunchApp = { pkg -> viewModel.launchApp(pkg) },
                                onOpenAppDetails = { pkg -> viewModel.openAppDetails(pkg) },
                                onBackupApp = { app -> viewModel.backupApp(app) },
                                onRefresh = { viewModel.loadInstalledApps() },
                                onBack = { viewModel.navigateTo(Screen.HOME) }
                            )
                        }
                        Screen.RECYCLE_BIN -> {
                            RecycleBinScreen(
                                items = uiState.recycleBinItems,
                                onRestore = { item -> viewModel.restoreRecycleBinItem(item) },
                                onEmptyBin = { viewModel.emptyRecycleBin() },
                                onBack = { viewModel.navigateTo(Screen.HOME) }
                            )
                        }
                        Screen.STORAGE_ANALYZER -> {
                            StorageAnalyzerScreen(
                                analysisResult = uiState.storageAnalysis,
                                isAnalyzing = uiState.isAnalyzing,
                                onCategoryClick = { cat -> viewModel.openCategory(cat) },
                                onRescan = { viewModel.startStorageAnalysis() },
                                onDeleteFile = { path -> viewModel.deleteFileDirect(path) },
                                onCleanDuplicateGroup = { group -> viewModel.cleanDuplicateGroup(group) },
                                onDeleteAllEmptyFolders = { viewModel.deleteAllEmptyFolders() },
                                onExplorePartition = { path -> viewModel.openDirectory(path) },
                                onRemountPartition = { part, rw -> viewModel.remountPartition(part, rw) },
                                onOpenFile = { file -> viewModel.openFile(file) },
                                onBack = { viewModel.navigateTo(Screen.HOME) }
                            )
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                showHiddenFiles = uiState.showHiddenFiles,
                                onToggleHiddenFiles = { viewModel.toggleHiddenFiles() },
                                onBack = { viewModel.navigateTo(Screen.HOME) }
                            )
                        }
                    }
                }

                // Persistent Mini Audio Player Bar docked at bottom
                if (uiState.audioState.currentTrack != null && !uiState.audioState.isExpanded) {
                    ESMiniPlayerBar(
                        state = uiState.audioState,
                        onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                        onNext = { viewModel.nextAudioTrack() },
                        onExpand = { viewModel.expandAudioPlayer() },
                        onClose = { viewModel.closeAudioPlayer() },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // Modal Dialogs & Full-Screen Viewers
    if (uiState.showRootDialog) {
        RootSettingsDialog(
            isRootMode = uiState.isRootMode,
            isRootAvailable = uiState.isRootAvailable,
            isRootGranted = uiState.isRootGranted,
            onToggleRootMode = { viewModel.toggleRootMode() },
            onRemount = { part, rw -> viewModel.remountPartition(part, rw) },
            onDismiss = { viewModel.dismissRootSettingsDialog() }
        )
    }

    if (uiState.showTerminalDialog) {
        TerminalDialog(
            terminalOutput = uiState.terminalOutput,
            isExecuting = uiState.isExecutingTerminal,
            isRootMode = uiState.isRootMode,
            onExecute = { cmd, root -> viewModel.executeTerminalCommand(cmd, root) },
            onClear = { viewModel.clearTerminalOutput() },
            onDismiss = { viewModel.dismissTerminalDialog() }
        )
    }

    uiState.chmodItem?.let { item ->
        ChmodDialog(
            item = item,
            onDismiss = { viewModel.dismissChmodDialog() },
            onConfirmChmod = { mode -> viewModel.applyChmod(item, mode) }
        )
    }

    if (uiState.showCreateDialog) {
        CreateFileDialog(
            onDismiss = { viewModel.dismissCreateDialog() },
            onCreateFolder = { name -> viewModel.createFolder(name) },
            onCreateFile = { name, content -> viewModel.createNewFile(name, content) }
        )
    }

    uiState.renamingItem?.let { item ->
        RenameDialog(
            item = item,
            onDismiss = { viewModel.dismissRenameDialog() },
            onConfirm = { newName -> viewModel.renameFile(item, newName) }
        )
    }

    uiState.filesToDelete?.let { list ->
        DeleteConfirmationDialog(
            items = list,
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = { toRecycleBin -> viewModel.confirmDelete(toRecycleBin) }
        )
    }

    if (uiState.showZipDialog) {
        ZipDialog(
            onDismiss = { viewModel.dismissZipDialog() },
            onConfirm = { zipName -> viewModel.compressSelectedToZip(zipName) }
        )
    }

    uiState.propertiesItem?.let { item ->
        FilePropertiesDialog(
            item = item,
            onDismiss = { viewModel.closeProperties() },
            onShare = { StoragePermissionHelper.shareFile(context, File(item.path)) },
            onOpenWith = { StoragePermissionHelper.openFileWithSystemApp(context, File(item.path)) },
            onChangePermissions = { viewModel.showChmodDialog(item) }
        )
    }

    // ES Note Editor Dialog
    uiState.editingFile?.let { file ->
        ESNoteEditorDialog(
            file = file,
            initialContent = uiState.editingContent,
            onDismiss = { viewModel.closeEditor() },
            onSave = { updated -> viewModel.saveEditedFile(updated) },
            onSaveAs = { newName, content -> viewModel.saveEditedFileAs(newName, content) }
        )
    }

    // ES Fullscreen Audio Player Dialog
    if (uiState.audioState.isExpanded && uiState.audioState.currentTrack != null) {
        ESAudioPlayerDialog(
            state = uiState.audioState,
            onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
            onNext = { viewModel.nextAudioTrack() },
            onPrevious = { viewModel.previousAudioTrack() },
            onSeekTo = { ms -> viewModel.seekAudioTo(ms) },
            onToggleLoopMode = { viewModel.toggleAudioLoopMode() },
            onSelectTrack = { trk -> viewModel.playAudio(trk, uiState.audioState.playlist, expand = true) },
            onDismiss = { viewModel.collapseAudioPlayer() }
        )
    }

    // ES Video Player Dialog
    uiState.viewingVideo?.let { file ->
        ESVideoPlayerDialog(
            file = file,
            onDismiss = { viewModel.closeVideoPlayer() }
        )
    }

    // ES Image Viewer Dialog
    uiState.viewingImage?.let { file ->
        ESImageViewerDialog(
            file = file,
            folderImages = uiState.folderImages,
            onDismiss = { viewModel.closeImageViewer() },
            onNavigateImage = { target -> viewModel.navigateImageViewer(target) },
            onDeleteImage = { target -> viewModel.deleteImageFromViewer(target) }
        )
    }

    // ES Batch Rename Dialog
    if (uiState.showBatchRenameDialog && uiState.batchRenameItems.isNotEmpty()) {
        ESBatchRenameDialog(
            selectedFiles = uiState.batchRenameItems,
            onDismiss = { viewModel.dismissBatchRename() },
            onConfirmBatchRename = { map -> viewModel.confirmBatchRename(map) }
        )
    }

    // ES Checksum Dialog
    uiState.checksumItem?.let { file ->
        ESChecksumDialog(
            file = file,
            onDismiss = { viewModel.dismissChecksum() }
        )
    }

    // ES Encryption / Decryption Dialog
    uiState.encryptionItem?.let { file ->
        ESEncryptionDialog(
            file = file,
            isDecryptMode = uiState.isDecryptMode,
            onDismiss = { viewModel.dismissEncryption() },
            onConfirmEncrypt = { pwd, del -> viewModel.confirmEncrypt(pwd, del) },
            onConfirmDecrypt = { pwd -> viewModel.confirmDecrypt(pwd) }
        )
    }

    // ZIP Archive Viewer Dialog
    uiState.viewingZip?.let { file ->
        ZipViewerDialog(
            file = file,
            entries = uiState.zipEntries,
            onDismiss = { viewModel.closeZipViewer() },
            onExtract = { viewModel.extractZipFile(file) }
        )
    }
}
