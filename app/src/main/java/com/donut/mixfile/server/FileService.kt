package com.donut.mixfile.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.alibaba.fastjson2.toJSONString
import com.donut.mixfile.MainActivity
import com.donut.mixfile.R
import com.donut.mixfile.app
import com.donut.mixfile.appScope
import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.utils.MixUploadTask
import com.donut.mixfile.server.image.createBlankBitmap
import com.donut.mixfile.server.image.toGif
import com.donut.mixfile.ui.routes.home.UploadTask
import com.donut.mixfile.ui.routes.home.serverAddress
import com.donut.mixfile.ui.routes.increaseDownloadData
import com.donut.mixfile.ui.routes.increaseUploadData
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.showError
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.InputStream

var DOWNLOAD_TASK_COUNT by cachedMutableOf(5, "download_task_count")
var UPLOAD_TASK_COUNT by cachedMutableOf(10, "upload_task_count")
var enableAccessKey by cachedMutableOf(false, "enable_mix_file_access_key")
var UPLOAD_RETRY_TIMES by cachedMutableOf(10, "UPLOAD_RETRY_TIMES")


val mixFileServer = object : MixFileServer(
    accessKeyTip = "网页端已被禁止访问,请到APP设置中开启",
    enableAccessKey = enableAccessKey,
) {

    override fun onDownloadData(data: ByteArray) {
        increaseDownloadData(data.size.toLong())
    }

    override fun onUploadData(data: ByteArray) {
        increaseUploadData(data.size.toLong())
    }


    override val downloadTaskCount: Int
        get() = DOWNLOAD_TASK_COUNT.toInt()
    override val uploadTaskCount: Int
        get() = UPLOAD_TASK_COUNT.toInt()
    override val requestRetryCount: Int
        get() = UPLOAD_RETRY_TIMES.toInt()


    override fun onError(error: Throwable) {
        showError(error)
    }

    override fun getUploader(): Uploader {
        return getCurrentUploader()
    }

    override fun getStaticFile(path: String): InputStream? {
        try {
            val fileStream = app.assets.open(path)
            return fileStream
        } catch (e: FileNotFoundException) {
            return null
        }
    }

    override fun genDefaultImage(): ByteArray {
        return createBlankBitmap().toGif()
    }

    override fun getFileHistory(): String {
        return favorites.takeLast(1000).toJSONString()
    }

    override fun getUploadTask(
        call: ApplicationCall,
        name: String,
        size: Long,
        add: Boolean
    ): MixUploadTask {
        return UploadTask(call, name, size, add)
    }

}
var serverStarted by mutableStateOf(false)


class FileService : Service() {

    companion object {
        var instance: FileService? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val CHANNEL_ID = "MixFileServerChannel"


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appScope.launch(Dispatchers.IO) {
            mixFileServer.start()
            delay(1000)
            serverStarted = true
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, getNotification())
        instance = this
    }


    private fun getNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MixFile局域网服务器")
            .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
            .setOngoing(true)
            .setContentText("运行中: $serverAddress")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun updateNotification() {
        val notification: Notification = getNotification()

        val mNotificationManager =
            getSystemService(NotificationManager::class.java)
        mNotificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "局域网文件服务器",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}