package io.github.pm665.telegramboot.admin.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TelegramLoginVerifierTest {
    private val properties = TelegramAdminProperties(telegramLoginBotToken = "bot-token", telegramLoginMaxAgeSeconds = 300)
    private val verifier = TelegramLoginVerifier(properties)

    @Test
    fun `accepts valid payload hash`() {
        val authDate = (System.currentTimeMillis() / 1000) - 5
        val payload =
            TelegramLoginPayload(
                id = 123456,
                firstName = "John",
                username = "john",
                authDate = authDate,
                hash = "",
            )
        val hash = buildHash(payload, properties.telegramLoginBotToken)

        assertTrue(verifier.verify(payload.copy(hash = hash)))
    }

    @Test
    fun `rejects invalid hash`() {
        val authDate = (System.currentTimeMillis() / 1000) - 5
        val payload = TelegramLoginPayload(id = 123456, username = "john", authDate = authDate, hash = "bad-hash")

        assertFalse(verifier.verify(payload))
    }

    @Test
    fun `rejects payload when bot token config missing`() {
        val authDate = (System.currentTimeMillis() / 1000) - 5
        val payload = TelegramLoginPayload(id = 123456, username = "john", authDate = authDate, hash = "")
        val hash = buildHash(payload, "")
        val verifierWithMissingToken = TelegramLoginVerifier(TelegramAdminProperties(telegramLoginBotToken = ""))

        assertFalse(verifierWithMissingToken.verify(payload.copy(hash = hash)))
    }

    @Test
    fun `rejects expired payload`() {
        val authDate = (System.currentTimeMillis() / 1000) - 1000
        val payload =
            TelegramLoginPayload(
                id = 123456,
                username = "john",
                authDate = authDate,
                hash = "",
            )
        val hash = buildHash(payload, properties.telegramLoginBotToken)

        assertFalse(verifier.verify(payload.copy(hash = hash)))
    }

    @Test
    fun `accepts valid mini app init data`() {
        val authDate = (System.currentTimeMillis() / 1000) - 5
        val initData =
            buildMiniAppInitData(
                authDate = authDate,
                userJson = "{\"id\":123456,\"username\":\"john\"}",
                botToken = properties.telegramLoginBotToken,
            )

        val result = verifier.verifyMiniApp(initData)

        assertNotNull(result)
        assertTrue(result!!.userId == 123456L)
        assertTrue(result.username == "john")
    }

    @Test
    fun `rejects mini app init data with bad hash`() {
        val authDate = (System.currentTimeMillis() / 1000) - 5
        val initData =
            buildMiniAppInitData(
                authDate = authDate,
                userJson = "{\"id\":123456,\"username\":\"john\"}",
                botToken = properties.telegramLoginBotToken,
            ) + "x"

        assertNull(verifier.verifyMiniApp(initData))
    }

    private fun buildHash(
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

    private fun buildMiniAppInitData(
        authDate: Long,
        userJson: String,
        botToken: String,
    ): String {
        val params =
            linkedMapOf(
                "auth_date" to authDate.toString(),
                "query_id" to "AAHdF6IQAAAAAN0XohDhrOrc",
                "user" to userJson,
            )

        val dataCheckString = params.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }
        val hash = buildMiniAppHash(dataCheckString, botToken)
        val encodedParams =
            params.entries.joinToString("&") {
                "${URLEncoder.encode(it.key, StandardCharsets.UTF_8)}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"
            }

        return "$encodedParams&hash=$hash"
    }

    private fun buildMiniAppHash(
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
}
