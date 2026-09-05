package com.mina.legadostudio.export

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** 只绑 127.0.0.1 的一次性 JSON 端点，首次成功 GET 后关闭。 */
class OneShotJsonServer private constructor(json: String, ttlMs: Long) : Closeable {
    private val serverSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 4)
    }
    private val route = "/import/" + UUID.randomUUID().toString().replace("-", "") + ".json"
    val url: String = "http://127.0.0.1:${serverSocket.localPort}$route"
    val port: Int get() = serverSocket.localPort
    private val expiresAtMs = System.currentTimeMillis() + ttlMs
    private val closed = AtomicBoolean(false)
    @Volatile private var payload: ByteArray? = json.toByteArray(StandardCharsets.UTF_8)

    init {
        Thread(this::serve, "reader-import-loopback").apply { isDaemon = true }.start()
    }

    fun isClosed(): Boolean = closed.get()

    private fun serve() {
        try {
            while (!closed.get()) {
                val remaining = expiresAtMs - System.currentTimeMillis()
                if (remaining <= 0) break
                serverSocket.soTimeout = remaining.coerceAtMost(1_000L).toInt()
                try {
                    serverSocket.accept().use { client -> if (handle(client)) break }
                } catch (_: SocketTimeoutException) {
                } catch (_: SocketException) {
                    if (!closed.get()) break
                } catch (_: java.io.IOException) {
                }
            }
        } catch (_: SocketException) {
        } finally {
            close()
        }
    }

    private fun handle(client: Socket): Boolean {
        client.soTimeout = CLIENT_TIMEOUT_MS
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII))
        val requestLine = reader.readLine()
        var headerChars = requestLine?.length ?: 0
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            headerChars += line.length
            if (headerChars > MAX_HEADER_CHARS) {
                writeResponse(client, "431 Request Header Fields Too Large", null, false)
                return false
            }
        }
        if (requestLine == null) return false
        val parts = requestLine.split(" ")
        if (parts.size < 2) {
            writeResponse(client, "400 Bad Request", null, false)
            return false
        }
        val method = parts[0]
        var target = parts[1]
        val query = target.indexOf('?')
        if (query >= 0) target = target.substring(0, query)
        if (target != route) {
            writeResponse(client, "404 Not Found", null, false)
            return false
        }
        if (method == "HEAD") {
            writeResponse(client, "200 OK", payload, false)
            return false
        }
        if (method != "GET") {
            writeResponse(client, "405 Method Not Allowed", null, false)
            return false
        }
        val body = payload
        if (body == null) {
            writeResponse(client, "410 Gone", null, false)
            return false
        }
        writeResponse(client, "200 OK", body, true)
        payload = null
        return true
    }

    private fun writeResponse(client: Socket, status: String, body: ByteArray?, includeBody: Boolean) {
        val length = body?.size ?: 0
        val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), StandardCharsets.US_ASCII))
        writer.write("HTTP/1.1 $status\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Content-Length: $length\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.flush()
        if (includeBody && length > 0 && body != null) {
            client.getOutputStream().write(body)
            client.getOutputStream().flush()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        payload = null
        runCatching { serverSocket.close() }
    }

    companion object {
        private const val MAX_HEADER_CHARS = 16 * 1024
        private const val CLIENT_TIMEOUT_MS = 10_000
        fun start(json: String, ttlMs: Long): OneShotJsonServer {
            require(json.isNotBlank()) { "missing json" }
            require(ttlMs > 0) { "ttl must be positive" }
            return OneShotJsonServer(json, ttlMs)
        }
    }
}
