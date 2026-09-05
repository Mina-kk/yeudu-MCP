package com.mina.legadostudio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFormProbeTest {
    @Test fun rejectsLoginFormAsSearchProbe() {
        val login = """
            <form method="post" action="/user/user/login.html">
              <input type="text" name="username" placeholder="请输入用户名">
              <input type="password" name="password" placeholder="请输入密码">
            </form>
        """.trimIndent()
        org.junit.Assert.assertNull(SearchFormProbe.detect(login, "http://www.biquge.pro/"))
    }

    @Test fun detectsEmpireCmsSearchForm() {
        val html = """
            <form method="post" action="/e/search/index.php">
              <input type="hidden" name="tbname" value="bookname" />
              <input type="hidden" name="show" value="title,writer" />
              <input type="hidden" name="tempid" value="1" />
              <input type="text" name="keyboard" placeholder="请输入书名或作者" />
            </form>
        """.trimIndent()
        val form = SearchFormProbe.detect(html, "https://www.shuzhaige.com/")!!
        assertEquals("POST", form.method)
        assertEquals("keyboard", form.keywordField)
        assertTrue(form.action.endsWith("/e/search/index.php"))
        val body = form.encode("万相之王")
        assertTrue(body.contains("tbname=bookname"))
        assertTrue(body.contains("keyboard="))
    }
}
