package com.estrongs.android.pop.ui.screens

import androidx.compose.foundation.background
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
import com.estrongs.android.pop.data.model.AppItem
import com.estrongs.android.pop.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    apps: List<AppItem>,
    isLoading: Boolean,
    includeSystemApps: Boolean,
    onToggleSystemApps: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onBackupApp: (AppItem) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("app_manager_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("app_manager_refresh_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Controls header: Search & User/System app filter
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed applications...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredApps.size} applications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "System Apps",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = includeSystemApps,
                                onCheckedChange = { onToggleSystemApps() },
                                modifier = Modifier.testTag("toggle_system_apps_switch")
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ESBlue)
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No applications found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItemCard(
                            app = app,
                            onLaunch = { onLaunchApp(app.packageName) },
                            onDetails = { onOpenAppDetails(app.packageName) },
                            onBackup = { onBackupApp(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItemCard(
    app: AppItem,
    onLaunch: () -> Unit,
    onDetails: () -> Unit,
    onBackup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ESApkGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = ESApkGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "System",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "v${app.versionName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = app.formattedSize,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackup,
                    modifier = Modifier.testTag("app_backup_${app.packageName}")
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup APK")
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDetails) {
                    Icon(Icons.Default.Info, contentDescription = "App Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(4.dp))

                FilledTonalButton(
                    onClick = onLaunch,
                    modifier = Modifier.testTag("app_launch_${app.packageName}")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open")
                }
            }
        }
    }
}
