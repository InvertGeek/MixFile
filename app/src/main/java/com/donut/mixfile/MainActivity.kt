package com.donut.mixfile

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.donut.mixfile.ui.component.MainContent
import com.donut.mixfile.ui.theme.MainTheme
import com.donut.mixfile.util.file.MixFileSelector
import com.donut.mixfile.util.file.uploadFileUris
import com.donut.mixfile.util.objects.MixActivity
import com.donut.mixfile.util.reveiver.NetworkChangeReceiver


class MainActivity : MixActivity(MAIN_ID) {

    companion object {
        lateinit var networkChangeReceiver: NetworkChangeReceiver
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        networkChangeReceiver = NetworkChangeReceiver
        //请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        registerReceiver(networkChangeReceiver, intentFilter)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                MainContent()
            }
        }
        handleIntent()
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent()
        super.onNewIntent(intent)
    }

    private fun handleIntent() {
        val action = intent.action
        intent.type ?: return
        when (action) {
            Intent.ACTION_SEND -> {
                // 处理单文件分享
                val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (fileUri != null) {
                    uploadFileUris(listOf(fileUri))
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                // 处理多文件分享
                val fileUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (fileUris != null) {
                    uploadFileUris(fileUris)
                }
            }
        }
    }
}

