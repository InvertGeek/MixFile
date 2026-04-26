package com.donut.mixfile.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import com.donut.mixfile.app
import com.donut.mixfile.appScope
import com.donut.mixfile.currentActivity
import com.donut.mixfile.server.core.utils.encodeURL
import com.donut.mixfile.server.core.utils.genRandomString
import com.donut.mixfile.server.core.utils.ignoreError
import com.donut.mixfile.server.mixFileServer
import com.donut.mixfile.ui.routes.home.getLocalServerAddress
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.EOFException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.log10
import kotlin.math.pow

fun String.copyToClipboard(showToast: Boolean = true) {
    val clipboard = getClipBoard()
    val clip = ClipData.newPlainText("Copied Text", this)
    clipboard.setPrimaryClip(clip)
    if (showToast) showToast("复制成功")
}

fun getClipBoard(context: Context = app.applicationContext): ClipboardManager {
    return context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

fun startActivity(intent: Intent) {
    val context = currentActivity ?: app
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun readClipBoardText(): String {
    val clipboard = getClipBoard()
    val clip = clipboard.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0).text
        return text?.toString() ?: ""
    }
    return ""
}


fun formatFileSize(bytes: Long, forceMB: Boolean = false): String {
    if (bytes <= 0) return "0 B"
    if (forceMB && bytes > 1024 * 1024) {
        return String.format(
            Locale.US,
            "%.2f MB",
            bytes / 1024.0 / 1024.0
        )
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)

    return String.format(
        Locale.US,
        "%.2f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}

fun getAppVersion(context: Context): Pair<String, Long> {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
        Pair(versionName, versionCode)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        Pair("Unknown", -1L)
    }
}


class CachedDelegate<T>(val getKeys: () -> Array<Any?>, private val initializer: () -> T) {
    private var cache: T = initializer()
    private var keys: Array<Any?> = getKeys()

    operator fun getValue(thisRef: Any?, property: Any?): T {
        val newKeys = getKeys()
        if (!keys.contentEquals(newKeys)) {
            keys = newKeys
            cache = initializer()
        }
        return cache
    }

    operator fun setValue(thisRef: Any?, property: Any?, value: T) {
        cache = value
    }
}

inline fun String.isUrl(block: (URL) -> Unit = {}): Boolean {
    val urlPattern =
        Regex("^https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)\$")
    val result = urlPattern.matches(this)
    if (result) {
        ignoreError {
            block(URL(this))
        }
    }
    return result
}

fun getUrlHost(url: String): String? {
    url.isUrl {
        return it.host
    }
    return null
}

@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.encodeToBase64() = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64() = Base64.decode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64String() = Base64.decode(this).decodeToString()

fun String.encodeToBase64() = this.toByteArray().encodeToBase64()

fun <T> List<T>.at(index: Long): T {
    var fixedIndex = index % this.size
    if (fixedIndex < 0) {
        fixedIndex += this.size
    }
    return this[fixedIndex.toInt()]
}

fun String.compareByName(b: String): Int {
    val a = this
    if (a == b) return 0

    val len1 = a.length
    val len2 = b.length
    var i = 0
    var j = 0

    while (i < len1 && j < len2) {
        val c1 = a[i]
        val c2 = b[j]

        val isDig1 = c1 in '0'..'9'
        val isDig2 = c2 in '0'..'9'

        if (isDig1 && isDig2) {
            val start1 = i
            val start2 = j

            // 跳过前导零，但保留最后一个零（如果是全零或数字结尾）
            while (i < len1 - 1 && a[i] == '0' && a[i + 1] in '0'..'9') i++
            while (j < len2 - 1 && b[j] == '0' && b[j + 1] in '0'..'9') j++

            val valStart1 = i
            val valStart2 = j

            // 计算数字部分的长度
            while (i < len1 && a[i] in '0'..'9') i++
            while (j < len2 && b[j] in '0'..'9') j++

            val numLen1 = i - valStart1
            val numLen2 = j - valStart2

            // 1. 比较数字长度（位数多者大）
            if (numLen1 != numLen2) return numLen1 - numLen2

            // 2. 长度相同时，逐位比较数值大小
            for (k in 0 until numLen1) {
                val diff = a[valStart1 + k] - b[valStart2 + k]
                if (diff != 0) return diff
            }

            // 3. 数值完全一样，比较包含前导零的原始长度
            val fullLen1 = i - start1
            val fullLen2 = j - start2
            if (fullLen1 != fullLen2) return fullLen1 - fullLen2

        } else {
            // 非数字部分比较
            if (c1 != c2) {
                // 模拟 case-insensitive 比较 (仅针对 ASCII)
                val low1 = if (c1 in 'A'..'Z') c1 + 32 else c1
                val low2 = if (c2 in 'A'..'Z') c2 + 32 else c2

                if (low1 != low2) return low1 - low2
                // 小写相同但原始字符不同（如 'a' vs 'A'）
                return c1 - c2
            }
            i++
            j++
        }
    }

    // 4. 若前面都相同，则短串在前
    return len1 - len2
}

fun <T> List<T>.at(index: Int): T {
    return this.at(index.toLong())
}

fun getAppVersionName(context: Context): String? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}

