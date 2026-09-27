package com.estrongs.android.pop.data.model

enum class FileCategory(val label: String) {
    ALL("All Files"),
    IMAGES("Images"),
    MUSIC("Music"),
    VIDEOS("Videos"),
    DOCUMENTS("Documents"),
    APKS("APKs"),
    ARCHIVES("Archives"),
    DOWNLOADS("Downloads"),
    RECENT("Recent")
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val extension: String = "",
    val itemCount: Int = 0,
    val category: FileCategory = FileCategory.ALL,
    val isHidden: Boolean = name.startsWith(".")
) {
    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (itemCount == 1) "1 item" else "$itemCount items"
            }
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val adjusted = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format("%.1f %s", adjusted, units[digitGroups.coerceIn(0, units.size - 1)])
        }

    val formattedDate: String
        get() {
            if (lastModified <= 0) return ""
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(lastModified))
        }
}
