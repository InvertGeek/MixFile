package com.donut.mixfile.server.core.routes.api.webdav.objects

import com.alibaba.fastjson2.annotation.JSONField
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.utils.hashSHA256
import com.donut.mixfile.server.core.utils.parseFileMimeType
import com.donut.mixfile.server.core.utils.sanitizeFileName
import com.donut.mixfile.server.core.utils.toHex
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


fun WebDavFile.toDataLog() = FileDataLog(shareInfoData, name, size)

// WebDAV 文件类，包含额外属性
class WebDavFile(
    var name: String,
    val size: Long = 0,
    val shareInfoData: String = "",
    val isFolder: Boolean = false,
    var lastModified: Long = System.currentTimeMillis()
) {

    init {
        name = name.trim().sanitizeFileName()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebDavFile) return false

        return name.contentEquals(other.name)
    }

    @JSONField(serialize = false)
    fun getLastModifiedFormatted(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date(lastModified))
    }


    override fun hashCode(): Int = name.hashCode()

    fun toXML(path: String, isRoot: Boolean = false): String {
        val pathName = name.takeIf { !isRoot } ?: ""
        if (isFolder) {
            return xml("D:response") {
                "D:href" {
                    -"/${"$path/${pathName}".normalPath()}/".encodeURLPath(encodeEncoded = true)
                }
                "D:propstat" {
                    "D:prop" {
                        "D:displayname" {
                            -name.encodeURLParameter()
                        }
                        "D:resourcetype" {
                            "D:collection" {
                                attribute("xmlns:D", "DAV:")
                            }
                        }

                        "D:getlastmodified" {
                            -getLastModifiedFormatted()
                        }
                    }
                    "D:status" {
                        -"HTTP/1.1 200 OK"
                    }
                }
            }.toString()
        }
        return xml("D:response") {
            "D:href" {
                -"/${"$path/$name".normalPath()}".encodeURLPath(encodeEncoded = true)
            }
            "D:propstat" {
                "D:prop" {
                    "D:displayname" {
                        -name.encodeURLParameter()
                    }
                    "D:resourcetype" {

                    }
                    "D:getcontenttype" {
                        -name.parseFileMimeType().toString()
                    }
                    "D:getcontentlength" {
                        -size.toString()
                    }
                    "D:getetag" {
                        -shareInfoData.hashSHA256().toHex()
                    }
                    "D:getlastmodified" {
                        -getLastModifiedFormatted()
                    }
                }
                "D:status" {
                    -"HTTP/1.1 200 OK"
                }
            }
        }.toString()
    }

}