package com.estrongs.android.pop.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.estrongs.android.pop.data.model.FileItem
import com.estrongs.android.pop.ui.theme.ESAccentOrange
import com.estrongs.android.pop.ui.theme.ESAccentPurple
import com.estrongs.android.pop.ui.theme.ESBlue
import java.io.File
import kotlin.random.Random

enum class LoopMode {
    SEQUENTIAL,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE
}

data class AudioPlayerState(
    val currentTrack: FileItem? = null,
    val playlist: List<FileItem> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0,
    val loopMode: LoopMode = LoopMode.REPEAT_ALL,
    val isExpanded: Boolean = false
)

private fun formatTime(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun ESMiniPlayerBar(
    state: AudioPlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable { onExpand() }
                .testTag("es_mini_player_bar"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                // Mini progress line on top
                val progress = if (state.totalDurationMs > 0) {
                    (state.currentPositionMs.toFloat() / state.totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = ESAccentPurple,
                    trackColor = Color.Transparent
                )

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
                        // Spinning mini vinyl or music icon
                        val infiniteTransition = rememberInfiniteTransition(label = "mini_disc")
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(4000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "mini_disc_rotation"
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(ESAccentPurple, Color(0xFF1E1B4B))
                                    )
                                )
                                .then(if (state.isPlaying) Modifier.rotate(angle) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatTime(state.currentPositionMs)} / ${formatTime(state.totalDurationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = ESAccentPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.testTag("mini_player_next")
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("mini_player_close")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Player",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ESAudioPlayerDialog(
    state: AudioPlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onToggleLoopMode: () -> Unit,
    onSelectTrack: (FileItem) -> Unit,
    onDismiss: () -> Unit
) {
    val track = state.currentTrack ?: return
    var showPlaylist by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("es_audio_player_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("audio_player_collapse_btn")
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ES Media Player",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "PLAYING FROM STORAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ESAccentPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { showPlaylist = !showPlaylist },
                        modifier = Modifier.testTag("audio_player_playlist_btn")
                    ) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = "Playlist",
                            tint = if (showPlaylist) ESAccentPurple else Color.White
                        )
                    }
                }

                if (showPlaylist) {
                    // Playlist View
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Playlist (${state.playlist.size} tracks)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(state.playlist) { idx, item ->
                                    val isCurrent = item.path == track.path
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) ESAccentPurple.copy(alpha = 0.25f) else Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectTrack(item) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}.",
                                                color = if (isCurrent) ESAccentPurple else Color.Gray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Icon(
                                                if (isCurrent && state.isPlaying) Icons.Default.Equalizer else Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = if (isCurrent) ESAccentPurple else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.name,
                                                color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Center Big Vinyl Art with Spinning Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(6000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "vinyl_angle"
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF3B0764),
                                        Color(0xFF1E1B4B),
                                        Color(0xFF090D16)
                                    )
                                )
                            )
                            .then(if (state.isPlaying) Modifier.rotate(rotationAngle) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vinyl Grooves
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1B4B).copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ESAccentPurple, ESAccentOrange)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    // Simulated Audio Visualizer Frequency Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (i in 0 until 18) {
                            val barHeight = if (state.isPlaying) {
                                val anim = rememberInfiniteTransition(label = "bar_$i")
                                val h by anim.animateFloat(
                                    initialValue = 4f,
                                    targetValue = (12f + (i * 7) % 24f).coerceIn(6f, 32f),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(250 + (i * 45) % 300, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "bar_height_$i"
                                )
                                h.dp
                            } else 4.dp

                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(ESAccentPurple, ESAccentOrange)
                                        )
                                    )
                            )
                        }
                    }

                    // Track Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${track.extension.uppercase()} Audio • ${track.formattedSize}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Controls & Seekbar Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Seekbar
                    val currentProgress = if (state.totalDurationMs > 0) {
                        (state.currentPositionMs.toFloat() / state.totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = currentProgress,
                        onValueChange = { frac ->
                            val targetMs = (frac * state.totalDurationMs).toInt()
                            onSeekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = ESAccentPurple,
                            activeTrackColor = ESAccentPurple,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("audio_seekbar")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(state.currentPositionMs),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = formatTime(state.totalDurationMs),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    // Playback Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Loop mode button
                        IconButton(
                            onClick = onToggleLoopMode,
                            modifier = Modifier.testTag("audio_loop_mode_btn")
                        ) {
                            Icon(
                                when (state.loopMode) {
                                    LoopMode.SEQUENTIAL -> Icons.Default.TrendingFlat
                                    LoopMode.REPEAT_ALL -> Icons.Default.Repeat
                                    LoopMode.REPEAT_ONE -> Icons.Default.RepeatOne
                                    LoopMode.SHUFFLE -> Icons.Default.Shuffle
                                },
                                contentDescription = "Loop Mode",
                                tint = if (state.loopMode != LoopMode.SEQUENTIAL) ESAccentPurple else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Previous
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(48.dp).testTag("audio_prev_btn")
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Big Play / Pause Button
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(64.dp).testTag("audio_play_pause_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ESAccentPurple)
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(48.dp).testTag("audio_next_btn")
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Close Dialog
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("audio_close_btn")
                        ) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "Close Player",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
