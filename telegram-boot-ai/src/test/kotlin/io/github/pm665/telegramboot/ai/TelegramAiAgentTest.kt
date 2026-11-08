package io.github.pm665.telegramboot.ai

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.tool.annotation.Tool
import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets

class TelegramAiAgentTest {
    @Test
    fun `passes context and user message to model`() {
        val model = CapturingChatModel()
        val agent = TelegramAiAgent(TelegramAiService(model, listOf(TelegramAiTools(), MultiplicationTool())))
        val systemPrompt =
            ClassPathResource("prompts/system-prompt.txt").getContentAsString(StandardCharsets.UTF_8)

        assertThat(agent.processMessage("What can you do?")).isEqualTo("Nothing dramatic.")
        assertThat(model.prompt.instructions.map { it.text })
            .containsExactly(systemPrompt, "What can you do?")
        assertThat((model.prompt.options as ToolCallingChatOptions).toolCallbacks.orEmpty().map { it.toolDefinition.name() })
            .containsExactlyInAnyOrder("currentUtcDateTime", "add", "multiply")
    }

    private class MultiplicationTool : AiTools {
        @Tool(description = "Multiply two integer numbers")
        fun multiply(first: Int, second: Int): Int = first * second
    }

    private class CapturingChatModel : ChatModel {
        lateinit var prompt: Prompt

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        override fun call(prompt: Prompt): ChatResponse {
            this.prompt = prompt
            return ChatResponse(listOf(Generation(AssistantMessage("Nothing dramatic."))))
        }
    }
}
