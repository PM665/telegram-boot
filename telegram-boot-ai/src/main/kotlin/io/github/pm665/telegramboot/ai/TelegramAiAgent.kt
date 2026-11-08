package io.github.pm665.telegramboot.ai

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(TelegramAiService::class, TelegramAiTools::class)
class TelegramAiAgent(
    private val service: TelegramAiService,
) {
    fun processMessage(message: String): String = service.processMessage(message)
}
