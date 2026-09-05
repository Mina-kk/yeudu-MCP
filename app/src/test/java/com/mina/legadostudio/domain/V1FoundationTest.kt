package com.mina.legadostudio.domain

import com.google.gson.Gson
import com.mina.legadostudio.network.HttpFetcher
import com.mina.legadostudio.skills.SkillRepository
import com.mina.legadostudio.verification.VerificationRequiredException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1FoundationTest {
    @Test fun skillInfoUsesStableJsonFieldNames() {
        val json = Gson().toJson(SkillRepository.SkillInfo("legado-book-source", "书源技能", true, true))
        assertTrue(json.contains("\"id\":\"legado-book-source\""))
        assertTrue(json.contains("\"name\":\"书源技能\""))
        assertTrue(json.contains("\"builtIn\":true"))
        assertTrue(json.contains("\"enabled\":true"))
    }

    @Test fun wafResponseBecomesVerificationRequestInsteadOfRuleFailure() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("<html><title>Verify Yourself</title><form action='/WAF/VERIFY/CAPTCHA'></form></html>"))
            server.start()
            val error = runCatching { HttpFetcher().fetch(HttpFetcher.FetchRequest(server.url("/").toString())) }.exceptionOrNull()
            assertTrue("actual=$error", error is VerificationRequiredException)
            assertEquals(server.hostName, (error as VerificationRequiredException).domain)
        }
    }
}
