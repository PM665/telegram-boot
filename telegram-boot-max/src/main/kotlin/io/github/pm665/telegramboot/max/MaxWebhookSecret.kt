package io.github.pm665.telegramboot.max

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

internal object MaxWebhookSecret {
    fun fromToken(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8)))

    fun matches(
        token: String,
        candidate: String?,
    ): Boolean =
        MessageDigest.isEqual(
            fromToken(token).toByteArray(StandardCharsets.UTF_8),
            candidate?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0),
        )
}
