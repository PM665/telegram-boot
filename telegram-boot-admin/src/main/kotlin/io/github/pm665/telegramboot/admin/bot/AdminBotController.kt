package io.github.pm665.telegramboot.admin.bot

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.telegram.telegrambots.meta.api.objects.Update
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@RestController
@Conditional(AdminBotEnabledCondition::class)
class AdminBotController(
    private val adminBot: AdminBot,
) {
    private val objectMapper = ObjectMapper()

    @PostMapping("/bot/{botUsername}")
    fun receiveUpdate(
        @PathVariable botUsername: String,
        @RequestHeader(TELEGRAM_SECRET_HEADER, required = false) secretToken: String?,
        @RequestBody rawUpdate: String,
    ): ResponseEntity<Void> {
        if (botUsername != adminBot.botUsername) {
            return ResponseEntity.notFound().build()
        }
        if (!hasValidSecret(secretToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val update =
            try {
                objectMapper.readValue(rawUpdate, Update::class.java)
            } catch (_: JsonProcessingException) {
                return ResponseEntity.badRequest().build()
            }
        adminBot.handle(update)
        return ResponseEntity.ok().build()
    }

    private fun hasValidSecret(secretToken: String?): Boolean =
        MessageDigest.isEqual(
            adminBot.secretToken.toByteArray(StandardCharsets.UTF_8),
            secretToken?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0),
        )

    companion object {
        const val TELEGRAM_SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token"
    }
}
