package com.donut.mixfile.util.file

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.donut.mixfile.activity.video.VideoActivity
import com.donut.mixfile.app
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.isImage
import com.donut.mixfile.server.core.objects.isVideo
import com.donut.mixfile.server.core.utils.extensions.isNotNull
import com.donut.mixfile.server.core.utils.hashSHA256
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.core.utils.shareCode
import com.donut.mixfile.server.core.utils.toHex
import com.donut.mixfile.ui.component.common.Chip
import com.donut.mixfile.ui.component.common.InfoText
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.favorites.openCategorySelect
import com.donut.mixfile.ui.routes.home.DownloadTask
import com.donut.mixfile.ui.routes.home.showDownloadTaskWindow
import com.donut.mixfile.ui.routes.settings.useShortCode
import com.donut.mixfile.ui.routes.settings.useSystemPlayer
import com.donut.mixfile.util.CachedDelegate
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.showToast
import com.donut.mixfile.util.startActivity

@OptIn(ExperimentalLayoutApi::class)
fun showFileInfoDialog(
    dataLog: FileDataLog,
    onDismiss: () -> Unit = {}
) {
    var isFav = false

    val log by CachedDelegate({ arrayOf(favorites) }) {
        val firstSimilar = favorites.firstOrNull { it.isSimilar(dataLog) }
        isFav = firstSimilar.isNotNull()
        firstSimilar ?: dataLog
    }

    val shareInfo = resolveMixShareInfo(log.shareInfoData)
    if (shareInfo == null) {
        showToast("解析文件分享码失败")
        return
    }

    MixDialogBuilder("文件信息", tag = "file-info-${shareInfo.url}").apply {
        onDismiss(onDismiss)
        setNegativeButton("复制分享码") {
            shareInfo.shareCode(useShortCode).copyToClipboard()
        }
        setContent {
            val fileName = log.name
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "名称: ", value = fileName)
                InfoText(key = "大小: ", value = formatFileSize(shareInfo.fileSize))
                InfoText(key = "密钥: ", value = shareInfo.key)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (fileName.endsWith(".mix_list")) {
                        Chip("文件列表") {
                            importFileList(log.downloadUrl)
                        }
                    }
                    if (fileName.endsWith(".mix_dav")) {
                        Chip("查看文件") {
                            previewWebDavData(log.downloadUrl)
                        }
                    }
                    if (!isFav) {
                        Chip("收藏") {
                            addFavoriteLog(log)
                        }
                    } else {
                        Chip("取消收藏") {
                            deleteFavoriteLog(log)
                        }
                        Chip("重命名") {
                            log.rename {
                                closeDialog()
                                showFileInfoDialog(it, onDismiss)
                            }
                        }
                        Chip("分类: ${log.getCategory()}") {
                            openCategorySelect(log.getCategory()) { category ->
                                favorites = log.updateDataList(favorites) {
                                    log.copy(category = category)
                                }
                            }
                        }
                    }
                    if (dataLog.isVideo) {
                        Chip("播放视频") {
                            if (useSystemPlayer) {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(log.downloadUrl.toUri(), "video/*")
                                startActivity(intent)
                                return@Chip
                            }
                            val intent = Intent(app, VideoActivity::class.java)
                            VideoActivity.videoList = listOf(log.downloadUrl.toUri())
                            VideoActivity.videoHash = shareInfo.toString().hashSHA256().toHex()
                            startActivity(intent)
                        }
                    }
                    if (dataLog.isImage) {
                        Chip("查看图片") {
                            showImageDialog(log.downloadUrl)
                        }
                    }
                    Chip("复制局域网地址") {
                        log.lanUrl.copyToClipboard()
                    }
                }
            }
        }
        setPositiveButton("下载文件") {
            downloadFile(log)
            closeDialog()
            showDownloadTaskWindow()
        }
        show()
    }
}

fun downloadFile(file: FileDataLog) {
    val task = DownloadTask(file.name, file.size, file.downloadUrl)
    task.start()
}
