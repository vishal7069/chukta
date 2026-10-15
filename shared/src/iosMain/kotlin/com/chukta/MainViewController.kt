@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.chukta

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUUID
import platform.UIKit.UIViewController

fun MainViewController(
    apiUrl: String,
    read: (String) -> String?,
    write: (String, String?) -> Unit,
    openUrl: (String) -> Boolean,
    share: (String) -> Unit,
    copy: (String) -> Unit,
    pickPhoto: ((String?) -> Unit) -> Unit,
    pickContact: ((String?) -> Unit) -> Unit,
    photoData: (String) -> String?,
    formatDate: (String) -> String
): UIViewController {
    val configuredUrl=apiUrl
    val hostRead=read
    val hostWrite=write
    val hostOpen=openUrl
    val hostShare=share
    val hostCopy=copy
    val hostPhoto=pickPhoto
    val hostContact=pickContact
    val hostPhotoData=photoData
    val hostFormat=formatDate
    val platform = object: Platform {
        override val apiUrl: String get() = hostRead("api-url") ?: configuredUrl
        override val development = configuredUrl.contains("example.com")
        override fun read(key: String) = hostRead(key)
        override fun write(key: String, value: String?) = hostWrite(key, value)
        override fun openUrl(url: String) = hostOpen(url)
        override fun share(text: String) = hostShare(text)
        override fun copy(text: String) = hostCopy(text)
        override fun pickPhoto(onResult: (String?) -> Unit) = hostPhoto(onResult)
        override fun pickContact(onResult: (String?) -> Unit) = hostContact(onResult)
        override fun photoData(uri: String): String? = hostPhotoData(uri)
        override fun newId() = NSUUID().UUIDString
        override fun formatDate(iso: String) = hostFormat(iso)
        override fun today(): String = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd" }.stringFromDate(NSDate())
    }
    return ComposeUIViewController { ChuktaApp(platform) }
}
