package com.donut.mixfile.activity.video.player

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList


@Composable
fun BottomControl(
    visible: Boolean,
    modifier: Modifier,
    player: ExoPlayer,
    videos: List<Uri>,
    progress: Float,
    onTrackTimeChange: (Float) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(5.dp),
        ) {
            // 1. 进度条：修复了回调逻辑，通过 onTrackTimeChange 同步状态
            PlayerProgressSlider(
                progress = progress,
                onSeek = { newValue ->
                    player.seekTo((player.duration * newValue).toLong())
                    onTrackTimeChange(newValue)
                },
                onSeekFinished = { player.play() }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：播放控制与时间
                Row(
                    modifier = Modifier.padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    var lastSeek = remember { System.currentTimeMillis() }
                    if (player.mediaItemCount > 1) {
                        IconButton(
                            modifier = Modifier.scale(1f),
                            onClick = {
                                if (System.currentTimeMillis() - lastSeek < 500) {
                                    return@IconButton
                                }
                                lastSeek = System.currentTimeMillis()
                                player.seekToPreviousMediaItem()
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(100.dp),
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            modifier = Modifier.scale(1f),
                            onClick = {
                                if (System.currentTimeMillis() - lastSeek < 500) {
                                    return@IconButton
                                }
                                lastSeek = System.currentTimeMillis()
                                player.seekToNextMediaItem()
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(100.dp),
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = "${formatTime((player.duration * progress).toLong())}/${
                            formatTime(
                                player.duration.coerceAtLeast(0)
                            )
                        }",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                // 右侧：轨道、选集、速度设置
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    val tracks = player.currentTracks
                    // 2. 音轨 (支持去重)
                    val audioGroups =
                        remember(tracks) { tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO } }
                    if (audioGroups.size > 1) {
                        PlayerSettingChip("音轨") {
                            showTrackSelector(
                                title = "选择音轨",
                                player = player,
                                trackType = C.TRACK_TYPE_AUDIO,
                                colorScheme = playerColorScheme,
                                hasDisableOption = false
                            )
                        }
                    }

                    // 字幕选择
                    val textGroups = remember(tracks) {
                        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                    }

                    if (textGroups.isNotEmpty()) {
                        PlayerSettingChip("字幕") {
                            showTrackSelector(
                                title = "选择字幕",
                                player = player,
                                trackType = C.TRACK_TYPE_TEXT,
                                colorScheme = playerColorScheme,
                                hasDisableOption = true
                            )
                        }
                    }

                    // 选集选择
                    if (player.mediaItemCount > 1) {
                        PlayerSettingChip("选集") {
                            val indexMap = videos.mapIndexed { index, uri -> index to uri }
                            MixDialogBuilder("选集", colorScheme = playerColorScheme).apply {
                                setContent {
                                    SingleSelectItemList(
                                        indexMap,
                                        currentOption = indexMap.getOrNull(player.currentMediaItemIndex),
                                        getLabel = { "${it.first + 1} - ${it.second.fragment ?: "未命名"}" }
                                    ) {
                                        player.seekToDefaultPosition(it.first)
                                        closeDialog()
                                    }
                                }
                                show()
                            }
                        }
                    }

                    // 速度选择
                    val currentSpeed = player.playbackParameters.speed

                    PlayerSettingChip("速度: ${currentSpeed}x") {
                        MixDialogBuilder("播放速度", colorScheme = playerColorScheme).apply {
                            setContent {
                                val speeds = listOf(
                                    "0.25",
                                    "0.5",
                                    "0.75",
                                    "1.0",
                                    "1.25",
                                    "1.5",
                                    "1.75",
                                    "2.0",
                                    "2.5",
                                    "3.0"
                                )
                                SingleSelectItemList(speeds, currentOption = "$currentSpeed") {
                                    player.setPlaybackSpeed(it.toFloat())
                                    closeDialog()
                                }
                            }
                            show()
                        }
                    }
                }
            }
        }
    }
}