package com.mina.legadostudio.skills

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SkillRepository(private val context: Context) {
    @Keep
    data class SkillInfo(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("builtIn") val builtIn: Boolean,
        @SerializedName("enabled") val enabled: Boolean,
    )

    private val prefs = context.getSharedPreferences("studio_skills", Context.MODE_PRIVATE)

    private val customDir = File(context.filesDir, "skills").apply { mkdirs() }

    fun isEnabled(id: String): Boolean = prefs.getBoolean("enabled_$id", true)

    fun setEnabled(id: String, enabled: Boolean) {
        require(validId(id)) { "非法 Skill ID" }
        prefs.edit().putBoolean("enabled_$id", enabled).apply()
    }

    fun list(): List<SkillInfo> {
        val builtInIds = builtInIds()
        val builtIn = builtInIds.map { id ->
            SkillInfo(id, readName(readAsset("skills/$id/SKILL.md")) ?: id, true, isEnabled(id))
        }
        val custom = customDir.listFiles { file -> file.isDirectory }.orEmpty().mapNotNull { dir ->
            if (dir.name in builtInIds) return@mapNotNull null
            val file = File(dir, "SKILL.md").takeIf { it.isFile } ?: return@mapNotNull null
            SkillInfo(dir.name, readName(file.readText()) ?: dir.name, false, isEnabled(dir.name))
        }
        return (builtIn + custom).sortedBy { it.name }
    }

    fun isBuiltIn(id: String): Boolean = id in builtInIds()

    fun read(id: String): String {
        require(validId(id)) { "非法 Skill ID" }
        if (isBuiltIn(id)) return readAsset("skills/$id/SKILL.md")
        val custom = File(customDir, "$id/SKILL.md")
        require(custom.isFile) { "Skill 不存在" }
        return custom.readText()
    }

    fun readReference(id: String, relative: String): String {
        require(validId(id)) { "非法 Skill ID" }
        require(relative.matches(Regex("[\\p{L}\\p{N}._/-]+")) && !relative.contains("..")) { "非法参考路径" }
        if (isBuiltIn(id)) return readAsset("skills/$id/$relative")
        val custom = File(customDir, "$id/$relative")
        require(custom.isFile) { "参考文件不存在" }
        return custom.readText()
    }

    fun save(id: String, markdown: String) {
        requireMutable(id)
        require(markdown.contains("name:")) { "SKILL.md 缺少 name" }
        val dir = File(customDir, id).apply { mkdirs() }
        File(dir, "SKILL.md").writeText(markdown)
    }

    fun importPackage(fileName: String, input: InputStream): SkillInfo {
        return if (fileName.endsWith(".zip", true)) importZip(input) else {
            val markdown = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val id = slug(fileName.substringBeforeLast('.').ifBlank { readName(markdown).orEmpty() })
            save(id, markdown)
            SkillInfo(id, readName(markdown) ?: id, false, true)
        }
    }

    fun exportPackage(id: String, output: OutputStream) {
        require(validId(id)) { "非法 Skill ID" }
        ZipOutputStream(output).use { zip ->
            if (isBuiltIn(id)) {
                exportAssetTree(zip, "skills/$id", id)
                return@use
            }
            val custom = File(customDir, id)
            require(custom.isDirectory) { "Skill 不存在" }
            custom.walkTopDown().filter(File::isFile).forEach { file ->
                val relative = file.relativeTo(custom).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry("$id/$relative"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun delete(id: String): Boolean {
        requireMutable(id)
        return File(customDir, id).takeIf(File::isDirectory)?.deleteRecursively() ?: false
    }

    private fun importZip(input: InputStream): SkillInfo {
        val temp = File(context.cacheDir, "skill-import-${System.nanoTime()}").apply { mkdirs() }
        try {
            ZipInputStream(input).use { zip ->
                var entries = 0
                var totalBytes = 0L
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++entries <= 500) { "Skill 压缩包文件过多" }
                    val normalized = entry.name.replace('\\', '/').trimStart('/')
                    if (normalized.isBlank() || normalized.split('/').any { it == ".." }) continue
                    val target = File(temp, normalized).canonicalFile
                    require(target.path.startsWith(temp.canonicalPath + File.separator)) { "Skill 压缩包路径非法" }
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                totalBytes += read
                                require(totalBytes <= 20L * 1024 * 1024) { "Skill 压缩包解压后超过 20 MiB" }
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val skillFile = temp.walkTopDown().firstOrNull { it.isFile && it.name == "SKILL.md" }
                ?: error("压缩包中没有 SKILL.md")
            val markdown = skillFile.readText()
            val id = slug(skillFile.parentFile?.name.orEmpty().ifBlank { readName(markdown).orEmpty() })
            requireMutable(id)
            val sourceRoot = requireNotNull(skillFile.parentFile)
            val destination = File(customDir, id)
            destination.deleteRecursively(); destination.mkdirs()
            sourceRoot.walkTopDown().filter(File::isFile).forEach { file ->
                val target = File(destination, file.relativeTo(sourceRoot).path)
                target.parentFile?.mkdirs(); file.copyTo(target, overwrite = true)
            }
            return SkillInfo(id, readName(markdown) ?: id, false, isEnabled(id))
        } finally { temp.deleteRecursively() }
    }

    private fun exportAssetTree(zip: ZipOutputStream, assetPath: String, zipPath: String) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            zip.putNextEntry(ZipEntry(zipPath))
            context.assets.open(assetPath).use { it.copyTo(zip) }
            zip.closeEntry()
        } else children.forEach { exportAssetTree(zip, "$assetPath/$it", "$zipPath/$it") }
    }

    private fun builtInIds(): Set<String> = context.assets.list("skills").orEmpty().toSet()

    private fun requireMutable(id: String) = Companion.requireMutable(id, builtInIds())

    private fun slug(value: String): String {
        val cleaned = value.trim().replace(Regex("[^\\p{L}\\p{N}._-]+"), "-").trim('-').take(80)
        return cleaned.ifBlank { "skill-${System.currentTimeMillis()}" }
    }

    companion object {
        fun validId(id: String): Boolean =
            id.length in 1..80 && id !in setOf(".", "..") && id.matches(Regex("[\\p{L}\\p{N}._-]+"))

        fun requireMutable(id: String, builtInIds: Set<String>) {
            require(validId(id)) { "非法 Skill ID" }
            require(id !in builtInIds) { "内置 Skill 不可修改" }
        }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun readName(markdown: String): String? =
        Regex("(?m)^name:\\s*[\"']?([^\"'\\r\\n]+)").find(markdown)?.groupValues?.get(1)?.trim()
}
