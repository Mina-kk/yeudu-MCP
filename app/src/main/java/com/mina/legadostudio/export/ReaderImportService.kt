package com.mina.legadostudio.export

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import java.io.IOException

/** 阅读打开原生勾选页期间，维持本机回环 JSON 端点。 */
class ReaderImportService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        closeActive()
        super.onDestroy()
    }

    companion object {
        private val lock = Any()
        @Volatile private var activeServer: OneShotJsonServer? = null

        fun prepare(context: Context, json: String, ttlMs: Long): String {
            val app = context.applicationContext
            val candidate = OneShotJsonServer.start(json, ttlMs)
            synchronized(lock) {
                activeServer?.close()
                activeServer = candidate
            }
            try {
                app.startService(Intent(app, ReaderImportService::class.java))
            } catch (error: RuntimeException) {
                synchronized(lock) { if (activeServer === candidate) activeServer = null }
                candidate.close()
                throw error
            }
            synchronized(lock) {
                if (activeServer !== candidate || candidate.isClosed()) {
                    app.stopService(Intent(app, ReaderImportService::class.java))
                    throw IOException("loopback import endpoint stopped before launch")
                }
            }
            Thread({
                while (!candidate.isClosed()) {
                    try {
                        Thread.sleep(200)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
                val wasActive = synchronized(lock) {
                    val active = activeServer === candidate
                    if (active) activeServer = null
                    active
                }
                if (wasActive) app.stopService(Intent(app, ReaderImportService::class.java))
            }, "reader-import-service-monitor").apply { isDaemon = true }.start()
            return candidate.url
        }

        fun cancel(context: Context) {
            closeActive()
            context.applicationContext.stopService(Intent(context.applicationContext, ReaderImportService::class.java))
        }

        private fun closeActive() {
            synchronized(lock) {
                activeServer?.close()
                activeServer = null
            }
        }
    }
}
