package com.estrongs.android.pop.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object StoragePermissionHelper {

    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun getRequiredRuntimePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun openFileWithSystemApp(context: Context, file: File): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareFile(context: Context, file: File): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share file via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }

    fun calculateChecksum(file: File, algorithm: String = "MD5"): String {
        return try {
            if (!file.exists() || file.isDirectory) return "N/A"
            val digest = MessageDigest.getInstance(algorithm)
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
            fis.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
