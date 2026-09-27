package com.estrongs.android.pop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrongs.android.pop.ui.theme.ESAccentGreen
import com.estrongs.android.pop.ui.theme.ESAccentOrange
import com.estrongs.android.pop.ui.theme.ESBlue

@Composable
fun RootSettingsDialog(
    isRootMode: Boolean,
    isRootAvailable: Boolean,
    isRootGranted: Boolean,
    onToggleRootMode: () -> Unit,
    onRemount: (partition: String, rw: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = if (isRootMode) ESAccentGreen else ESAccentOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Root Explorer Settings")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SU Binary Detected:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (isRootAvailable) "Yes (su ready)" else "Emulated Elevated Shell",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRootAvailable) ESAccentGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Elevated Mode:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (isRootMode) "ACTIVE (Temporary RW)" else "INACTIVE (Safe RO)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRootMode) ESAccentGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Root Explorer", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Browse & edit inner Android system files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isRootMode,
                        onCheckedChange = { onToggleRootMode() },
                        modifier = Modifier.testTag("root_dialog_toggle_switch")
                    )
                }

                HorizontalDivider()

                Text("Mount System Partitions R/W", fontWeight = FontWeight.SemiBold)

                PartitionMountRow(
                    name = "/system",
                    onMountRw = { onRemount("/system", true) },
                    onMountRo = { onRemount("/system", false) }
                )

                PartitionMountRow(
                    name = "/data",
                    onMountRw = { onRemount("/data", true) },
                    onMountRo = { onRemount("/data", false) }
                )

                PartitionMountRow(
                    name = "/ (root)",
                    onMountRw = { onRemount("/", true) },
                    onMountRo = { onRemount("/", false) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("root_dialog_done_btn")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PartitionMountRow(
    name: String,
    onMountRw: () -> Unit,
    onMountRo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = onMountRo,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("RO", fontSize = 11.sp)
            }
            Button(
                onClick = onMountRw,
                colors = ButtonDefaults.buttonColors(containerColor = ESBlue),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("RW", fontSize = 11.sp)
            }
        }
    }
}
