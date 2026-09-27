package com.estrongs.android.pop.data.model

data class RecycleBinItem(
    val id: String,
    val originalName: String,
    val originalPath: String,
    val currentPath: String,
    val isDirectory: Boolean,
    val size: Long,
    val deletedTimestamp: Long
) {
    val formattedSize: String
        get() = StorageInfo.formatBytes(size)

    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(deletedTimestamp))
        }
}
