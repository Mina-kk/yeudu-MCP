package com.mina.legadostudio.domain

import com.google.gson.Gson
import com.mina.legadostudio.network.HttpFetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundaryNullRegressionTest {
    private val gson = Gson()

    @Test fun fetchRequestMissingUrlShowsFriendlyValidationInsteadOfStartsWithCrash() {
        val request = gson.fromJson("{}", HttpFetcher.FetchRequest::class.java)
        val error = runCatching { HttpFetcher().fetch(request) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("请先填写要抓取的网址", error?.message)
    }
}