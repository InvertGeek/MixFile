package com.donut.mixfile.activity.video.player


import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.donut.mixfile.activity.video.VideoHistory
import com.donut.mixfile.activity.video.playHistory
import com.donut.mixfile.ui.theme.mainColorScheme
import com.donut.mixfile.util.ForceUpdateMutable
import com.donut.mixfile.util.showErrorDialog
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

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUris: List<Uri>,
    hash: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember {
        // 1. 创建渲染器工厂
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        }


        val extractorsFactory = DefaultExtractorsFactory().apply {
            // 开启对所有可能的容器支持
            setConstantBitrateSeekingEnabled(true) // 允许对没有索引的流进行粗略进度拖动
            setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
        }

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context, extractorsFactory))
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        showErrorDialog(error, "播放出错", playerColorScheme)
                    }
                })
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
                    this.useController = false

                    // 1. 获取 SubtitleView 引用
                    subtitleView?.apply {
                        // 2. 禁用内嵌样式（强制使用自定义样式，防止 .ass 字幕干扰）
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)

                        // 3. 设置样式：白色文字、黑色半透明背景、圆角或阴影
                        val customStyle = CaptionStyleCompat(
                            android.graphics.Color.WHITE,               // 字体颜色
                            android.graphics.Color.TRANSPARENT,         // 背景颜色
                            android.graphics.Color.TRANSPARENT,         // 窗口颜色
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE, // 边缘样式
                            android.graphics.Color.BLACK,               // 边缘颜色
                            Typeface.DEFAULT      // 粗体
                        )
                        setStyle(customStyle)

                        // 4. 设置字体大小：0.05f 代表字体高度约占屏幕高度的 5%
                        setFractionalTextSize(0.05f)

                        // 5. 调整边距：防止字幕太靠底
                        setBottomPaddingFraction(0.08f)
                    }
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
