package com.mina.legadostudio.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.mina.legadostudio.domain.BookSourceValidator
import java.io.File

class SourceExporter(private val context: Context, private val validator: BookSourceValidator) {
    fun saveToDownloads(fileName: String, sourceJson: String): String {
        validate(sourceJson)
        val safe = safeName(fileName) + ".json"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "阅读书源MCP").apply { mkdirs() }
            return File(dir, safe).apply { writeText(sourceJson) }.absolutePath
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safe)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/阅读书源MCP")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建下载文件")
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(sourceJson) }
            ?: error("无法写入下载文件")
        return uri.toString()
    }

    fun share(fileName: String, sourceJson: String) {
        validate(sourceJson)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, safeName(fileName) + ".json").apply { writeText(sourceJson) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }, "发送书源").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun validate(sourceJson: String) {
        val report = validator.validate(sourceJson)
        require(report.isValid) { "书源验证未通过：${report.issues.joinToString { it.message }}" }
    }

    private fun safeName(name: String): String = name.ifBlank { "BookSource" }.replace(Regex("[\\/:*?\"<>|]"), "_").take(80)
}
