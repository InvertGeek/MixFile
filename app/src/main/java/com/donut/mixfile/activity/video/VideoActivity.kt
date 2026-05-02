package com.donut.mixfile.activity.video

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import com.donut.mixfile.activity.video.player.VideoPlayerScreen
import com.donut.mixfile.ui.theme.MainTheme
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.objects.MixActivity
import kotlinx.serialization.Serializable


var playHistory by cachedMutableOf(listOf<VideoHistory>(), "video_player_history_v2")

@Serializable
data class VideoHistory(val time: Long, val hash: String, val episode: Int)

class VideoActivity : MixActivity("video") {

    companion object {
        var videoList = listOf<Uri>()
        var videoHash = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterFullScreen()
        // 设置保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                if (videoList.isEmpty()) {
                    Text(text = "视频url为空")
                    return@MainTheme
                }
                VideoPlayerScreen(
                    videoUris = videoList,
                    hash = videoHash
                )
                videoList = listOf()
                videoHash = ""
                return@MainTheme
            }
        }
    }

    private fun enterFullScreen() {
        val decorView = window.decorView
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}

