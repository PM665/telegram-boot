package io.github.pm665.telegramboot.ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class TelegramAiService(
    chatModel: ChatModel,
    private val tools: List<AiTools>,
) {
    private val chatClient = ChatClient.builder(chatModel).build()
    private val systemPrompt = ClassPathResource("prompts/system-prompt.txt")

    fun processMessage(message: String): String =
        chatClient
            .prompt()
            .system(systemPrompt)
            .user(message)
            .tools(*tools.toTypedArray())
            .call()
            .content()
            .orEmpty()
}
