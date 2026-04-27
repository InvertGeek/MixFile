package com.donut.mixfile.server

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.uploaders.A1Uploader
import com.donut.mixfile.server.core.uploaders.A2Uploader
import com.donut.mixfile.server.core.uploaders.A3Uploader
import com.donut.mixfile.server.core.uploaders.base.HttpUploader
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.cachedMutableOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var CUSTOM_UPLOAD_URL by cachedMutableOf("", "CUSTOM_UPLOAD_URL")

var CUSTOM_REFERER by cachedMutableOf("", "CUSTOM_REFERER")


val UPLOADERS = listOf(CustomUploader, JavaScriptUploader)

val DEFAULT_UPLOADER = JavaScriptUploader

var currentUploader by cachedMutableOf(DEFAULT_UPLOADER.name, "current_uploader")

fun getCurrentUploader(uploaders: List<Uploader> = UPLOADERS) =
    uploaders.firstOrNull { it.name.contentEquals(currentUploader) } ?: DEFAULT_UPLOADER

object CustomUploader : HttpUploader("自定义") {

    override val referer: String
        get() = CUSTOM_REFERER


    override val reqUrl: String
        get() = CUSTOM_UPLOAD_URL

    override suspend fun setReferer(value: String) {
        withContext(Dispatchers.Main) {
            CUSTOM_REFERER = value
        }
    }

}

fun openCustomUploaderWindow() {
    MixDialogBuilder("自定义线路设置").apply {
        setContent {
            OutlinedTextField(
                value = CUSTOM_UPLOAD_URL,
                onValueChange = {
                    CUSTOM_UPLOAD_URL = it.trim()
                },
                maxLines = 1,
                label = {
                    Text(text = "请求地址")
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = CUSTOM_REFERER,
                maxLines = 1,
                onValueChange = {
                    CUSTOM_REFERER = it.trim()
                },
                label = {
                    Text(text = "referer")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                text = """
        自定义线路请自行实现,app会使用PUT方式发送请求
        请求体为图片二进制,成功请返回200状态码,内容直接返回URL
        失败返回403或500(会重试)状态码
        另外需要实现GET方法返回填充图片,推荐gif格式
        图片尺寸不宜过大,否则影响上传速度
        """.trimIndent(),
            )
        }
        setDefaultNegative("关闭")
        show()
    }
}