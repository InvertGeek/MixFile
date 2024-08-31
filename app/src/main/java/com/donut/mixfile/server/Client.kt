package com.donut.mixfile.server

import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.userAgent
import io.ktor.serialization.gson.GsonConverter
import io.ktor.serialization.gson.gson
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toOutputStream
import java.io.InputStream

val uploadClient = HttpClient(CIO).config {

    install(ContentNegotiation) {
        gson()
        register(ContentType.Text.Html, GsonConverter(GsonBuilder().create()))
    }
    install(HttpRequestRetry) {
        retryOnExceptionOrServerErrors(3)
        delayMillis { retry ->
            retry * 100L
        }
    }
    install(HttpTimeout)
    install(DefaultRequest) {
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
    }
}

val localClient = HttpClient(CIO).config {
    install(HttpTimeout)
}

class StreamContent(private val stream: InputStream, val length: Long = 0) :
    OutgoingContent.WriteChannelContent() {
    override suspend fun writeTo(channel: ByteWriteChannel) {
        stream.copyTo(channel.toOutputStream())
    }

    override val contentLength: Long
        get() = length

}