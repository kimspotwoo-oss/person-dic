package com.persondic.ui.common

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The system photo picker only grants short-lived read access to the URI it returns, so storing
 * that URI would leave a broken avatar after the next launch. Copying the bytes into app-internal
 * storage keeps the photo working indefinitely and keeps everything on-device.
 */
suspend fun copyImageToInternalStorage(context: Context, source: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val target = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            target.absolutePath
        }.getOrNull()
    }

fun deleteStoredPhoto(path: String?) {
    if (path.isNullOrBlank()) return
    runCatching { File(path).takeIf { it.exists() }?.delete() }
}
