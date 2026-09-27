package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estrongs.android.pop.data.model.FileItem

@Composable
fun ChmodDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onConfirmChmod: (mode: String) -> Unit
) {
    var ownerRead by remember { mutableStateOf(true) }
    var ownerWrite by remember { mutableStateOf(true) }
    var ownerExec by remember { mutableStateOf(item.isDirectory) }

    var groupRead by remember { mutableStateOf(true) }
    var groupWrite by remember { mutableStateOf(false) }
    var groupExec by remember { mutableStateOf(item.isDirectory) }

    var othersRead by remember { mutableStateOf(true) }
    var othersWrite by remember { mutableStateOf(false) }
    var othersExec by remember { mutableStateOf(item.isDirectory) }

    val octalString = remember(ownerRead, ownerWrite, ownerExec, groupRead, groupWrite, groupExec, othersRead, othersWrite, othersExec) {
        val o = (if (ownerRead) 4 else 0) + (if (ownerWrite) 2 else 0) + (if (ownerExec) 1 else 0)
        val g = (if (groupRead) 4 else 0) + (if (groupWrite) 2 else 0) + (if (groupExec) 1 else 0)
        val oth = (if (othersRead) 4 else 0) + (if (othersWrite) 2 else 0) + (if (othersExec) 1 else 0)
        "$o$g$oth"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Permissions (chmod)")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mode: $octalString",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                PermissionRow("Owner", ownerRead, { ownerRead = it }, ownerWrite, { ownerWrite = it }, ownerExec, { ownerExec = it })
                PermissionRow("Group", groupRead, { groupRead = it }, groupWrite, { groupWrite = it }, groupExec, { groupExec = it })
                PermissionRow("Others", othersRead, { othersRead = it }, othersWrite, { othersWrite = it }, othersExec, { othersExec = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmChmod(octalString) },
                modifier = Modifier.testTag("confirm_chmod_btn")
            ) {
                Text("Apply ($octalString)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    read: Boolean,
    onReadChange: (Boolean) -> Unit,
    write: Boolean,
    onWriteChange: (Boolean) -> Unit,
    exec: Boolean,
    onExecChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.width(60.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = read, onCheckedChange = onReadChange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("R", style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = write, onCheckedChange = onWriteChange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("W", style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = exec, onCheckedChange = onExecChange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("X", style = MaterialTheme.typography.labelMedium)
        }
    }
}
