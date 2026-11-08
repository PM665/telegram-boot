package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

internal class MaxApiClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {
    fun botInfo(token: String): JsonNode =
        restClient
            .get()
            .uri("/me")
            .header(AUTHORIZATION, token)
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: error("MAX returned no bot information")

    fun subscribe(
        token: String,
        webhookUrl: String,
        secret: String,
    ) {
        restClient
            .post()
            .uri("/subscriptions")
            .header(AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "url" to webhookUrl,
                    "update_types" to listOf("message_created", "message_callback"),
                    "secret" to secret,
                ),
            ).retrieve()
            .toBodilessEntity()
    }

    fun unsubscribe(
        token: String,
        webhookUrl: String,
    ) {
        restClient
            .delete()
            .uri("/subscriptions?url={url}", webhookUrl)
            .header(AUTHORIZATION, token)
            .retrieve()
            .toBodilessEntity()
    }

    fun sendMessage(
        token: String,
        chatId: Long,
        body: Map<String, Any>,
    ): JsonNode =
        restClient
            .post()
            .uri("/messages?user_id={chatId}", chatId)
            .header(AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String::class.java)
            ?.let(objectMapper::readTree)
            ?: error("MAX returned no message")

    fun answerCallback(
        token: String,
        callbackId: String,
        message: Map<String, Any>? = null,
    ) {
        val uri = "/answers?callback_id=${UriUtils.encodeQueryParam(callbackId, StandardCharsets.UTF_8)}"
        val request =
            restClient
                .post()
                .uri(uri)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
        if (message == null) {
            request.retrieve().toBodilessEntity()
        } else {
            request.body(mapOf("message" to message)).retrieve().toBodilessEntity()
        }
    }

    fun deleteMessage(
        token: String,
        messageId: String,
    ) {
        restClient
            .delete()
            .uri("/messages?message_id={messageId}", messageId)
            .header(AUTHORIZATION, token)
            .retrieve()
            .toBodilessEntity()
    }

    fun setCommands(
        token: String,
        commands: List<Map<String, String>>,
    ) {
        restClient
            .patch()
            .uri("/me/commands")
            .header(AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("commands" to commands))
            .retrieve()
            .toBodilessEntity()
    }

    companion object {
        private const val AUTHORIZATION = "Authorization"
    }
}
