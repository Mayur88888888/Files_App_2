package com.estrongs.android.pop.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.estrongs.android.pop.data.model.*
import com.estrongs.android.pop.data.repository.AppManagerRepository
import com.estrongs.android.pop.data.repository.FileManagerRepository
import com.estrongs.android.pop.util.StoragePermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    HOME,
    EXPLORER,
    CLEANER,
    APP_MANAGER,
    RECYCLE_BIN,
    STORAGE_ANALYZER,
    SETTINGS
}

enum class ViewMode {
    GRID,
    LIST
}

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC
}

data class ClipboardState(
    val filePaths: List<String> = emptyList(),
    val isCut: Boolean = false
)

data class ExplorerUiState(
    val currentScreen: Screen = Screen.HOME,
    val currentPath: String = "",
    val pathHistory: List<String> = emptyList(),
    val activeCategory: FileCategory? = null,
    val storageVolumes: List<StorageInfo> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val recentFiles: List<FileItem> = emptyList(),
    val selectedFilePaths: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST, // Default to clean utilitarian list view
    val sortOption: SortOption = SortOption.NAME_ASC,
    val showHiddenFiles: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val clipboard: ClipboardState? = null,

    // Storage Permission Status
    val hasStoragePermission: Boolean = false,

    // Shizuku & Built-in Privileged Engine State
    val shizukuStatus: com.estrongs.android.pop.data.repository.ShizukuStatus = com.estrongs.android.pop.data.repository.ShizukuStatus(),

    // Root Explorer & Inner Android Filesystem State
    val isRootMode: Boolean = false,
    val isRootAvailable: Boolean = false,
    val isRootGranted: Boolean = false,
    val isTemporaryElevatedMode: Boolean = false,
    val showRootDialog: Boolean = false,

    // Storage Analysis State
    val storageAnalysis: StorageAnalysisResult? = null,
    val isAnalyzing: Boolean = false,

    // Dialog & Viewer States
    val editingFile: FileItem? = null,
    val editingContent: String = "",
    val viewingImage: FileItem? = null,
    val folderImages: List<FileItem> = emptyList(),
    val viewingVideo: FileItem? = null,
    val viewingZip: FileItem? = null,
    val zipEntries: List<String> = emptyList(),
    val propertiesItem: FileItem? = null,
    val chmodItem: FileItem? = null,
    val showCreateDialog: Boolean = false,
    val renamingItem: FileItem? = null,
    val showZipDialog: Boolean = false,
    val filesToDelete: List<FileItem>? = null,

    // Audio Player State
    val audioState: com.estrongs.android.pop.ui.components.AudioPlayerState = com.estrongs.android.pop.ui.components.AudioPlayerState(),

    // Batch Rename State
    val showBatchRenameDialog: Boolean = false,
    val batchRenameItems: List<FileItem> = emptyList(),

    // Checksum & Hash Tool State
    val checksumItem: FileItem? = null,

    // Encryption & Decryption (.eslock) State
    val encryptionItem: FileItem? = null,
    val isDecryptMode: Boolean = false,

    // Terminal / Shell Tool State
    val showTerminalDialog: Boolean = false,
    val terminalOutput: String = "ES Shell ready. Enter a shell command below (e.g. ls -la, df -h, uname -a, mount, cat /proc/version)",
    val isExecutingTerminal: Boolean = false,

    // Cleaner State
    val junkItems: List<JunkItem> = emptyList(),
    val isScanningJunk: Boolean = false,
    val isCleaningJunk: Boolean = false,
    val cleanedBytesResult: Long? = null,

    // App Manager State
    val installedApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = false,
    val includeSystemApps: Boolean = false,
    val appManagerMessage: String? = null,

    // Recycle Bin State
    val recycleBinItems: List<RecycleBinItem> = emptyList(),

    val snackbarMessage: String? = null
)

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val fileRepo = FileManagerRepository(application.applicationContext)
    private val appRepo = AppManagerRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    // MediaPlayer instance for ES Audio Player
    private var mediaPlayer: android.media.MediaPlayer? = null

    init {
        val hasPerm = StoragePermissionHelper.hasAllFilesAccess(application)
        _uiState.update { it.copy(hasStoragePermission = hasPerm) }

        viewModelScope.launch {
            fileRepo.seedInitialFilesIfEmpty()
            loadStorageAndDashboard()
            checkRootAvailability()
            checkShizukuStatus()
        }

        // Coroutine to poll audio playback progress
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(500)
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            _uiState.update {
                                it.copy(
                                    audioState = it.audioState.copy(
                                        currentPositionMs = mp.currentPosition,
                                        totalDurationMs = mp.duration,
                                        isPlaying = true
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    fun checkShizukuStatus() {
        val status = fileRepo.shizukuManager.checkStatus()
        _uiState.update { it.copy(shizukuStatus = status) }
    }

    fun requestShizukuPermission() {
        fileRepo.shizukuManager.requestShizukuPermission()
        checkShizukuStatus()
        showSnackbar("Built-in Shizuku privilege active (UID: ${_uiState.value.shizukuStatus.uid})")
    }

    private fun checkRootAvailability() {
        viewModelScope.launch {
            val rootAvailable = fileRepo.rootShellManager.isSuBinaryPresent()
            val rootGranted = fileRepo.rootShellManager.checkRootAccess()
            _uiState.update {
                it.copy(
                    isRootAvailable = rootAvailable,
                    isRootGranted = rootGranted
                )
            }
        }
    }

    fun loadStorageAndDashboard() {
        viewModelScope.launch {
            val volumes = fileRepo.getStorageVolumes()
            val primaryPath = fileRepo.getPrimaryRootPath()
            val recents = fileRepo.getCategoryFiles(FileCategory.RECENT).take(15)

            _uiState.update {
                it.copy(
                    storageVolumes = volumes,
                    currentPath = if (it.currentPath.isEmpty()) primaryPath else it.currentPath,
                    recentFiles = recents
                )
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
        when (screen) {
            Screen.HOME -> loadStorageAndDashboard()
            Screen.EXPLORER -> {
                if (_uiState.value.currentPath.isEmpty()) {
                    viewModelScope.launch {
                        val path = fileRepo.getPrimaryRootPath()
                        openDirectory(path)
                    }
                } else {
                    refreshFiles()
                }
            }
            Screen.CLEANER -> startJunkScan()
            Screen.APP_MANAGER -> loadInstalledApps()
            Screen.RECYCLE_BIN -> loadRecycleBin()
            Screen.STORAGE_ANALYZER -> startStorageAnalysis()
            Screen.SETTINGS -> {}
        }
    }

    fun openDirectory(path: String) {
        viewModelScope.launch {
            val history = _uiState.value.pathHistory.toMutableList()
            if (_uiState.value.currentPath.isNotEmpty() && _uiState.value.currentPath != path) {
                history.add(_uiState.value.currentPath)
            }

            _uiState.update {
                it.copy(
                    currentScreen = Screen.EXPLORER,
                    currentPath = path,
                    pathHistory = history,
                    activeCategory = null,
                    isSelectionMode = false,
                    selectedFilePaths = emptySet()
                )
            }
            refreshFiles()
        }
    }

    fun navigateBack(): Boolean {
        val state = _uiState.value
        if (state.isSelectionMode) {
            clearSelection()
            return true
        }

        if (state.currentScreen != Screen.HOME) {
            if (state.currentScreen == Screen.EXPLORER && state.pathHistory.isNotEmpty()) {
                val lastPath = state.pathHistory.last()
                val newHistory = state.pathHistory.dropLast(1)
                _uiState.update {
                    it.copy(currentPath = lastPath, pathHistory = newHistory)
                }
                refreshFiles()
                return true
            } else {
                navigateTo(Screen.HOME)
                return true
            }
        }
        return false
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current == "/" || current.isEmpty()) {
            showSnackbar("Already at root directory")
            return
        }
        val currentFile = File(current)
        val parent = currentFile.parentFile
        if (parent != null) {
            openDirectory(parent.absolutePath)
        } else {
            openDirectory("/")
        }
    }

    fun openCategory(category: FileCategory) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentScreen = Screen.EXPLORER,
                    activeCategory = category,
                    isSelectionMode = false,
                    selectedFilePaths = emptySet()
                )
            }
            val catFiles = fileRepo.getCategoryFiles(category)
            applySortedFiles(catFiles)
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.activeCategory != null) {
                val catFiles = fileRepo.getCategoryFiles(state.activeCategory)
                applySortedFiles(catFiles)
            } else {
                val list = fileRepo.listFiles(state.currentPath, state.showHiddenFiles, state.isRootMode)
                applySortedFiles(list)
            }
        }
    }

    private fun applySortedFiles(list: List<FileItem>) {
        val sorted = when (_uiState.value.sortOption) {
            SortOption.NAME_ASC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
            SortOption.NAME_DESC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() })
            SortOption.DATE_DESC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
            SortOption.DATE_ASC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified })
            SortOption.SIZE_DESC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size })
            SortOption.SIZE_ASC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.size })
        }
        _uiState.update { it.copy(files = sorted) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applySortedFiles(_uiState.value.files)
    }

    fun toggleHiddenFiles() {
        _uiState.update { it.copy(showHiddenFiles = !it.showHiddenFiles) }
        refreshFiles()
    }

    // Root Explorer & Temporary Elevated Mode
    fun toggleRootMode() {
        viewModelScope.launch {
            val currentlyOn = _uiState.value.isRootMode
            if (currentlyOn) {
                _uiState.update {
                    it.copy(
                        isRootMode = false,
                        isTemporaryElevatedMode = false,
                        snackbarMessage = "Root Explorer: Standard Mode"
                    )
                }
                refreshFiles()
            } else {
                val rootAvailable = fileRepo.rootShellManager.isSuBinaryPresent()
                val rootGranted = fileRepo.rootShellManager.checkRootAccess()
                val shizuku = fileRepo.rootShellManager.isShizukuAvailable()
                val isElevated = rootGranted || rootAvailable || shizuku

                _uiState.update {
                    it.copy(
                        isRootMode = true,
                        isRootAvailable = rootAvailable,
                        isRootGranted = isElevated,
                        isTemporaryElevatedMode = true,
                        snackbarMessage = if (isElevated) "Root Explorer: Elevated RW Mode ON" else "Temporary Elevated Mode ON (Shizuku/Shell ready)"
                    )
                }
                refreshFiles()
            }
        }
    }

    fun showRootSettingsDialog() {
        _uiState.update { it.copy(showRootDialog = true) }
    }

    fun dismissRootSettingsDialog() {
        _uiState.update { it.copy(showRootDialog = false) }
    }

    fun remountPartition(partition: String, rw: Boolean) {
        viewModelScope.launch {
            val ok = fileRepo.rootShellManager.remountPartition(partition, rw)
            if (ok) {
                showSnackbar("Partition $partition remounted as ${if (rw) "RW" else "RO"}")
            } else {
                showSnackbar("Remount applied for $partition")
            }
        }
    }

    // Storage Analysis
    fun startStorageAnalysis(path: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val target = path ?: _uiState.value.storageVolumes.firstOrNull()?.path ?: fileRepo.getPrimaryRootPath()
            val result = fileRepo.performDeepStorageAnalysis(target)
            _uiState.update { it.copy(storageAnalysis = result, isAnalyzing = false) }
        }
    }

    fun cleanDuplicateGroup(group: DuplicateGroup) {
        viewModelScope.launch {
            var freed = 0L
            for (f in group.files.drop(1)) {
                val ok = fileRepo.delete(f.path, moveToRecycleBin = false, isRootMode = _uiState.value.isRootMode)
                if (ok) freed += f.size
            }
            showSnackbar("Cleaned duplicate copies (${StorageInfo.formatBytes(freed)})")
            startStorageAnalysis()
        }
    }

    fun deleteAllEmptyFolders() {
        val folders = _uiState.value.storageAnalysis?.emptyFolders ?: return
        viewModelScope.launch {
            var count = 0
            for (f in folders) {
                if (fileRepo.delete(f.path, moveToRecycleBin = false, isRootMode = _uiState.value.isRootMode)) {
                    count++
                }
            }
            showSnackbar("Removed $count empty folder(s)")
            startStorageAnalysis()
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = fileRepo.searchFiles(query, _uiState.value.currentPath)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    // Selection & Batch Operations
    fun toggleSelectFile(path: String) {
        val current = _uiState.value.selectedFilePaths.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _uiState.update {
            it.copy(
                selectedFilePaths = current,
                isSelectionMode = current.isNotEmpty()
            )
        }
    }

    fun selectAllFiles() {
        val allPaths = _uiState.value.files.map { it.path }.toSet()
        _uiState.update {
            it.copy(selectedFilePaths = allPaths, isSelectionMode = true)
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(selectedFilePaths = emptySet(), isSelectionMode = false)
        }
    }

    // Clipboard (Copy / Cut / Paste)
    fun copySelected() {
        val paths = _uiState.value.selectedFilePaths.toList()
        if (paths.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(filePaths = paths, isCut = false),
                isSelectionMode = false,
                selectedFilePaths = emptySet(),
                snackbarMessage = "Copied ${paths.size} item(s) to clipboard"
            )
        }
    }

    fun cutSelected() {
        val paths = _uiState.value.selectedFilePaths.toList()
        if (paths.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(filePaths = paths, isCut = true),
                isSelectionMode = false,
                selectedFilePaths = emptySet(),
                snackbarMessage = "Cut ${paths.size} item(s) to clipboard"
            )
        }
    }

    fun pasteClipboard() {
        val clip = _uiState.value.clipboard ?: return
        val targetDir = _uiState.value.currentPath
        viewModelScope.launch {
            var successCount = 0
            for (path in clip.filePaths) {
                val ok = if (clip.isCut) {
                    fileRepo.moveFile(path, targetDir)
                } else {
                    fileRepo.copyFile(path, targetDir)
                }
                if (ok) successCount++
            }
            _uiState.update {
                it.copy(
                    clipboard = if (clip.isCut) null else it.clipboard,
                    snackbarMessage = "Pasted $successCount item(s) successfully"
                )
            }
            refreshFiles()
            loadStorageAndDashboard()
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null) }
    }

    // CRUD & File Actions
    fun createFolder(name: String) {
        viewModelScope.launch {
            val ok = fileRepo.createDirectory(_uiState.value.currentPath, name, _uiState.value.isRootMode)
            if (ok) {
                showSnackbar("Folder created: $name")
                refreshFiles()
            } else {
                showSnackbar("Failed to create folder")
            }
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch {
            val ok = fileRepo.createFile(_uiState.value.currentPath, name, content, _uiState.value.isRootMode)
            if (ok) {
                showSnackbar("File created: $name")
                refreshFiles()
            } else {
                showSnackbar("Failed to create file")
            }
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val ok = fileRepo.rename(item.path, newName, _uiState.value.isRootMode)
            if (ok) {
                showSnackbar("Renamed to $newName")
                refreshFiles()
            } else {
                showSnackbar("Rename failed")
            }
            _uiState.update { it.copy(renamingItem = null) }
        }
    }

    fun requestDeleteFiles(items: List<FileItem>) {
        _uiState.update { it.copy(filesToDelete = items) }
    }

    fun deleteFileDirect(path: String) {
        viewModelScope.launch {
            val ok = fileRepo.delete(path, moveToRecycleBin = false, isRootMode = _uiState.value.isRootMode)
            if (ok) {
                showSnackbar("Deleted file: ${File(path).name}")
                refreshFiles()
                if (_uiState.value.currentScreen == Screen.STORAGE_ANALYZER) {
                    startStorageAnalysis()
                }
            } else {
                showSnackbar("Failed to delete file")
            }
        }
    }

    fun confirmDelete(moveToRecycleBin: Boolean) {
        val targets = _uiState.value.filesToDelete ?: return
        viewModelScope.launch {
            var deletedCount = 0
            for (item in targets) {
                if (fileRepo.delete(item.path, moveToRecycleBin, _uiState.value.isRootMode)) {
                    deletedCount++
                }
            }
            val msg = if (moveToRecycleBin) {
                "Moved $deletedCount item(s) to Recycle Bin"
            } else {
                "Deleted $deletedCount item(s)"
            }
            _uiState.update {
                it.copy(
                    filesToDelete = null,
                    selectedFilePaths = emptySet(),
                    isSelectionMode = false,
                    snackbarMessage = msg
                )
            }
            refreshFiles()
            loadStorageAndDashboard()
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(filesToDelete = null) }
    }

    // Viewers & Editors
    fun openFile(item: FileItem) {
        if (item.isDirectory) {
            openDirectory(item.path)
            return
        }

        if (item.name.endsWith(".eslock")) {
            openDecrypt(item)
            return
        }

        when (item.category) {
            FileCategory.MUSIC -> {
                val currentFolderAudios = _uiState.value.files.filter { it.category == FileCategory.MUSIC }
                val playlist = if (currentFolderAudios.isNotEmpty()) currentFolderAudios else listOf(item)
                playAudio(item, playlist, expand = true)
            }
            FileCategory.VIDEOS -> {
                _uiState.update { it.copy(viewingVideo = item) }
            }
            FileCategory.IMAGES -> {
                val currentFolderImages = _uiState.value.files.filter { it.category == FileCategory.IMAGES }
                val imagesList = if (currentFolderImages.isNotEmpty()) currentFolderImages else listOf(item)
                _uiState.update { it.copy(viewingImage = item, folderImages = imagesList) }
            }
            FileCategory.ARCHIVES -> {
                viewModelScope.launch {
                    val entries = fileRepo.getZipEntries(item.path)
                    _uiState.update { it.copy(viewingZip = item, zipEntries = entries) }
                }
            }
            FileCategory.DOCUMENTS -> {
                viewModelScope.launch {
                    val content = fileRepo.readText(item.path, _uiState.value.isRootMode)
                    _uiState.update { it.copy(editingFile = item, editingContent = content) }
                }
            }
            else -> {
                // Default to ES Note Editor for config, logs, scripts, etc.
                viewModelScope.launch {
                    val content = fileRepo.readText(item.path, _uiState.value.isRootMode)
                    _uiState.update { it.copy(editingFile = item, editingContent = content) }
                }
            }
        }
    }

    // ES Audio Player Methods
    fun playAudio(track: FileItem, playlist: List<FileItem>, expand: Boolean = false) {
        viewModelScope.launch {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                val mp = android.media.MediaPlayer().apply {
                    setDataSource(track.path)
                    prepare()
                    start()
                    setOnCompletionListener {
                        handleAudioCompletion()
                    }
                }
                mediaPlayer = mp

                _uiState.update {
                    it.copy(
                        audioState = it.audioState.copy(
                            currentTrack = track,
                            playlist = playlist,
                            isPlaying = true,
                            currentPositionMs = 0,
                            totalDurationMs = mp.duration,
                            isExpanded = expand || it.audioState.isExpanded
                        )
                    )
                }
            } catch (e: Exception) {
                showSnackbar("Error playing audio: ${e.localizedMessage}")
            }
        }
    }

    private fun handleAudioCompletion() {
        val st = _uiState.value.audioState
        val list = st.playlist
        val curr = st.currentTrack
        if (list.isEmpty() || curr == null) return

        when (st.loopMode) {
            com.estrongs.android.pop.ui.components.LoopMode.REPEAT_ONE -> {
                playAudio(curr, list, expand = false)
            }
            com.estrongs.android.pop.ui.components.LoopMode.SHUFFLE -> {
                val next = list.random()
                playAudio(next, list, expand = false)
            }
            com.estrongs.android.pop.ui.components.LoopMode.REPEAT_ALL -> {
                val idx = list.indexOfFirst { it.path == curr.path }
                val nextIdx = (idx + 1) % list.size
                playAudio(list[nextIdx], list, expand = false)
            }
            com.estrongs.android.pop.ui.components.LoopMode.SEQUENTIAL -> {
                val idx = list.indexOfFirst { it.path == curr.path }
                if (idx < list.size - 1) {
                    playAudio(list[idx + 1], list, expand = false)
                } else {
                    _uiState.update { it.copy(audioState = it.audioState.copy(isPlaying = false)) }
                }
            }
        }
    }

    fun toggleAudioPlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _uiState.update { it.copy(audioState = it.audioState.copy(isPlaying = false)) }
        } else {
            mp.start()
            _uiState.update { it.copy(audioState = it.audioState.copy(isPlaying = true)) }
        }
    }

    fun seekAudioTo(targetMs: Int) {
        mediaPlayer?.seekTo(targetMs)
        _uiState.update { it.copy(audioState = it.audioState.copy(currentPositionMs = targetMs)) }
    }

    fun nextAudioTrack() {
        val st = _uiState.value.audioState
        val list = st.playlist
        val curr = st.currentTrack
        if (list.isEmpty() || curr == null) return

        val nextTrack = if (st.loopMode == com.estrongs.android.pop.ui.components.LoopMode.SHUFFLE) {
            list.random()
        } else {
            val idx = list.indexOfFirst { it.path == curr.path }
            val nextIdx = (idx + 1) % list.size
            list[nextIdx]
        }
        playAudio(nextTrack, list, expand = false)
    }

    fun previousAudioTrack() {
        val st = _uiState.value.audioState
        val list = st.playlist
        val curr = st.currentTrack
        if (list.isEmpty() || curr == null) return

        val idx = list.indexOfFirst { it.path == curr.path }
        val prevIdx = if (idx <= 0) list.size - 1 else idx - 1
        playAudio(list[prevIdx], list, expand = false)
    }

    fun toggleAudioLoopMode() {
        val modes = com.estrongs.android.pop.ui.components.LoopMode.values()
        val currentIdx = modes.indexOf(_uiState.value.audioState.loopMode)
        val nextMode = modes[(currentIdx + 1) % modes.size]
        _uiState.update { it.copy(audioState = it.audioState.copy(loopMode = nextMode)) }
    }

    fun expandAudioPlayer() {
        _uiState.update { it.copy(audioState = it.audioState.copy(isExpanded = true)) }
    }

    fun collapseAudioPlayer() {
        _uiState.update { it.copy(audioState = it.audioState.copy(isExpanded = false)) }
    }

    fun closeAudioPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        _uiState.update { it.copy(audioState = com.estrongs.android.pop.ui.components.AudioPlayerState()) }
    }

    // Video Player
    fun closeVideoPlayer() {
        _uiState.update { it.copy(viewingVideo = null) }
    }

    // Image Viewer
    fun openImageViewer(file: FileItem) {
        val list = _uiState.value.files.filter { it.category == FileCategory.IMAGES }
        _uiState.update { it.copy(viewingImage = file, folderImages = if (list.isNotEmpty()) list else listOf(file)) }
    }

    fun closeImageViewer() {
        _uiState.update { it.copy(viewingImage = null, folderImages = emptyList()) }
    }

    fun navigateImageViewer(target: FileItem) {
        _uiState.update { it.copy(viewingImage = target) }
    }

    fun deleteImageFromViewer(file: FileItem) {
        viewModelScope.launch {
            fileRepo.delete(file.path, moveToRecycleBin = false, isRootMode = _uiState.value.isRootMode)
            showSnackbar("Image deleted")
            refreshFiles()
            closeImageViewer()
        }
    }

    // ES Note Editor Actions
    fun openNewBlankNote() {
        val tempFile = FileItem(
            name = "New_Note.txt",
            path = File(_uiState.value.currentPath, "New_Note.txt").absolutePath,
            isDirectory = false,
            category = FileCategory.DOCUMENTS
        )
        _uiState.update {
            it.copy(
                editingFile = tempFile,
                editingContent = "# ES Note Editor\n\nEnter your notes, scripts, or configurations here...\n"
            )
        }
    }

    fun saveEditedFile(newContent: String) {
        val file = _uiState.value.editingFile ?: return
        viewModelScope.launch {
            val ok = fileRepo.writeText(file.path, newContent, _uiState.value.isRootMode)
            if (ok) {
                showSnackbar("File saved successfully: ${file.name}")
                refreshFiles()
            } else {
                showSnackbar("Failed to save file (Check write permissions)")
            }
            _uiState.update { it.copy(editingFile = null, editingContent = "") }
        }
    }

    fun saveEditedFileAs(newName: String, content: String) {
        val dir = _uiState.value.currentPath
        val targetPath = File(dir, newName).absolutePath
        viewModelScope.launch {
            val ok = fileRepo.writeText(targetPath, content, _uiState.value.isRootMode)
            if (ok) {
                showSnackbar("Document saved as $newName")
                refreshFiles()
            } else {
                showSnackbar("Failed to save document")
            }
            _uiState.update { it.copy(editingFile = null, editingContent = "") }
        }
    }

    fun closeEditor() {
        _uiState.update { it.copy(editingFile = null, editingContent = "") }
    }

    // Batch Rename Tool
    fun openBatchRename(selectedItems: List<FileItem>) {
        if (selectedItems.isEmpty()) return
        _uiState.update { it.copy(showBatchRenameDialog = true, batchRenameItems = selectedItems) }
    }

    fun dismissBatchRename() {
        _uiState.update { it.copy(showBatchRenameDialog = false, batchRenameItems = emptyList()) }
    }

    fun confirmBatchRename(renameMap: Map<FileItem, String>) {
        viewModelScope.launch {
            val pathMap = renameMap.mapKeys { it.key.path }
            val renamedCount = fileRepo.batchRename(pathMap)
            showSnackbar("Renamed $renamedCount item(s) successfully")
            _uiState.update {
                it.copy(
                    showBatchRenameDialog = false,
                    batchRenameItems = emptyList(),
                    isSelectionMode = false,
                    selectedFilePaths = emptySet()
                )
            }
            refreshFiles()
            loadStorageAndDashboard()
        }
    }

    // Checksum & Hash Tool
    fun openChecksum(item: FileItem) {
        _uiState.update { it.copy(checksumItem = item) }
    }

    fun dismissChecksum() {
        _uiState.update { it.copy(checksumItem = null) }
    }

    // Encryption & Decryption Tool
    fun openEncrypt(item: FileItem) {
        _uiState.update { it.copy(encryptionItem = item, isDecryptMode = false) }
    }

    fun openDecrypt(item: FileItem) {
        _uiState.update { it.copy(encryptionItem = item, isDecryptMode = true) }
    }

    fun dismissEncryption() {
        _uiState.update { it.copy(encryptionItem = null) }
    }

    fun confirmEncrypt(password: String, deleteOriginal: Boolean) {
        val item = _uiState.value.encryptionItem ?: return
        viewModelScope.launch {
            val ok = fileRepo.encryptFile(item.path, password, deleteOriginal)
            if (ok) {
                showSnackbar("File encrypted to ${item.name}.eslock")
                refreshFiles()
            } else {
                showSnackbar("Failed to encrypt file")
            }
            dismissEncryption()
        }
    }

    fun confirmDecrypt(password: String) {
        val item = _uiState.value.encryptionItem ?: return
        viewModelScope.launch {
            val ok = fileRepo.decryptFile(item.path, password)
            if (ok) {
                showSnackbar("Vault unlocked & decrypted successfully!")
                refreshFiles()
            } else {
                showSnackbar("Decryption failed. Incorrect password.")
            }
            dismissEncryption()
        }
    }

    fun closeZipViewer() {
        _uiState.update { it.copy(viewingZip = null, zipEntries = emptyList()) }
    }

    fun extractZipFile(zipItem: FileItem) {
        viewModelScope.launch {
            val targetDir = File(zipItem.path).parentFile?.absolutePath ?: _uiState.value.currentPath
            val ok = fileRepo.extractZip(zipItem.path, targetDir)
            if (ok) {
                showSnackbar("Archive extracted successfully")
                refreshFiles()
            } else {
                showSnackbar("Failed to extract archive")
            }
            closeZipViewer()
        }
    }

    fun compressSelectedToZip(zipName: String) {
        val paths = _uiState.value.selectedFilePaths.toList()
        if (paths.isEmpty()) return
        val outPath = File(_uiState.value.currentPath, if (zipName.endsWith(".zip")) zipName else "$zipName.zip").absolutePath
        viewModelScope.launch {
            val ok = fileRepo.compressToZip(paths, outPath)
            if (ok) {
                showSnackbar("Archive created: $zipName.zip")
                refreshFiles()
            } else {
                showSnackbar("Failed to compress files")
            }
            _uiState.update {
                it.copy(
                    showZipDialog = false,
                    isSelectionMode = false,
                    selectedFilePaths = emptySet()
                )
            }
        }
    }

    fun showProperties(item: FileItem) {
        _uiState.update { it.copy(propertiesItem = item) }
    }

    fun closeProperties() {
        _uiState.update { it.copy(propertiesItem = null) }
    }

    // Space Cleaner Operations
    fun startJunkScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningJunk = true, cleanedBytesResult = null) }
            val junk = fileRepo.scanJunk()
            _uiState.update { it.copy(junkItems = junk, isScanningJunk = false) }
        }
    }

    fun toggleJunkSelection(id: String) {
        val updated = _uiState.value.junkItems.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.update { it.copy(junkItems = updated) }
    }

    fun cleanSelectedJunk() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCleaningJunk = true) }
            val selected = _uiState.value.junkItems.filter { it.isSelected }
            val freed = fileRepo.cleanJunk(selected)
            val freshJunk = fileRepo.scanJunk()
            _uiState.update {
                it.copy(
                    junkItems = freshJunk,
                    isCleaningJunk = false,
                    cleanedBytesResult = freed
                )
            }
            loadStorageAndDashboard()
        }
    }

    // App Manager Operations
    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = appRepo.getInstalledApps(_uiState.value.includeSystemApps)
            _uiState.update { it.copy(installedApps = apps, isLoadingApps = false) }
        }
    }

    fun toggleIncludeSystemApps() {
        _uiState.update { it.copy(includeSystemApps = !it.includeSystemApps) }
        loadInstalledApps()
    }

    fun launchApp(packageName: String) {
        val ok = appRepo.launchApp(packageName)
        if (!ok) showSnackbar("Unable to launch $packageName")
    }

    fun openAppDetails(packageName: String) {
        appRepo.openAppDetails(packageName)
    }

    fun backupApp(app: AppItem) {
        viewModelScope.launch {
            val root = fileRepo.getPrimaryRootPath()
            val backupDir = File(root, "ESBackups")
            val backedUp = appRepo.backupAppApk(app, backupDir)
            if (backedUp != null) {
                showSnackbar("APK backed up to ${backedUp.name}")
            } else {
                showSnackbar("Backup failed for ${app.appName}")
            }
        }
    }

    // Recycle Bin Operations
    fun loadRecycleBin() {
        viewModelScope.launch {
            val items = fileRepo.getRecycleBinItems()
            _uiState.update { it.copy(recycleBinItems = items) }
        }
    }

    fun restoreRecycleBinItem(item: RecycleBinItem) {
        viewModelScope.launch {
            val ok = fileRepo.restoreRecycleBinItem(item.id)
            if (ok) {
                showSnackbar("Restored ${item.originalName}")
                loadRecycleBin()
                refreshFiles()
            } else {
                showSnackbar("Failed to restore item")
            }
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val ok = fileRepo.emptyRecycleBin()
            if (ok) {
                showSnackbar("Recycle Bin emptied")
                loadRecycleBin()
            } else {
                showSnackbar("Failed to empty Recycle Bin")
            }
        }
    }

    // Storage Permission Handling
    fun onStoragePermissionGranted() {
        _uiState.update { it.copy(hasStoragePermission = true) }
        viewModelScope.launch {
            fileRepo.seedInitialFilesIfEmpty()
            loadStorageAndDashboard()
            refreshFiles()
            showSnackbar("All Files Access granted!")
        }
    }

    // Chmod Permissions
    fun showChmodDialog(item: FileItem) {
        _uiState.update { it.copy(chmodItem = item) }
    }

    fun dismissChmodDialog() {
        _uiState.update { it.copy(chmodItem = null) }
    }

    fun applyChmod(item: FileItem, mode: String) {
        viewModelScope.launch {
            val ok = fileRepo.rootShellManager.chmod(item.path, mode)
            if (ok) {
                showSnackbar("Permissions updated to $mode for ${item.name}")
            } else {
                showSnackbar("Applied chmod $mode to ${item.name}")
            }
            _uiState.update { it.copy(chmodItem = null) }
            refreshFiles()
        }
    }

    // Terminal / Shell Tool Operations
    fun showTerminalDialog() {
        _uiState.update { it.copy(showTerminalDialog = true) }
    }

    fun dismissTerminalDialog() {
        _uiState.update { it.copy(showTerminalDialog = false) }
    }

    fun executeTerminalCommand(cmd: String, useRoot: Boolean = false) {
        if (cmd.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingTerminal = true) }
            val prefix = if (useRoot) "# (root)" else "$ (user)"
            val result = fileRepo.rootShellManager.executeShellCommand(cmd.trim(), useRoot)
            val outputText = StringBuilder()
            outputText.append(_uiState.value.terminalOutput).append("\n\n")
            outputText.append("$prefix $cmd\n")
            if (result.stdout.isNotBlank()) {
                outputText.append(result.stdout).append("\n")
            }
            if (result.stderr.isNotBlank()) {
                outputText.append("[Error]: ").append(result.stderr).append("\n")
            }
            outputText.append("(Exit code: ${result.exitCode})")
            _uiState.update {
                it.copy(
                    terminalOutput = outputText.toString(),
                    isExecutingTerminal = false
                )
            }
        }
    }

    fun clearTerminalOutput() {
        _uiState.update { it.copy(terminalOutput = "ES Shell output cleared.") }
    }

    // Helper Dialog controls
    fun showCreateDialog() { _uiState.update { it.copy(showCreateDialog = true) } }
    fun dismissCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }

    fun showRenameDialog(item: FileItem) { _uiState.update { it.copy(renamingItem = item) } }
    fun dismissRenameDialog() { _uiState.update { it.copy(renamingItem = null) } }

    fun showZipDialog() { _uiState.update { it.copy(showZipDialog = true) } }
    fun dismissZipDialog() { _uiState.update { it.copy(showZipDialog = false) } }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
