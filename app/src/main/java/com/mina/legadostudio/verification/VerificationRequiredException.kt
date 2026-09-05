package com.mina.legadostudio.verification

class VerificationRequiredException(
    val verificationUrl: String,
    val domain: String,
    message: String = "网站需要在 App 内完成验证",
) : Exception(message)
