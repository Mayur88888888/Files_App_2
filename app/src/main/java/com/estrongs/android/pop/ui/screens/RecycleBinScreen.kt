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
import com.estrongs.android.pop.data.model.RecycleBinItem
import com.estrongs.android.pop.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    items: List<RecycleBinItem>,
    onRestore: (RecycleBinItem) -> Unit,
    onEmptyBin: () -> Unit,
    onBack: () -> Unit
) {
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    if (showEmptyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = { Text("Empty Recycle Bin?") },
            text = { Text("All items in the Recycle Bin will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirmDialog = false
                        onEmptyBin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_empty_bin_btn")
                ) {
                    Text("Empty Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("recycle_bin_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyConfirmDialog = true },
                            modifier = Modifier.testTag("empty_recycle_bin_btn")
                        ) {
                            Text("Empty", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ESBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(ESAccentRed.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = ESAccentRed,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recycle Bin is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Deleted files moved here can be restored",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        RecycleBinRow(item = item, onRestore = { onRestore(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecycleBinRow(
    item: RecycleBinItem,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recycle_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (item.isDirectory) ESFolderYellow.copy(alpha = 0.15f) else ESDocBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (item.isDirectory) ESFolderYellow else ESDocBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Original: ${item.originalPath}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onRestore,
                modifier = Modifier.testTag("restore_btn_${item.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = ESBlue)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore")
            }
        }
    }
}
