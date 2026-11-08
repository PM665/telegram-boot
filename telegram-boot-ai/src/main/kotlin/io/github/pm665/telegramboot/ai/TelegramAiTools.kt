package io.github.pm665.telegramboot.ai

import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TelegramAiTools : AiTools {
    @Tool(description = "Get the current UTC date and time")
    fun currentUtcDateTime(): String = Instant.now().toString()

    @Tool(description = "Add two integer numbers")
    fun add(first: Int, second: Int): Int = first + second
}
