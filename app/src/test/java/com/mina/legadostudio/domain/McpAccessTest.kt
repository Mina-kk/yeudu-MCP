package com.mina.legadostudio.domain
import com.mina.legadostudio.mcp.McpAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
class McpAccessTest {
    @Test fun exposesLoopbackFirst() {
        assertEquals(listOf("http://127.0.0.1:1237/mcp"), McpAccess.endpoints(1237))
    }
    @Test fun tokenHeaderLineIncludesNameAndValue() {
        assertEquals("X-Studio-Token: secret", McpAccess.tokenHeaderLine("secret"))
    }
    @Test fun keepsLoopbackAllowed() {
        val hosts = McpAccess.allowedHosts(listOf(InetAddress.getByName("192.168.1.8")))
        assertTrue(hosts.contains("localhost"))
        assertTrue(hosts.contains("127.0.0.1"))
    }
    @Test fun lanEndpointsAreLanIpv4Only() {
        val urls = McpAccess.lanEndpoints(58823)
        urls.forEach { url ->
            assertTrue(url.startsWith("http://"))
            assertTrue(url.endsWith(":58823/mcp"))
            assertTrue(!url.contains("127.0.0.1"))
            val host = url.removePrefix("http://").removeSuffix(":58823/mcp")
            assertTrue(host.all { it.isDigit() || it == '.' })
            assertTrue(host.count { it == '.' } == 3)
        }
    }
}
