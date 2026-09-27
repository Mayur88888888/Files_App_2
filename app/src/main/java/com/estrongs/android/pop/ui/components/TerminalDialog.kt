package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.estrongs.android.pop.ui.theme.ESAccentGreen
import com.estrongs.android.pop.ui.theme.ESAccentOrange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalDialog(
    terminalOutput: String,
    isExecuting: Boolean,
    isRootMode: Boolean,
    onExecute: (cmd: String, useRoot: Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    var useRoot by remember { mutableStateOf(isRootMode) }
    val scrollState = rememberScrollState()

    // Auto-scroll when output updates
    LaunchedEffect(terminalOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (useRoot) ESAccentGreen else MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Inner System Shell & Terminal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (useRoot) "Elevated Mode (# root)" else "Standard Mode ($ user)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (useRoot) ESAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onClear, modifier = Modifier.testTag("terminal_clear_btn")) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Output")
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("terminal_close_btn")) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Command Presets
                Text(
                    text = "Quick OS Commands:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickCmds = listOf("df -h", "mount", "cat /proc/version", "cat /proc/cpuinfo", "cat /proc/meminfo", "ls -la /system", "uptime")
                    quickCmds.forEach { qc ->
                        SuggestionChip(
                            onClick = {
                                command = qc
                                onExecute(qc, useRoot)
                            },
                            label = { Text(qc, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Black Console Output Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = terminalOutput,
                        color = Color(0xFF00FF66),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier.testTag("terminal_output_text")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode toggle & Command Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = useRoot,
                        onClick = { useRoot = !useRoot },
                        label = { Text(if (useRoot) "SU / Root" else "Normal Shell") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = if (useRoot) ESAccentGreen else ESAccentOrange
                            )
                        },
                        modifier = Modifier.testTag("terminal_root_chip")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_cmd_input"),
                        placeholder = { Text("e.g. ls -la /storage", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    Button(
                        onClick = {
                            if (command.isNotBlank()) {
                                onExecute(command, useRoot)
                                command = ""
                            }
                        },
                        enabled = command.isNotBlank() && !isExecuting,
                        modifier = Modifier.testTag("terminal_run_btn")
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run")
                        }
                    }
                }
            }
        }
    }
}
