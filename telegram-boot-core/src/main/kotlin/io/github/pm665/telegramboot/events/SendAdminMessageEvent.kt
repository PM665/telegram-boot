package io.github.pm665.telegramboot.events

data class SendAdminMessageEvent(
    val message: String,
    val recipientsUserIds: Collection<Long>,
    val tag: String?,
    val buttons: Collection<AdminMessageButton> = emptyList(),
    val deleteMessageId: Int? = null,
    val richHtml: String? = null,
    val editChatId: Long? = null,
    val editMessageId: Int? = null,
    val requestId: String? = null,
    val navigationTerminal: Boolean = false,
    val navigationReplace: Boolean = false,
) {
    companion object {
        const val HEALTH_TAG = "health"
    }
}

data class AdminMessageButton(
    val text: String,
    val callbackData: String,
)

data class AdminBotMessageSentEvent(
    val requestId: String,
    val recipientUserId: Long,
    val chatId: Long,
    val messageId: Int,
)

data class AdminBotActionEvent(
    val userId: Long,
    val chatId: Long,
    val message: String?,
    val callbackData: String?,
    val sourceMessageId: Int? = null,
    val navigationBack: Boolean = false,
) {
    val commandText: String?
        get() = callbackData ?: message
}
