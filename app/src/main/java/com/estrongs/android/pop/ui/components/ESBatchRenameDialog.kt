package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.ESAccentOrange
import com.estrongs.android.pop.ui.theme.ESBlue

enum class RenameMode(val label: String) {
    NUMBERING("Numbering Sequence"),
    FIND_REPLACE("Find & Replace"),
    PREFIX_SUFFIX("Prefix / Suffix"),
    CASE_CONVERT("Letter Case"),
    EXTENSION("Extension Change")
}

@Composable
fun ESBatchRenameDialog(
    selectedFiles: List<FileItem>,
    onDismiss: () -> Unit,
    onConfirmBatchRename: (Map<FileItem, String>) -> Unit
) {
    var mode by remember { mutableStateOf(RenameMode.NUMBERING) }

    // Numbering mode states
    var baseName by remember { mutableStateOf("File_") }
    var startNumber by remember { mutableIntStateOf(1) }
    var digitPadding by remember { mutableIntStateOf(3) }
    var keepOriginalExtension by remember { mutableStateOf(true) }

    // Find & replace states
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }

    // Prefix / Suffix states
    var prefixText by remember { mutableStateOf("") }
    var suffixText by remember { mutableStateOf("") }

    // Case convert states
    var caseOption by remember { mutableIntStateOf(0) } // 0: lowercase, 1: UPPERCASE, 2: Capitalize Words

    // Extension state
    var newExtension by remember { mutableStateOf("") }

    // Calculate preview map
    val previewMap = remember(
        mode, selectedFiles, baseName, startNumber, digitPadding, keepOriginalExtension,
        findText, replaceText, matchCase, prefixText, suffixText, caseOption, newExtension
    ) {
        val result = mutableMapOf<FileItem, String>()
        selectedFiles.forEachIndexed { index, file ->
            val ext = if (file.isDirectory) "" else file.extension
            val nameWithoutExt = if (file.isDirectory || ext.isEmpty()) file.name else file.name.substringBeforeLast(".")

            val newName = when (mode) {
                RenameMode.NUMBERING -> {
                    val num = startNumber + index
                    val formattedNum = String.format("%0${digitPadding}d", num)
                    val extPart = if (keepOriginalExtension && ext.isNotEmpty()) ".$ext" else ""
                    "$baseName$formattedNum$extPart"
                }
                RenameMode.FIND_REPLACE -> {
                    if (findText.isEmpty()) {
                        file.name
                    } else {
                        val regex = if (matchCase) Regex.fromLiteral(findText) else Regex(Regex.escape(findText), RegexOption.IGNORE_CASE)
                        file.name.replace(regex, replaceText)
                    }
                }
                RenameMode.PREFIX_SUFFIX -> {
                    val extPart = if (ext.isNotEmpty()) ".$ext" else ""
                    "$prefixText$nameWithoutExt$suffixText$extPart"
                }
                RenameMode.CASE_CONVERT -> {
                    val converted = when (caseOption) {
                        0 -> nameWithoutExt.lowercase()
                        1 -> nameWithoutExt.uppercase()
                        else -> nameWithoutExt.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    }
                    val extPart = if (ext.isNotEmpty()) ".${if (caseOption == 1) ext.uppercase() else ext.lowercase()}" else ""
                    "$converted$extPart"
                }
                RenameMode.EXTENSION -> {
                    val cleanExt = newExtension.removePrefix(".")
                    if (cleanExt.isEmpty() || file.isDirectory) {
                        file.name
                    } else {
                        "$nameWithoutExt.$cleanExt"
                    }
                }
            }
            result[file] = newName
        }
        result
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("es_batch_rename_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = ESBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ES Batch Rename",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedFiles.size} files selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Tabs
                ScrollableTabRow(
                    selectedTabIndex = RenameMode.values().indexOf(mode),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RenameMode.values().forEach { m ->
                        Tab(
                            selected = mode == m,
                            onClick = { mode = m },
                            text = { Text(m.label, fontSize = 11.sp, fontWeight = if (mode == m) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode-specific configuration inputs
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (mode) {
                            RenameMode.NUMBERING -> {
                                OutlinedTextField(
                                    value = baseName,
                                    onValueChange = { baseName = it },
                                    label = { Text("Base Prefix (e.g. Photo_)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("batch_base_name_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = startNumber.toString(),
                                        onValueChange = { startNumber = it.toIntOrNull() ?: 1 },
                                        label = { Text("Start Index") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("batch_start_num_input")
                                    )
                                    OutlinedTextField(
                                        value = digitPadding.toString(),
                                        onValueChange = { digitPadding = (it.toIntOrNull() ?: 3).coerceIn(1, 8) },
                                        label = { Text("Digits (e.g. 3 -> 001)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("batch_digits_input")
                                    )
                                }
                            }
                            RenameMode.FIND_REPLACE -> {
                                OutlinedTextField(
                                    value = findText,
                                    onValueChange = { findText = it },
                                    label = { Text("Find substring") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("batch_find_input")
                                )
                                OutlinedTextField(
                                    value = replaceText,
                                    onValueChange = { replaceText = it },
                                    label = { Text("Replace with") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("batch_replace_input")
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = matchCase, onCheckedChange = { matchCase = it })
                                    Text("Match case", fontSize = 12.sp)
                                }
                            }
                            RenameMode.PREFIX_SUFFIX -> {
                                OutlinedTextField(
                                    value = prefixText,
                                    onValueChange = { prefixText = it },
                                    label = { Text("Add to beginning (Prefix)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("batch_prefix_input")
                                )
                                OutlinedTextField(
                                    value = suffixText,
                                    onValueChange = { suffixText = it },
                                    label = { Text("Add to end (Suffix)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("batch_suffix_input")
                                )
                            }
                            RenameMode.CASE_CONVERT -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = caseOption == 0,
                                        onClick = { caseOption = 0 },
                                        label = { Text("lowercase") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = caseOption == 1,
                                        onClick = { caseOption = 1 },
                                        label = { Text("UPPERCASE") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = caseOption == 2,
                                        onClick = { caseOption = 2 },
                                        label = { Text("Title Case") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            RenameMode.EXTENSION -> {
                                OutlinedTextField(
                                    value = newExtension,
                                    onValueChange = { newExtension = it },
                                    label = { Text("New Extension (e.g. jpg, txt, png)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("batch_ext_input")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Table
                Text(
                    text = "Live Rename Preview (${previewMap.size} items):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(selectedFiles) { idx, file ->
                        val newName = previewMap[file] ?: file.name
                        val hasChanged = newName != file.name

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (hasChanged) ESAccentOrange else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = newName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasChanged) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (idx < selectedFiles.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("batch_rename_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmBatchRename(previewMap) },
                        modifier = Modifier.testTag("batch_rename_apply_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ESBlue)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Rename")
                    }
                }
            }
        }
    }
}
