package com.mina.legadostudio.skills

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

class KnowledgeRepository(private val context: Context) {
    @Keep
    data class Hit(
        @SerializedName("path") val path: String,
        @SerializedName("title") val title: String,
        @SerializedName("snippet") val snippet: String,
    )

    private val files by lazy { walk("knowledge").filterNot { it.endsWith("index.json") } }

    fun listDocuments(): List<Hit> = files.map { path ->
        Hit(path, path.substringAfterLast('/').substringBeforeLast('.'), read(path).lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().take(240))
    }

    fun search(query: String, limit: Int = 20): List<Hit> {
        val words = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return listDocuments().take(limit.coerceIn(1, 100))
        return files.mapNotNull { path ->
            val text = read(path)
            val index = words.map { text.indexOf(it, ignoreCase = true) }.filter { it >= 0 }.minOrNull() ?: return@mapNotNull null
            val start = (index - 180).coerceAtLeast(0)
            val end = (index + 420).coerceAtMost(text.length)
            val snippet = text.substring(start, end).replace(Regex("\\s+"), " ").trim()
            Hit(path, path.substringAfterLast('/').substringBeforeLast('.'), snippet)
        }.take(limit.coerceIn(1, 100))
    }

    fun read(path: String): String {
        require(path in files || path == "knowledge/index.json") { "非法知识库路径" }
        return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun walk(path: String): List<String> = context.assets.list(path).orEmpty().flatMap { name ->
        val child = "$path/$name"
        val nested = context.assets.list(child).orEmpty()
        if (nested.isEmpty()) listOf(child) else walk(child)
    }
}
