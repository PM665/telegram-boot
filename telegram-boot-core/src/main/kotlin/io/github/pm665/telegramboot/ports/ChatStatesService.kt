package io.github.pm665.telegramboot.ports

interface ChatStatesService {
    fun getChatState(
        chatId: Long,
        botUsername: String,
    ): ChatState?

    fun setChatState(state: ChatState)

    fun deleteChatState(
        chatId: Long,
        botUsername: String,
    )

    data class ChatState(
        val chatId: Long,
        val botUsername: String,
        val command: String,
        val state: String,
    ) {
        fun `is`(
            command: String,
            state: String,
        ): Boolean = this.command == command && this.state == state
    }
}
