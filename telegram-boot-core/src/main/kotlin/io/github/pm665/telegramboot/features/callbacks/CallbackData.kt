package io.github.pm665.telegramboot.features.callbacks

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.HexFormat

data class CallbackData(
    val id: String,
    val data: String,
) {
    companion object {
        private const val CALLBACK_ID_HASH_LENGTH = 8

        fun fromData(data: String): CallbackData =
            CallbackData(
                id = CallbackDataService.PREFIX + shortHash(data),
                data = data,
            )

        private fun shortHash(value: String): String {
            val digest = sha256Hex(value)
            if (digest.length < CALLBACK_ID_HASH_LENGTH) {
                return digest
            }
            return digest.take(CALLBACK_ID_HASH_LENGTH)
        }

        private fun sha256Hex(value: String): String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(value.toByteArray(StandardCharsets.UTF_8))
                return HexFormat.of().formatHex(hash)
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException("SHA-256 algorithm is not available", e)
            }
        }
    }
}
