package com.estrongs.android.pop.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.ESAccentOrange
import com.estrongs.android.pop.util.StoragePermissionHelper
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ESImageViewerDialog(
    file: FileItem,
    folderImages: List<FileItem> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateImage: (FileItem) -> Unit = {},
    onDeleteImage: (FileItem) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }

    var showControls by remember { mutableStateOf(true) }
    var showExifDialog by remember { mutableStateOf(false) }
    var isSlideshowActive by remember { mutableStateOf(false) }
    var slideshowIntervalSec by remember { mutableIntStateOf(3) }

    val context = LocalContext.current

    // Current index in folder images
    val currentIndex = remember(file, folderImages) {
        folderImages.indexOfFirst { it.path == file.path }
    }

    // Reset zoom and rotation when current image changes
    LaunchedEffect(file.path) {
        scale = 1f
        offset = Offset.Zero
        rotationAngle = 0f
        flipHorizontal = false
        flipVertical = false
    }

    // Slideshow loop
    LaunchedEffect(isSlideshowActive, folderImages, currentIndex) {
        if (isSlideshowActive && folderImages.isNotEmpty()) {
            while (true) {
                delay(slideshowIntervalSec * 1000L)
                val nextIdx = (currentIndex + 1) % folderImages.size
                onNavigateImage(folderImages[nextIdx])
            }
        }
    }

    // Image dimensions
    val imageDimensions = remember(file.path) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, options)
            "${options.outWidth} × ${options.outHeight}"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("es_image_viewer_dialog")
        ) {
            // Main Interactive Zoomable & Pannable Image Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(file.path) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                            if (scale > 1f) {
                                val maxOffset = (scale - 1) * 600f
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                    (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(file.path),
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale * (if (flipHorizontal) -1f else 1f),
                            scaleY = scale * (if (flipVertical) -1f else 1f),
                            rotationZ = rotationAngle,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .testTag("image_viewer_canvas")
                )
            }

            // Top Header Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0x99000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("image_viewer_close_btn")) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (folderImages.isNotEmpty()) "${currentIndex + 1} / ${folderImages.size} • $imageDimensions • ${file.formattedSize}" else "$imageDimensions • ${file.formattedSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Slideshow toggle
                            IconButton(
                                onClick = { isSlideshowActive = !isSlideshowActive },
                                modifier = Modifier.testTag("image_viewer_slideshow_btn")
                            ) {
                                Icon(
                                    if (isSlideshowActive) Icons.Default.PauseCircle else Icons.Default.Slideshow,
                                    contentDescription = "Slideshow",
                                    tint = if (isSlideshowActive) ESAccentOrange else Color.White
                                )
                            }

                            // EXIF Info
                            IconButton(
                                onClick = { showExifDialog = true },
                                modifier = Modifier.testTag("image_viewer_info_btn")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "EXIF Details", tint = Color.White)
                            }

                            // Share
                            IconButton(
                                onClick = { StoragePermissionHelper.shareFile(context, File(file.path)) },
                                modifier = Modifier.testTag("image_viewer_share_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Left / Right Navigation Arrows if multiple images in folder
            if (folderImages.size > 1 && showControls) {
                // Previous
                IconButton(
                    onClick = {
                        val prevIdx = if (currentIndex <= 0) folderImages.size - 1 else currentIndex - 1
                        onNavigateImage(folderImages[prevIdx])
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .testTag("image_viewer_prev_btn")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Image", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Next
                IconButton(
                    onClick = {
                        val nextIdx = (currentIndex + 1) % folderImages.size
                        onNavigateImage(folderImages[nextIdx])
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .testTag("image_viewer_next_btn")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Image", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            // Bottom Image Transform & Tool Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color(0x99000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotate Left
                        IconButton(
                            onClick = { rotationAngle = (rotationAngle - 90f) % 360f },
                            modifier = Modifier.testTag("image_rotate_left_btn")
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left 90°", tint = Color.White)
                        }

                        // Rotate Right
                        IconButton(
                            onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                            modifier = Modifier.testTag("image_rotate_right_btn")
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right 90°", tint = Color.White)
                        }

                        // Flip Horizontal
                        IconButton(
                            onClick = { flipHorizontal = !flipHorizontal },
                            modifier = Modifier.testTag("image_flip_h_btn")
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = if (flipHorizontal) ESAccentOrange else Color.White)
                        }

                        // Reset View
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                                rotationAngle = 0f
                                flipHorizontal = false
                                flipVertical = false
                            },
                            modifier = Modifier.testTag("image_reset_zoom_btn")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom & Orientation", tint = Color.White)
                        }

                        // Delete
                        IconButton(
                            onClick = {
                                onDeleteImage(file)
                            },
                            modifier = Modifier.testTag("image_viewer_delete_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Photo", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    // EXIF Metadata Inspector Dialog
    if (showExifDialog) {
        AlertDialog(
            onDismissRequest = { showExifDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = ESAccentOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Image Details & EXIF")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExifRow("File Name", file.name)
                    ExifRow("Resolution", imageDimensions)
                    ExifRow("File Size", "${file.formattedSize} (${file.size} bytes)")
                    ExifRow("Format", file.extension.uppercase())
                    ExifRow("Path", file.path)
                    ExifRow("Last Modified", file.formattedDate)
                }
            },
            confirmButton = {
                Button(onClick = { showExifDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ExifRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = ESAccentOrange, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
