package io.github.pm665.telegramboot.admin.security

import org.springframework.stereotype.Component
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class TelegramLoginVerifier(
    private val properties: TelegramAdminProperties,
) {
    fun verify(payload: TelegramLoginPayload): Boolean {
        if (payload.hash.isBlank()) {
            return false
        }
        if (properties.telegramLoginBotToken.isBlank()) {
            return false
        }

        val ageSeconds = Instant.now().epochSecond - payload.authDate
        if (ageSeconds < 0 || ageSeconds > properties.telegramLoginMaxAgeSeconds) {
            return false
        }

        val calculatedHash = calculateHash(payload, properties.telegramLoginBotToken)
        return calculatedHash.equals(payload.hash, ignoreCase = true)
    }

    fun verifyMiniApp(initData: String): TelegramMiniAppPayload? {
        if (initData.isBlank() || properties.telegramLoginBotToken.isBlank()) {
            return null
        }

        val parsed = parseInitData(initData)
        val receivedHash = parsed["hash"] ?: return null
        val authDate = parsed["auth_date"]?.toLongOrNull() ?: return null
        val userJson = parsed["user"] ?: return null

        val ageSeconds = Instant.now().epochSecond - authDate
        if (ageSeconds < 0 || ageSeconds > properties.telegramLoginMaxAgeSeconds) {
            return null
        }

        val dataCheckString =
            parsed
                .filterKeys { it != "hash" }
                .toSortedMap()
                .entries
                .joinToString("\n") { "${it.key}=${it.value}" }

        val calculatedHash = calculateMiniAppHash(dataCheckString, properties.telegramLoginBotToken)
        if (!calculatedHash.equals(receivedHash, ignoreCase = true)) {
            return null
        }

        val telegramUser = extractTelegramUser(userJson) ?: return null
        return TelegramMiniAppPayload(
            userId = telegramUser.first,
            username = telegramUser.second,
            authDate = authDate,
        )
    }

    internal fun calculateHash(
        payload: TelegramLoginPayload,
        botToken: String,
    ): String {
        val dataCheckString = payload.toDataMap().entries.sortedBy { it.key }.joinToString("\n") { "${it.key}=${it.value}" }
        val secretKey = MessageDigest.getInstance("SHA-256").digest(botToken.toByteArray(StandardCharsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey, "HmacSHA256"))
        val digest = mac.doFinal(dataCheckString.toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) }
    }

    internal fun calculateMiniAppHash(
        dataCheckString: String,
        botToken: String,
    ): String {
        val secretKeyMac = Mac.getInstance("HmacSHA256")
        secretKeyMac.init(SecretKeySpec("WebAppData".toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val secretKey = secretKeyMac.doFinal(botToken.toByteArray(StandardCharsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey, "HmacSHA256"))
        val digest = mac.doFinal(dataCheckString.toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseInitData(initData: String): Map<String, String> =
        initData
            .split("&")
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) {
                    null
                } else {
                    val key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8)
                    val value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8)
                    key to value
                }
            }.toMap()

    private fun extractTelegramUser(userJson: String): Pair<Long, String?>? {
        val idRegex = Regex("\"id\"\\s*:\\s*(\\d+)")
        val usernameRegex = Regex("\"username\"\\s*:\\s*\"([^\"]+)\"")

        val userId = idRegex.find(userJson)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val username = usernameRegex.find(userJson)?.groupValues?.get(1)

        return userId to username
    }
}

data class TelegramLoginPayload(
    val id: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val photoUrl: String? = null,
    val authDate: Long,
    val hash: String,
) {
    fun toDataMap(): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        fields["id"] = id.toString()
        firstName?.takeIf { it.isNotBlank() }?.let { fields["first_name"] = it }
        lastName?.takeIf { it.isNotBlank() }?.let { fields["last_name"] = it }
        username?.takeIf { it.isNotBlank() }?.let { fields["username"] = it }
        photoUrl?.takeIf { it.isNotBlank() }?.let { fields["photo_url"] = it }
        fields["auth_date"] = authDate.toString()
        return fields
    }
}

data class TelegramMiniAppPayload(
    val userId: Long,
    val username: String?,
    val authDate: Long,
)
