package com.donut.mixfile.activity.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import java.util.Locale


/**
 * 显示字幕/音轨选择弹窗
 */
@androidx.annotation.OptIn(UnstableApi::class)
fun showTrackSelector(
    title: String,
    player: ExoPlayer,
    trackType: @C.TrackType Int,
    colorScheme: ColorScheme,
    hasDisableOption: Boolean = false
) {
    // 1. 获取所有轨道组并提取规范化信息
    val groups = player.currentTracks.groups.filter { it.type == trackType }

    // 内部数据类，用于解耦逻辑
    data class TrackDisplayInfo(
        val label: String,
        val group: TrackGroup,
        val index: Int,
        val isSelected: Boolean
    )

    var trackIndex = 1
    val trackInfos = groups.flatMap { group ->
        val trackGroup = group.mediaTrackGroup
        List(trackGroup.length) { i ->
            val format = trackGroup.getFormat(i)
            val langLabel = format.language?.let { lang ->
                val locale = Locale.forLanguageTag(lang)

                locale.getDisplayName(Locale.getDefault()).ifBlank { format.language }
            } ?: "未知"


            val displayLabel = "${trackIndex} ${langLabel}".let {
                if (format.label != null) {
                    it + " - ${format.label}"
                } else it
            }
            trackIndex++


            TrackDisplayInfo(
                label = displayLabel,
                group = trackGroup,
                index = i,
                isSelected = group.isTrackSelected(i)
            )
        }
    }

    // 2. 构建 UI 选项列表
    val disableLabel = if (trackType == C.TRACK_TYPE_TEXT) "关闭字幕" else "禁用音轨"
    val options = buildList {
        if (hasDisableOption) add(disableLabel)
        addAll(trackInfos.map { it.label })
    }

    val currentSelectedLabel = trackInfos.firstOrNull { it.isSelected }?.label ?: disableLabel

    MixDialogBuilder(title, colorScheme = colorScheme).apply {
        setContent {
            SingleSelectItemList(options, currentOption = currentSelectedLabel) { selected ->
                val paramsBuilder = player.trackSelectionParameters.buildUpon()

                if (selected == disableLabel) {
                    paramsBuilder.setTrackTypeDisabled(trackType, true)
                } else {
                    val info = trackInfos.firstOrNull { it.label == selected }
                    info?.let {
                        paramsBuilder
                            .setTrackTypeDisabled(trackType, false)
                            // 清除该类型之前的覆盖设置，确保新设置生效
                            .clearOverridesOfType(trackType)
                            .addOverride(TrackSelectionOverride(it.group, it.index))
                    }
                }

                player.trackSelectionParameters = paramsBuilder.build()
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
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
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