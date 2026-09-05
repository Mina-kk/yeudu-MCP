package com.mina.legadostudio.export

import com.google.gson.JsonArray
import com.google.gson.JsonParser

object SourceImportPayload {
    fun arrayJson(sourceJson: String): String {
        val parsed = JsonParser.parseString(sourceJson)
        val array = when {
            parsed.isJsonArray -> parsed.asJsonArray
            parsed.isJsonObject -> JsonArray().also { it.add(parsed) }
            else -> error("书源 JSON 无效")
        }
        require(array.size() > 0) { "书源为空" }
        return array.toString()
    }
}
