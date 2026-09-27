package com.estrongs.android.pop.data.model

data class StorageInfo(
    val title: String,
    val path: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val isPrimary: Boolean = true
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedUsed: String
        get() = formatBytes(usedBytes)

    val formattedTotal: String
        get() = formatBytes(totalBytes)

    val formattedFree: String
        get() = formatBytes(freeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val adjusted = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format("%.1f %s", adjusted, units[digitGroups.coerceIn(0, units.size - 1)])
        }
    }
}

data class PartitionInfo(
    val mountPoint: String,
    val deviceName: String,
    val fsType: String,
    val mountOptions: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val formattedUsed: String
        get() = StorageInfo.formatBytes(usedBytes)
    val formattedTotal: String
        get() = StorageInfo.formatBytes(totalBytes)
    val formattedFree: String
        get() = StorageInfo.formatBytes(freeBytes)
}

data class CategoryBreakdown(
    val category: FileCategory,
    val name: String,
    val totalBytes: Long,
    val fileCount: Int,
    val percent: Float,
    val colorHex: Long
) {
    val formattedSize: String
        get() = StorageInfo.formatBytes(totalBytes)
}

data class DuplicateGroup(
    val fileName: String,
    val fileSize: Long,
    val files: List<FileItem>
) {
    val formattedSize: String
        get() = StorageInfo.formatBytes(fileSize)
    val redundantBytes: String
        get() = StorageInfo.formatBytes(fileSize * (files.size - 1).coerceAtLeast(0))
}

data class StorageAnalysisResult(
    val rootPath: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categories: List<CategoryBreakdown>,
    val largestFiles: List<FileItem>,
    val duplicateGroups: List<DuplicateGroup>,
    val emptyFolders: List<FileItem>,
    val cacheBytes: Long,
    val partitions: List<PartitionInfo> = emptyList(),
    val isScanComplete: Boolean = true
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedUsed: String
        get() = StorageInfo.formatBytes(usedBytes)
    val formattedTotal: String
        get() = StorageInfo.formatBytes(totalBytes)
    val formattedFree: String
        get() = StorageInfo.formatBytes(freeBytes)
}

data class AppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val apkSize: Long,
    val installedTime: Long,
    val sourceDir: String
) {
    val formattedSize: String
        get() = StorageInfo.formatBytes(apkSize)
}

enum class JunkType(val displayName: String) {
    CACHE("System & App Cache"),
    TEMP("Temporary Files"),
    EMPTY_DIRS("Empty Folders"),
    LARGE_FILES("Large Files (>10MB)"),
    RESIDUAL_APKS("Obsolete APKs")
}

data class JunkItem(
    val id: String,
    val title: String,
    val type: JunkType,
    val totalBytes: Long,
    val filePaths: List<String>,
    val isSelected: Boolean = true
) {
    val formattedSize: String
        get() = StorageInfo.formatBytes(totalBytes)
}
