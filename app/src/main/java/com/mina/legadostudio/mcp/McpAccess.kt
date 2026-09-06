package com.mina.legadostudio.mcp

import java.net.InetAddress
import java.net.NetworkInterface

object McpAccess {
    const val PATH = "/mcp"
    const val TOKEN_HEADER = "X-Studio-Token"

    fun localAddresses(): List<InetAddress> = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
        .flatMap { it.inetAddresses.toList() }
        .filter { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
        .distinctBy { it.hostAddress }

    fun allowedHosts(addresses: List<InetAddress>): List<String> = buildList {
        add("localhost"); add("127.0.0.1"); add("[::1]")
        addresses.mapNotNullTo(this) { it.hostAddress }
    }.distinct()

    fun allowedOrigins(hosts: List<String>): List<String> = hosts.map { "http://$it" }

    // 对外只暴露回环地址，避免切换 Wi-Fi/蜂窝后 IP 变化导致 MCP 客户端断连。
    fun endpoints(port: Int): List<String> = listOf("http://127.0.0.1:$port$PATH")

    fun lanEndpoints(port: Int): List<String> =
        localAddresses().mapNotNull { it.hostAddress }.map { "http://$it:$port$PATH" }

    fun tokenHeaderLine(token: String) = "$TOKEN_HEADER: $token"
}
