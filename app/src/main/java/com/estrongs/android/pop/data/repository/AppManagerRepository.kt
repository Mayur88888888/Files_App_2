package com.estrongs.android.pop.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.estrongs.android.pop.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppManagerRepository(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = try {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val appItems = mutableListOf<AppItem>()
        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (!includeSystemApps && isSystem) {
                continue
            }

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.packageName
            }

            val apkFile = File(appInfo.sourceDir)
            val apkSize = if (apkFile.exists()) apkFile.length() else 0L

            appItems.add(
                AppItem(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    isSystemApp = isSystem,
                    apkSize = apkSize,
                    installedTime = pkg.firstInstallTime,
                    sourceDir = appInfo.sourceDir
                )
            )
        }

        appItems.sortedBy { it.appName.lowercase() }
    }

    suspend fun backupAppApk(app: AppItem, targetBackupDir: File): File? = withContext(Dispatchers.IO) {
        try {
            if (!targetBackupDir.exists()) targetBackupDir.mkdirs()
            val sourceApk = File(app.sourceDir)
            if (!sourceApk.exists()) return@withContext null

            val safeName = app.appName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val targetFile = File(targetBackupDir, "${safeName}_v${app.versionName}.apk")
            sourceApk.copyTo(targetFile, overwrite = true)
            targetFile
        } catch (_: Exception) {
            null
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAppDetails(packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
