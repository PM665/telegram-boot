package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.core.JsonProcessingException
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.drivers.bot.BotUpdateHandler
import io.github.pm665.telegramboot.infrastructure.TaskExecutorService
import io.github.pm665.telegramboot.ports.BotsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
internal class MaxWebhookController(
    private val botsService: BotsService,
    private val mapper: MaxUpdateMapper,
    private val handler: BotUpdateHandler,
    private val taskExecutorService: TaskExecutorService,
) {
    @PostMapping("/max/bot/{botUsername}")
    fun receiveUpdate(
        @PathVariable botUsername: String,
        @RequestHeader(SECRET_HEADER, required = false) secret: String?,
        @RequestBody rawUpdate: String,
    ): ResponseEntity<Void> {
        val bot = botsService.findBotByBotUsername(botUsername)?.takeIf { it.platform == BotPlatform.MAX }
            ?: return ResponseEntity.notFound().build()
        if (!MaxWebhookSecret.matches(bot.token, secret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val update =
            try {
                mapper.from(rawUpdate)
            } catch (_: JsonProcessingException) {
                return ResponseEntity.badRequest().build()
            }
        if (update.message != null || update.callbackQuery != null) {
            taskExecutorService.executeImmediately { handler.handleUpdate(botUsername, update) }
        }
        return ResponseEntity.ok().build()
    }

    companion object {
        const val SECRET_HEADER = "X-Max-Bot-Api-Secret"
    }
}
