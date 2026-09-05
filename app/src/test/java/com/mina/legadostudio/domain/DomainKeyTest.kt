package com.mina.legadostudio.domain

import com.mina.legadostudio.verification.DomainKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DomainKeyTest {
    @Test fun sharesCookiesAcrossNormalSubdomains() {
        assertEquals("example.com", DomainKey.fromUrl("https://www.example.com/a"))
        assertEquals("example.com", DomainKey.fromUrl("https://search.example.com/b"))
    }

    @Test fun keepsKnownCompoundPublicSuffix() {
        assertEquals("example.com.cn", DomainKey.fromUrl("https://api.example.com.cn/a"))
        assertEquals("example.co.uk", DomainKey.fromUrl("https://www.example.co.uk/a"))
    }

    @Test fun keepsIpAndLocalhost() {
        assertEquals("192.168.1.8", DomainKey.fromUrl("http://192.168.1.8:1237/mcp"))
        assertEquals("localhost", DomainKey.fromUrl("http://localhost:1237/health"))
    }
}
