package com.donut.mixfile.activity.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList

object TrackUtils {
    /**
     * 获取格式化后的轨道列表，处理重复 Label
     */
    fun getFormattedTracks(
        groups: List<Tracks.Group>,
        defaultPrefix: String = "轨道"
    ): List<TrackInfo> {
        val options = mutableListOf<TrackInfo>()
        val labelCounter = mutableMapOf<String, Int>()

        groups.forEach { group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val baseLabel = format.label ?: format.language ?: defaultPrefix

                val count = labelCounter.getOrDefault(baseLabel, 0) + 1
                labelCounter[baseLabel] = count
                val finalLabel = if (count > 1) "$baseLabel-$count" else baseLabel

                options.add(
                    TrackInfo(
                        finalLabel,
                        group.mediaTrackGroup,
                        i,
                        group.isSelected && group.isTrackSelected(i)
                    )
                )
            }
        }
        return options
    }

    data class TrackInfo(
        val label: String,
        val group: TrackGroup,
        val index: Int,
        val isSelected: Boolean
    )
}

/**
 * 显示字幕/音轨选择弹窗
 */
fun showTrackSelector(
    title: String,
    player: ExoPlayer,
    trackType: @C.TrackType Int,
    colorScheme: ColorScheme,
    hasDisableOption: Boolean = false
) {
    val groups = player.currentTracks.groups.filter { it.type == trackType }
    val trackInfos = TrackUtils.getFormattedTracks(
        groups,
        if (trackType == C.TRACK_TYPE_TEXT) "字幕" else "音轨"
    )

    val options =
        if (hasDisableOption) listOf("关闭") + trackInfos.map { it.label } else trackInfos.map { it.label }
    val currentLabel =
        trackInfos.find { it.isSelected }?.label ?: if (hasDisableOption) "关闭" else ""

    MixDialogBuilder(title, colorScheme = colorScheme).apply {
        setContent {
            SingleSelectItemList(options, currentOption = currentLabel) { selected ->
                val builder = player.trackSelectionParameters.buildUpon()
                if (selected == "关闭") {
                    builder.setTrackTypeDisabled(trackType, true)
                } else {
                    val info = trackInfos.find { it.label == selected }!!
                    builder.setTrackTypeDisabled(trackType, false)
                        .setOverrideForType(TrackSelectionOverride(info.group, info.index))
                }
                player.trackSelectionParameters = builder.build()
                closeDialog()
            }
        }
        setDefaultNegative("取消")
        show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    Slider(
        value = progress,
        onValueChange = onSeek, // 直接执行传入的 seek 逻辑
        onValueChangeFinished = onSeekFinished,
        modifier = Modifier.fillMaxWidth(),
        thumb = {
            Box(modifier = Modifier.size(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.Center)
                        .background(Color.White, CircleShape)
                )
            }
        },
        track = { sliderState ->
            SliderDefaults.Track(sliderState = sliderState)
        }
    )
}

@Composable
fun PlayerSettingChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    )
}