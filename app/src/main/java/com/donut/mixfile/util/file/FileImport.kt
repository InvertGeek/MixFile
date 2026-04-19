package com.donut.mixfile.util.file

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.donut.mixfile.activity.video.VideoActivity
import com.donut.mixfile.app
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.isVideo
import com.donut.mixfile.server.core.objects.toByteArray
import com.donut.mixfile.server.core.utils.decompressGzip
import com.donut.mixfile.server.core.utils.hashSHA256
import com.donut.mixfile.server.core.utils.parseJsonObject
import com.donut.mixfile.server.core.utils.sanitizeFileName
import com.donut.mixfile.server.core.utils.toHex
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.AsyncEffect
import com.donut.mixfile.util.catchError
import com.donut.mixfile.util.compareByName
import com.donut.mixfile.util.errorDialog
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.getCurrentTime
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showConfirmDialog
import com.donut.mixfile.util.showToast
import com.donut.mixfile.util.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

fun exportFileList(fileList: Collection<FileDataLog>, name: String) {
    doUploadFile(
        fileList.toByteArray(),
        "${name}.mix_list",
        false
    )
}

fun showExportFileListDialog(fileList: Collection<FileDataLog>) {
    MixDialogBuilder("确定导出?").apply {
        var listName by mutableStateOf("文件列表-${getCurrentTime()}")
        setContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedTextField(
                    value = listName,
                    onValueChange = {
                        listName = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = "列表名称")
                    },
                    maxLines = 1,
                )
                Text(text = "将会导出当前筛选的文件列表上传为一键分享链接")
            }
        }
        setDefaultNegative()
        setPositiveButton("确定") {
            exportFileList(fileList, listName.sanitizeFileName())
            closeDialog()
        }
        show()
    }
}

fun List<FileDataLog>.hashSHA256(): String = joinToString { it.shareInfoData }.hashSHA256().toHex()


fun playVideoList(videoList: List<FileDataLog>) {
    val playList = videoList.sortedWith { file1, file2 ->
        file1.name.compareByName(file2.name)
    }
    val intent = Intent(app, VideoActivity::class.java)
    VideoActivity.videoList = playList.map { it.downloadUrl.toUri() }
    VideoActivity.videoHash = playList.hashSHA256()
    startActivity(intent)
}

fun showFileList(fileList: List<FileDataLog>) {
    val fileTotalSize = fileList.sumOf { it.size }
    val videoList =
        fileList.filter { it.isVideo }
    MixDialogBuilder(
        "文件列表",
        "共 ${fileList.size} 个文件 总大小: ${formatFileSize(fileTotalSize)}",
        tag = "file-list-${fileList.hashSHA256()}"
    ).apply {
        setContent {
            var listState by remember { mutableStateOf(fileList) }
            AsyncEffect(fileList) {
                catchError {
                    val sorted = fileList.sortedWith { file1, file2 ->
                        if (!isActive) {
                            throw Exception("排序取消")
                        }
                        file1.name.compareByName(file2.name)
                    }
                    withContext(Dispatchers.Main) {
                        listState = sorted
                    }
                }
            }
            FileCardList(listState)
        }
        if (videoList.isNotEmpty()) {
            setNegativeButton("全部播放") {
                playVideoList(videoList)
            }
        }
        setPositiveButton("导入文件") {
            showImportConfirmWindow(fileList)
        }
        show()
    }
}


fun showImportConfirmWindow(fileList: List<FileDataLog>) {
    showConfirmDialog("确定导入?", "是否确定导入文件列表") {
        val fileMap = favorites.map { it.shareInfoData }.toSet()
        val newFiles = mutableSetOf<FileDataLog>()
        val newCategories = mutableSetOf<String>()
        fileList.forEach {
            newCategories += it.getCategory()
            if (!fileMap.contains(it.shareInfoData)) {
                newFiles += it
            }
        }
        favCategories += newCategories
        favorites += newFiles
        showToast("导入了 ${newFiles.size} 个文件")
    }
}

suspend fun loadFileList(url: String, progress: ProgressContent): List<FileDataLog> {
    val fileListData = loadDataWithMaxSize(url, progress)
    return decompressGzip(fileListData).parseJsonObject()
}

fun importFileList(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("解析中").apply {
        setContent {
            AsyncEffect {
                errorDialog("解析文件失败", onError = { closeDialog() }) {
                    val fileList: List<FileDataLog> = loadFileList(url, progress)
                    withContext(Dispatchers.Main) {
                        showFileList(fileList.toList())
                        closeDialog()
                    }
                }
            }
            progress.LoadingContent()
        }
        setDefaultNegative()
        show()
    }

}