infix fun <T> List<T>.elementEquals(other: List<T>): Boolean {
    if (this.size != other.size) return false

    val tracker = BooleanArray(this.size)
    var counter = 0

    root@ for (value in this) {
        destination@ for ((i, o) in other.withIndex()) {
            if (tracker[i]) {
                continue@destination
            } else if (value?.equals(o) == true) {
                counter++
                tracker[i] = true
                continue@root
            }
        }
    }

    return counter == this.size
}


fun debug(text: String?, tag: String = "test") {
    Log.d(tag, text ?: "null")
}

inline fun catchError(tag: String = "", block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        showError(e, tag)
    }
}

fun getCurrentDate(reverseDays: Long = 0): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(System.currentTimeMillis() - (reverseDays * 86400 * 1000)))
}

fun getCurrentTime(): String {
    val currentTime = Date()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return formatter.format(currentTime)
}

fun genRandomHexString(length: Int = 32) = genRandomString(length, ('0'..'9') + ('a'..'f'))

fun readRawFile(id: Int) = app.resources.openRawResource(id).readBytes()


fun showError(e: Throwable, tag: String = "") {
    Log.e(
        "error",
        "${tag}发生错误: ${e.message} ${e.stackTraceToString()}"
    )
}

fun getFileAccessUrl(
    host: String = getLocalServerAddress(),
    shareInfo: String,
    fileName: String
): String {
    return URLBuilder("${host}/api/download/${fileName.encodeURL()}").apply {
        fragment = fileName
        parameters.apply {
            append("s", shareInfo)
            if (mixFileServer.password.isNotBlank()) {
                append("accessKey", mixFileServer.password)
            }
        }

    }.buildString()
}

fun getIpAddressInLocalNetwork(): String {
    return NetworkInterface.getNetworkInterfaces()?.asSequence()
        ?.filter { it.isUp }
        ?.flatMap { it.inetAddresses.asSequence() }
        ?.find { addr ->
            addr is Inet4Address &&
                    addr.isSiteLocalAddress &&
                    addr.hostAddress != "127.0.0.1"
        }?.hostAddress ?: "127.0.0.1"
}

fun isMainThread(): Boolean {
    return Looper.myLooper() == Looper.getMainLooper()
}

inline fun <T> errorDialog(title: String, onError: (Exception) -> Unit = {}, block: () -> T): T? {
    try {
        return block()
    } catch (e: Exception) {
        onError(e)
        when (e) {
            is CancellationException,
            is EOFException,
                -> return null
        }
        appScope.launch(Dispatchers.Main) {
            showErrorDialog(e, title)
        }
    }
    return null
}

fun CoroutineScope.loopTask(
    delay: Long,
    initDelay: Long = 0,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend () -> Unit
) = launch(dispatcher) {
    delay(initDelay)
    while (true) {
        block()
        delay(delay)
    }
}

fun formatTime(date: Date, format: String = "yyyy-MM-dd HH:mm:ss"): String {
    val formatter = SimpleDateFormat(format, Locale.US)
    return formatter.format(date)
}

fun formatTime(date: Long, format: String = "yyyy-MM-dd HH:mm:ss") = formatTime(Date(date), format)


fun Uri.getFileName(): String {
    var fileName = ""
    app.contentResolver.query(this, null, null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        fileName = it.getString(nameIndex)
    }
    return fileName
}

fun Uri.getFileSize() =
    app.contentResolver.openAssetFileDescriptor(this, "r")?.use { it.length } ?: 0