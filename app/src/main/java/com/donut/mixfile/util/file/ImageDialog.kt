package com.donut.mixfile.util.file

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.donut.mixfile.genImageLoader
import com.donut.mixfile.server.core.utils.extensions.isTrue
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.objects.ProgressContent
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.internal.toLongOrDefault
import java.util.concurrent.TimeUnit

@Composable
fun ErrorMessage(msg: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(200.dp, 600.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = msg,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )
    }
}

fun showImageDialog(url: String) {
    MixDialogBuilder("查看图片").apply {
        setContent {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.padding(10.dp)
            ) {
                ImageContent(url)
            }
        }
        setDefaultNegative("关闭")
        show()
    }

}

val forceCacheInterceptor = Interceptor { chain ->
    val response = chain.proceed(chain.request())
    val cacheControl = CacheControl.Builder()
        .maxAge(10, TimeUnit.MINUTES)
        .build()
    response.newBuilder()
        .removeHeader("Pragma")
        .removeHeader("Cache-Control")
        .header("Cache-Control", cacheControl.toString())
        .build()
}

val videoDecodeInterceptor = Interceptor { chain ->
    val response = chain.proceed(chain.request())
    if (response.header("content-type")?.startsWith("video/").isTrue()) {
        val contentLength = response.header("Content-Length")?.toLongOrDefault(-1) ?: 0
        if (contentLength > 1024 * 1024 * 5) {
            return@Interceptor response.newBuilder()
                .header("Content-Length", "${1024 * 1024 * 3}")
                .body(response.peekBody(1024 * 1024 * 3))
                .build()
        }
    }
    response
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageContent(
    imageUrl: String,
    @SuppressLint("ModifierParameter") modifier: Modifier? = null,
    scale: ContentScale = ContentScale.Fit,
) {
    val progress = remember {
        ProgressContent(tip = "图片加载中")
    }
    val zoomState = rememberZoomState()
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
//            .videoFrameMillis(0)
            .crossfade(true)
            .build(),
        error = {
            ErrorMessage(msg = "图片加载失败")
        },
        imageLoader = genImageLoader(
            LocalContext.current,
            initializer = {
                OkHttpClient.Builder()
                    .addNetworkInterceptor(progress.interceptor)
                    .addNetworkInterceptor(forceCacheInterceptor)
                    .addNetworkInterceptor(videoDecodeInterceptor)
                    .build()
            }),
        loading = {
            progress.LoadingContent()
        },
        contentDescription = "图片",
        contentScale = scale,
        modifier = modifier ?: Modifier
            .fillMaxWidth()
            .heightIn(400.dp, 1000.dp)
            .zoomable(zoomState)

    )
}