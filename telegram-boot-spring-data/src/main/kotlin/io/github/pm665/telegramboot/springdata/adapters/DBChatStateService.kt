package io.github.pm665.telegramboot.springdata.adapters

import io.github.pm665.telegramboot.ports.ChatStatesService
import io.github.pm665.telegramboot.springdata.ports.ChatStatesRepository
import org.springframework.transaction.annotation.Transactional

open class DBChatStateService(
    private val chatStatesRepository: ChatStatesRepository,
) : ChatStatesService {
    override fun getChatState(
        chatId: Long,
        botUsername: String,
    ): ChatStatesService.ChatState? = chatStatesRepository.findFirstByChatIdAndBotUsername(chatId, botUsername)?.toBackendChatState()

    @Transactional
    override fun setChatState(state: ChatStatesService.ChatState) {
        chatStatesRepository.upsert(state.chatId, state.botUsername, state.command, state.state)
    }

    override fun deleteChatState(
        chatId: Long,
        botUsername: String,
    ) {
        chatStatesRepository.deleteByChatIdAndBotUsername(chatId, botUsername)
    }
}
