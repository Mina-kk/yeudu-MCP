package com.mina.legadostudio.ui.theme
import android.content.Context
/** 主题模式：跟随系统 / 浅色 / 深色，持久化到 SharedPreferences。 */
enum class ThemeMode(val key: String, val label: String) {
    LIGHT("light", "浅色"),
    SYSTEM("system", "跟随系统"),
    DARK("dark", "深色");
    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
        fun resolveDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }
    }
}
class ThemeModeStore(context: Context) {
    private val prefs = context.getSharedPreferences("studio_theme", Context.MODE_PRIVATE)
    fun load(): ThemeMode = ThemeMode.fromKey(prefs.getString("mode", null))
    fun save(mode: ThemeMode) {
        prefs.edit().putString("mode", mode.key).apply()
    }
}
