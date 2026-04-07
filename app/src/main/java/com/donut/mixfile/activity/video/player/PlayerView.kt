package com.donut.mixfile.activity.video.player


import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.donut.mixfile.activity.video.VideoHistory
import com.donut.mixfile.activity.video.playHistory
import com.donut.mixfile.ui.theme.mainColorScheme
import com.donut.mixfile.util.ForceUpdateMutable
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.delay
import java.util.Locale

fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

val playerColorScheme
    get() = mainColorScheme.copy(
        onSurfaceVariant = Color.White.copy(0.8f),
        surface = Color.Black.copy(0.3f),
        onSurface = Color.White.copy(0.8f),
        onSecondaryContainer = mainColorScheme.primary.copy(0.8f)
    )

@Composable
fun VideoPlayerScreen(
    videoUris: List<Uri>,
    hash: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(videoUris.map { MediaItem.fromUri(it) })
            repeatMode = REPEAT_MODE_ALL
            val cached = playHistory.firstOrNull { it.hash.contentEquals(hash) }
            if (cached != null) {
                setMediaItems(
                    videoUris.map { MediaItem.fromUri(it) },
                    cached.episode,
                    (cached.time - 2000L).coerceAtLeast(0)
                )
                showToast("已跳转到上次播放位置", length = Toast.LENGTH_SHORT)
            }
            prepare()
            playWhenReady = true
        }
    }

    var currentMediaItem by remember { mutableIntStateOf(player.currentMediaItemIndex) }

    val controlsVisible = remember { ForceUpdateMutable(true) }


    // 控制栏自动隐藏
    LaunchedEffect(controlsVisible.inc) {
        if (controlsVisible.get) {
            delay(3000)
            controlsVisible.set(false)
        }
    }

    val lifecycleOwner =
        LocalLifecycleOwner.current


    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    player.play()
                }

                else -> {}
            }
        }

        val lifecycle = lifecycleOwner.lifecycle

        lifecycle.addObserver(observer)

        onDispose {
            player.release()
            lifecycle.removeObserver(observer)
        }
    }

    var lastClick by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (System.currentTimeMillis() - lastClick < 300L) {
                            if (player.isPlaying) {
                                player.pause()
                                controlsVisible.set(true)
                            } else {
                                player.play()
                                controlsVisible.set(false)
                            }
                            return@detectTapGestures
                        }
                        lastClick = System.currentTimeMillis()
                        controlsVisible.set(!controlsVisible.get)
                    }
                )
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        val currentMediaTitle = videoUris[currentMediaItem].fragment ?: ""

        TopControl(
            title = if (videoUris.size > 1) "${currentMediaItem + 1} - ${currentMediaTitle}" else currentMediaTitle,
            visible = controlsVisible.get,
            modifier = Modifier.align(Alignment.TopCenter)
        )


        CenterControl(
            controlsVisible.get,
            Modifier.align(Alignment.Center),
            player,
            onClick = {
                controlsVisible.set(true)
            }
        ) {
            currentMediaItem = player.currentMediaItemIndex
            controlsVisible.set(true)
        }

        var progress by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(player) {
            while (true) {
                progress = if (player.duration > 0) {
                    player.currentPosition.toFloat() / player.duration
                } else 0f
                playHistory =
                    playHistory.filter { !it.hash.contentEquals(hash) }.toMutableList().apply {
                        val history =
                            VideoHistory(player.currentPosition, hash, player.currentMediaItemIndex)
                        add(0, history)
                        if (size > 500) {
                            removeAt(lastIndex)
                        }
                    }
                delay(1000)
            }

        }


        BottomControl(
            visible = controlsVisible.get,
            modifier = Modifier.align(Alignment.BottomCenter),
            player = player,
            videos = videoUris,
            progress = progress,
            onTrackTimeChange = {
                progress = it
                controlsVisible.set(true)
            }
        )
    }
}
