package com.donut.mixfile.util.file

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.MixShareInfo
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.core.utils.sanitizeFileName
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.favorites.currentCategory
import com.donut.mixfile.ui.routes.home.getLocalServerAddress
import com.donut.mixfile.ui.routes.home.serverAddress
import com.donut.mixfile.ui.routes.settings.autoAddFavorite
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.getFileAccessUrl
import com.donut.mixfile.util.showToast


val FileDataLog.downloadUrl: String
    get() = getFileAccessUrl(getLocalServerAddress(), shareInfoData, name)


val FileDataLog.lanUrl: String
    get() = getFileAccessUrl(serverAddress, shareInfoData, name)

fun FileDataLog.updateDataList(
    list: List<FileDataLog>,
    action: (FileDataLog) -> FileDataLog
): List<FileDataLog> = list.map {
    if (it == this) {
        action(this)
    } else {
        it
    }
}

fun FileDataLog.rename(callback: (FileDataLog) -> Unit = {}) {
    var shareInfo = resolveMixShareInfo(shareInfoData) ?: return
    MixDialogBuilder("重命名文件").apply {
        var value by mutableStateOf(TextFieldValue(shareInfo.fileName))
        val focusRequester = FocusRequester()
        setContent {

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                val text = value.text
                val dotIndex = text.lastIndexOf('.')
                val end = if (dotIndex == -1) text.length else dotIndex

                value = value.copy(
                    selection = TextRange(0, end)
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                label = {
                    Text(text = "输入文件名")
                },
                maxLines = 1
            )

        }
        setDefaultNegative()
        setPositiveButton("确定") {
            if (value.text.isEmpty()) {
                showToast("文件名不能为空!")
                return@setPositiveButton
            }
            val sanitizedName = value.text.sanitizeFileName()
            shareInfo = shareInfo.copy(fileName = sanitizedName)
            val renamedLog = copy(
                name = sanitizedName,
                shareInfoData = shareInfo.toString()
            )
            favorites = updateDataList(favorites) {
                renamedLog
            }
            uploadLogs = updateDataList(uploadLogs) {
                renamedLog
            }
            callback(renamedLog)
            showToast("重命名文件成功!")
            closeDialog()
        }
        show()
    }
}


var favorites by cachedMutableOf(listOf<FileDataLog>(), "favorite_file_logs")

var uploadLogs by cachedMutableOf(listOf<FileDataLog>(), "upload_file_logs")

var favCategories by cachedMutableOf(setOf("默认"), "fav_categories")

fun addUploadLog(shareInfo: MixShareInfo) {
    if (autoAddFavorite) {
        addFavoriteLog(shareInfo.toDataLog())
    }
    val uploadLog = shareInfo.toDataLog()
    if (uploadLogs.size > 1000) {
        uploadLogs = uploadLogs.drop(1)
    }
    uploadLogs = uploadLogs + uploadLog
}


fun deleteUploadLog(uploadLog: FileDataLog, callback: () -> Unit = {}) {
    MixDialogBuilder("确定删除?").apply {
        setContent {
            Text(text = "确定从上传记录中删除?")
        }
        setPositiveButton("确定") {
            uploadLogs = uploadLogs.filter { it != uploadLog }
            closeDialog()
            callback()
            showToast("删除成功")
        }
        setDefaultNegative()
        show()
    }
}


fun addFavoriteLog(
    log: FileDataLog,
    category: String = currentCategory.ifEmpty { "默认" },
): Boolean {
    favCategories += category
    favorites = favorites.filter { it.shareInfoData != log.shareInfoData }
    favorites = favorites + log.copy(category = category)
    return true
}


fun deleteFavoriteLog(uploadLog: FileDataLog, callback: () -> Unit = {}) {
    MixDialogBuilder("确定删除?").apply {
        setContent {
            Text(text = "确定从收藏记录中删除?")
        }
        setPositiveButton("确定") {
            favorites -= uploadLog
            closeDialog()
            callback()
            showToast("删除成功")
        }
        setDefaultNegative()
        show()
    }
}
