package com.mina.legadostudio.domain

import com.mina.legadostudio.mcp.McpConfigStore
import org.junit.Assert.assertEquals
import org.junit.Test

class McpConfigStoreTest {
    @Test fun defaultPortIs58823() {
        assertEquals(58823, McpConfigStore.DEFAULT_PORT)
    }

    @Test fun legacyDefaultPortMigratesButCustomPortIsPreserved() {
        assertEquals(58823, McpConfigStore.migrateLegacyDefaultPort(1237))
        assertEquals(45678, McpConfigStore.migrateLegacyDefaultPort(45678))
    }
}
