package com.donut.mixfile.activity.video.player

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList


@OptIn(ExperimentalMaterial3Api::class)
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
        enter = slideInVertically(
            initialOffsetY = { it } // 从顶部（负方向）滑入
        ),
        exit = slideOutVertically(
            targetOffsetY = { it } // 向上（负方向）滑出
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(5.dp),
        ) {


            Slider(
                value = progress,
                onValueChange = { newValue ->
                    player.pause()
                    player.seekTo((player.duration * newValue).toLong())
                    onTrackTimeChange(newValue)
                },
                onValueChangeFinished = {
                    player.play()
                },
                modifier = Modifier
                    .fillMaxWidth(),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(15.dp) // 自定义滑块大小
                                .align(Alignment.Center)
                                .background(Color.White, CircleShape)
                        )
                    }
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState
                    )
                },
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
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
                    } else {
                        Row(modifier = Modifier.width(10.dp)) { }
                    }
                    Text(
                        text = "${formatTime((player.duration * progress).toLong())}/${
                            formatTime(
                                player.duration.coerceAtLeast(
                                    0
                                )
                            )
                        }",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    if (player.mediaItemCount > 1) {
                        AssistChip(
                            onClick = {
                                MixDialogBuilder(
                                    "选集",
                                    scheme = playerColorScheme
                                ).apply {
                                    setContent {
                                        SingleSelectItemList(
                                            videos,
                                            currentOption = videos[player.currentMediaItemIndex],
                                            getLabel = {
                                                "${videos.indexOf(it) + 1} - ${it.fragment}"
                                            }
                                        ) {
                                            player.seekToDefaultPosition(videos.indexOf(it))
                                            closeDialog()
                                        }
                                    }
                                    setDefaultNegative("取消")
                                    show()
                                }
                            },
                            label = {
                                Text(
                                    fontWeight = FontWeight.Bold,
                                    text = "选集",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }

                    val currentSpeed = player.playbackParameters.speed

                    AssistChip(
                        onClick = {
                            MixDialogBuilder(
                                "播放速度",
                                scheme = playerColorScheme
                            ).apply {
                                setContent {
                                    SingleSelectItemList(
                                        listOf(
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
                                        ), currentOption = "${currentSpeed}"
                                    ) {
                                        player.setPlaybackSpeed(it.toFloat())
                                        closeDialog()
                                    }
                                }
                                setDefaultNegative("取消")
                                show()
                            }
                        },
                        label = {
                            Text(
                                fontWeight = FontWeight.Bold,
                                text = "速度: ${currentSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    )
                }


            }

        }
    }
}
