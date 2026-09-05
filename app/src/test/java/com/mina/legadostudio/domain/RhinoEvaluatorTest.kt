package com.mina.legadostudio.domain

import com.google.gson.Gson
import com.mina.legadostudio.network.HttpFetcher
import com.mina.legadostudio.runtime.RhinoEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class RhinoEvaluatorTest {
    @Test fun executesOfficialRhinoAndCapturesLog() {
        val result = RhinoEvaluator(HttpFetcher(), Gson()).evaluate("java.log('hello'); 1 + 2")
        assertEquals("3.0", result.value)
        assertEquals(listOf("hello"), result.logs)
    }

    @Test fun exposesCommonLegadoJavaHelpers() {
        val result = RhinoEvaluator(HttpFetcher(), Gson()).evaluate("java.hexDecodeToString(java.hexEncodeToString('中文')) + ':' + String(java.sha256Encode('x')).length")
        assertEquals("中文:64", result.value)
    }
}
