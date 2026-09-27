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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.ESAccentGreen
import com.estrongs.android.pop.ui.theme.ESBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.CRC32

@Composable
fun ESChecksumDialog(
    file: FileItem,
    onDismiss: () -> Unit
) {
    var md5Hash by remember { mutableStateOf<String?>("Calculating...") }
    var sha1Hash by remember { mutableStateOf<String?>("Calculating...") }
    var sha256Hash by remember { mutableStateOf<String?>("Calculating...") }
    var crc32Hash by remember { mutableStateOf<String?>("Calculating...") }

    var verifyInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var copiedLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file.path) {
        withContext(Dispatchers.IO) {
            try {
                val f = File(file.path)
                if (f.exists() && f.isFile) {
                    val md5Digest = MessageDigest.getInstance("MD5")
                    val sha1Digest = MessageDigest.getInstance("SHA-1")
                    val sha256Digest = MessageDigest.getInstance("SHA-256")
                    val crc = CRC32()

                    val buffer = ByteArray(8192)
                    FileInputStream(f).use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            md5Digest.update(buffer, 0, read)
                            sha1Digest.update(buffer, 0, read)
                            sha256Digest.update(buffer, 0, read)
                            crc.update(buffer, 0, read)
                        }
                    }

                    md5Hash = md5Digest.digest().joinToString("") { "%02x".format(it) }
                    sha1Hash = sha1Digest.digest().joinToString("") { "%02x".format(it) }
                    sha256Hash = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                    crc32Hash = "%08X".format(crc.value)
                } else {
                    md5Hash = "N/A (Directory)"
                    sha1Hash = "N/A (Directory)"
                    sha256Hash = "N/A (Directory)"
                    crc32Hash = "N/A (Directory)"
                }
            } catch (e: Exception) {
                md5Hash = "Error: ${e.localizedMessage}"
                sha1Hash = "Error"
                sha256Hash = "Error"
                crc32Hash = "Error"
            }
        }
    }

    // Check if verification string matches any hash
    val cleanVerify = verifyInput.trim().lowercase()
    val matchAlgorithm = remember(cleanVerify, md5Hash, sha1Hash, sha256Hash, crc32Hash) {
        if (cleanVerify.isBlank()) null
        else when {
            cleanVerify == md5Hash?.lowercase() -> "MD5"
            cleanVerify == sha1Hash?.lowercase() -> "SHA-1"
            cleanVerify == sha256Hash?.lowercase() -> "SHA-256"
            cleanVerify == crc32Hash?.lowercase() -> "CRC32"
            else -> "MISMATCH"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("es_checksum_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ESBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "File Checksum & Hash",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${file.name} (${file.formattedSize})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Verification Input
                OutlinedTextField(
                    value = verifyInput,
                    onValueChange = { verifyInput = it },
                    label = { Text("Paste expected hash to verify") },
                    placeholder = { Text("e.g. 5d41402abc4b2a76b9719d911017c592") },
                    singleLine = true,
                    trailingIcon = {
                        if (verifyInput.isNotEmpty()) {
                            IconButton(onClick = { verifyInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("checksum_verify_input")
                )

                // Verification Result Badge
                if (matchAlgorithm != null) {
                    val isMatch = matchAlgorithm != "MISMATCH"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMatch) ESAccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isMatch) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isMatch) ESAccentGreen else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMatch) "Verified: Matches $matchAlgorithm hash!" else "Hash mismatch! File content does not match input.",
                                color = if (isMatch) ESAccentGreen else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Hashes List
                HashCard(
                    title = "MD5",
                    hash = md5Hash ?: "Calculating...",
                    onCopy = {
                        md5Hash?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            copiedLabel = "MD5"
                        }
                    },
                    testTag = "hash_md5"
                )

                HashCard(
                    title = "SHA-1",
                    hash = sha1Hash ?: "Calculating...",
                    onCopy = {
                        sha1Hash?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            copiedLabel = "SHA-1"
                        }
                    },
                    testTag = "hash_sha1"
                )

                HashCard(
                    title = "SHA-256",
                    hash = sha256Hash ?: "Calculating...",
                    onCopy = {
                        sha256Hash?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            copiedLabel = "SHA-256"
                        }
                    },
                    testTag = "hash_sha256"
                )

                HashCard(
                    title = "CRC-32",
                    hash = crc32Hash ?: "Calculating...",
                    onCopy = {
                        crc32Hash?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            copiedLabel = "CRC32"
                        }
                    },
                    testTag = "hash_crc32"
                )

                if (copiedLabel != null) {
                    Text(
                        text = "Copied $copiedLabel to clipboard!",
                        color = ESAccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("checksum_close_btn")
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun HashCard(
    title: String,
    hash: String,
    onCopy: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ESBlue
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy $title", modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = hash,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
