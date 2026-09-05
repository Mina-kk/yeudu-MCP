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
}
