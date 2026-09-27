package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.*
import java.io.File

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String, String) -> Unit
) {
    var isFolder by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFolder) "Create New Folder" else "Create New File",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isFolder,
                        onClick = { isFolder = true },
                        label = { Text("Folder") },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = ESFolderYellow)
                        },
                        modifier = Modifier.testTag("create_type_folder")
                    )
                    FilterChip(
                        selected = !isFolder,
                        onClick = { isFolder = false },
                        label = { Text("File (.txt)") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null, tint = ESDocBlue)
                        },
                        modifier = Modifier.testTag("create_type_file")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isFolder) "Folder Name" else "File Name (e.g. notes.txt)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_name_input")
                )

                if (!isFolder) {
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        label = { Text("Initial Content (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("create_content_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        if (isFolder) onCreateFolder(name.trim()) else onCreateFile(name.trim(), fileContent)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("create_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("create_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(item.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${if (item.isDirectory) "Folder" else "File"}") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && newName != item.name) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName != item.name,
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    items: List<FileItem>,
    onDismiss: () -> Unit,
    onConfirm: (moveToRecycleBin: Boolean) -> Unit
) {
    var moveToRecycleBin by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (items.size == 1) "Delete ${items[0].name}?" else "Delete ${items.size} items?"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to delete these files?")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { moveToRecycleBin = !moveToRecycleBin }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = moveToRecycleBin,
                        onCheckedChange = { moveToRecycleBin = it },
                        modifier = Modifier.testTag("recycle_bin_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Move to Recycle Bin (can be restored)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(moveToRecycleBin) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("delete_confirm_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("delete_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ZipDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var zipName by remember { mutableStateOf("archive_${System.currentTimeMillis() % 10000}.zip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress to ZIP") },
        text = {
            OutlinedTextField(
                value = zipName,
                onValueChange = { zipName = it },
                label = { Text("Archive Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zip_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (zipName.isNotBlank()) onConfirm(zipName.trim())
                },
                modifier = Modifier.testTag("zip_confirm_button")
            ) {
                Text("Compress")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("zip_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FilePropertiesDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit = {},
    onOpenWith: () -> Unit = {},
    onChangePermissions: () -> Unit = {}
) {
    var checksum by remember { mutableStateOf<String?>(null) }
    var isCalculatingChecksum by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("File Properties")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PropertyRow("Name", item.name)
                PropertyRow("Type", if (item.isDirectory) "Directory / Folder" else "${item.extension.uppercase()} file")
                PropertyRow("Size", "${item.formattedSize} (${item.size} bytes)")
                PropertyRow("Path", item.path)
                PropertyRow("Last Modified", item.formattedDate)
                if (item.isDirectory) {
                    PropertyRow("Contains", "${item.itemCount} items")
                }

                val f = File(item.path)
                val readStatus = if (f.canRead()) "Readable" else "Not readable"
                val writeStatus = if (f.canWrite()) "Writable" else "Read-only"
                val execStatus = if (f.canExecute()) "Executable" else "Non-executable"
                PropertyRow("Access Permissions", "$readStatus, $writeStatus, $execStatus")

                if (!item.isDirectory) {
                    if (checksum != null) {
                        PropertyRow("MD5 Checksum", checksum!!)
                    } else {
                        OutlinedButton(
                            onClick = {
                                isCalculatingChecksum = true
                                val md5 = com.estrongs.android.pop.util.StoragePermissionHelper.calculateChecksum(f, "MD5")
                                checksum = md5
                                isCalculatingChecksum = false
                            },
                            enabled = !isCalculatingChecksum,
                            modifier = Modifier.fillMaxWidth().testTag("compute_md5_btn"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(if (isCalculatingChecksum) "Calculating MD5..." else "Calculate MD5 Checksum", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f).testTag("prop_share_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenWith,
                        modifier = Modifier.weight(1f).testTag("prop_open_with_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open With", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onChangePermissions,
                        modifier = Modifier.weight(1f).testTag("prop_chmod_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chmod", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("properties_close_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun TextEditorDialog(
    file: FileItem,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("editor_cancel_button")
                        ) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(content) },
                            modifier = Modifier.testTag("editor_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("editor_text_field"),
                    placeholder = { Text("Write content here...") }
                )
            }
        }
    }
}

@Composable
fun ImageViewerDialog(
    file: FileItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = File(file.path),
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("image_viewer_preview")
            )

            // Top overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = file.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("image_viewer_close")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ZipViewerDialog(
    file: FileItem,
    entries: List<String>,
    onDismiss: () -> Unit,
    onExtract: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = ESZipPurple)
                        Column {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entries.size} entries",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                        Button(
                            onClick = onExtract,
                            modifier = Modifier.testTag("extract_zip_button")
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Extract")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(entries) { entry ->
                        val isDir = entry.endsWith("/")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (isDir) ESFolderYellow else ESDocBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
