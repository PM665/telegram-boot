package io.github.pm665.telegramboot.features.chatstates

import io.github.pm665.telegramboot.ports.ChatStatesService
import java.util.concurrent.ConcurrentHashMap

class InMemoryChatStatesService : ChatStatesService {
    private val storage = ConcurrentHashMap<Pair<String, Long>, ChatStatesService.ChatState>()

    override fun getChatState(
        chatId: Long,
        botUsername: String,
    ): ChatStatesService.ChatState? = storage[botUsername to chatId]

    override fun setChatState(state: ChatStatesService.ChatState) {
        storage[state.botUsername to state.chatId] = state
    }

    override fun deleteChatState(
        chatId: Long,
        botUsername: String,
    ) {
        storage.remove(botUsername to chatId)
    }
}
