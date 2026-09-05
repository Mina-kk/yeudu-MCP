package com.mina.legadostudio.diagnostic

object LogRedactor {
    private val secret = Regex("(?i)(authorization|api[-_ ]?key|token)\\s*[:=]\\s*[^,;\\s]+")

    fun redact(value: String): String = value.replace(secret, "$1=***")
}
