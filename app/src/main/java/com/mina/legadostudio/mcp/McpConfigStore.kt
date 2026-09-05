package com.mina.legadostudio.mcp

import android.content.Context
import com.mina.legadostudio.security.SecureStringStore
import java.security.SecureRandom

class McpConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("mcp_config", Context.MODE_PRIVATE)
    private val secure = SecureStringStore(context, "studio_mcp_token_v1", "secure_mcp_token_v1")
    data class Config(val port: Int, val tokenRequired: Boolean, val token: String)

    fun load(): Config {
        if (!prefs.getBoolean("v1SecurityInitialized", false)) {
            prefs.edit().putBoolean("tokenRequired", true).putBoolean("v1SecurityInitialized", true).apply()
        }
        if (!prefs.getBoolean("v2DefaultPortMigrated", false)) {
            val storedPort = prefs.getInt("port", LEGACY_DEFAULT_PORT)
            prefs.edit()
                .putInt("port", migrateLegacyDefaultPort(storedPort))
                .putBoolean("v2DefaultPortMigrated", true)
                .apply()
        }
        val token = secure.get("token")
            ?: prefs.getString("token", null)?.also { secure.put("token", it); prefs.edit().remove("token").apply() }
            ?: generateToken().also { secure.put("token", it) }
        return Config(
            port = prefs.getInt("port", DEFAULT_PORT).takeIf { it in 1024..65530 } ?: DEFAULT_PORT,
            tokenRequired = prefs.getBoolean("tokenRequired", true),
            token = token,
        )
    }

    fun save(config: Config) {
        require(config.port in 1024..65530) { "端口必须在 1024..65530" }
        require(!config.tokenRequired || config.token.isNotBlank()) { "启用访问令牌校验时，访问令牌不能为空" }
        prefs.edit().putInt("port", config.port).putBoolean("tokenRequired", config.tokenRequired).apply()
        secure.put("token", config.token)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(24).also(SecureRandom()::nextBytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
    }

    companion object {
        const val DEFAULT_PORT = 58823
        internal const val LEGACY_DEFAULT_PORT = 1237

        internal fun migrateLegacyDefaultPort(storedPort: Int): Int =
            if (storedPort == LEGACY_DEFAULT_PORT) DEFAULT_PORT else storedPort
    }
}
