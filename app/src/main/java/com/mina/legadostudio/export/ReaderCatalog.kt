package com.mina.legadostudio.export

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri

/** 已安装、能处理 legado://import/bookSource 的阅读分支。 */
object ReaderCatalog {
    data class App(val packageName: String, val label: String, val known: Boolean)

    fun resolve(pm: PackageManager, lastPackage: String = ""): List<App> {
        val seen = linkedSetOf<String>()
        val apps = query(pm, importIntent()).mapNotNull { info ->
            val pkg = info.activityInfo?.packageName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!seen.add(pkg)) return@mapNotNull null
            describe(pkg, info.loadLabel(pm))
        }.toMutableList()
        sort(apps, lastPackage)
        return apps
    }

    fun sort(apps: MutableList<App>, lastPackage: String?) {
        val last = lastPackage.orEmpty()
        apps.sortWith(
            compareBy<App> { it.packageName != last }
                .thenBy { !it.known }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                .thenBy { it.packageName },
        )
    }

    fun describe(packageName: String, activityLabel: CharSequence?): App {
        val known = KNOWN[packageName]
        if (known != null) return App(packageName, known, true)
        val raw = activityLabel?.toString()?.trim().orEmpty().ifBlank { "未知阅读" }
        val label = if (raw.contains("未知阅读")) raw else "$raw · 未知阅读"
        return App(packageName, label, false)
    }

    fun importIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("legado://import/bookSource"))
        .addCategory(Intent.CATEGORY_BROWSABLE)

    fun importIntent(packageName: String?, srcUrl: String): Intent {
        val view = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("legado://import/bookSource").buildUpon().appendQueryParameter("src", srcUrl).build(),
        ).addCategory(Intent.CATEGORY_BROWSABLE)
        if (!packageName.isNullOrBlank()) view.setPackage(packageName)
        return view
    }

    private fun query(pm: PackageManager, view: Intent): List<ResolveInfo> {
        val infos = pm.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY).orEmpty().toMutableList()
        runCatching {
            pm.queryIntentActivities(view, PackageManager.MATCH_ALL).orEmpty().forEach { info ->
                val pkg = info.activityInfo?.packageName ?: return@forEach
                if (infos.none { it.activityInfo?.packageName == pkg }) infos.add(info)
            }
        }
        return infos
    }

    private val KNOWN = linkedMapOf(
        "io.legado.app" to "阅读 原版",
        "io.legado.app.release" to "阅读 原版",
        "io.legado.app.debug" to "阅读 原版（调试）",
        "io.legado.app.md3" to "阅读 MD3",
        "io.legado.plus" to "阅读 Plus / Sigma",
        "io.legado.app.plus" to "阅读 Plus / Sigma",
        "io.legado.app.e" to "阅读 Plus / Sigma",
        "io.legado.app.t" to "阅读 T",
        "io.legado.app.r" to "阅读 R",
        "io.legado.app.c" to "阅读 C",
        "io.legado.app.max" to "阅读 MAX",
        "io.legado.app.sum" to "阅读 MAX SUM版",
        "io.legado.app.dandan" to "阅读 MAX 蛋蛋版",
        "io.legado.app.cichen" to "阅读 MAX 辞晨版",
        "io.legado.app.ng" to "阅读 NG",
        "io.legado.app.pp" to "阅读 PP",
        "io.legado.app.archive" to "阅读 Archive",
        "io.legado.app.beta" to "阅读 Beta / 喵公子",
        "io.legado.app.shutiao" to "阅读 薯条版",
        "io.legado.app.jingshiro" to "Jingshiro 版",
    )
}
