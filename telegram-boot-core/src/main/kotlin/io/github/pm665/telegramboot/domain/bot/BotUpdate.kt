package io.github.pm665.telegramboot.domain.bot

import java.time.Instant

data class BotUpdate(
    val updateId: String? = null,
    val message: BotIncomingMessage? = null,
    val editedMessage: BotIncomingMessage? = null,
    val channelPost: BotIncomingMessage? = null,
    val editedChannelPost: BotIncomingMessage? = null,
    val callbackQuery: BotCallbackQuery? = null,
    val myChatMember: BotChatMemberUpdate? = null,
    val chatMember: BotChatMemberUpdate? = null,
)

data class BotIncomingMessage(
    val messageId: String? = null,
    val chat: BotChatInfo? = null,
    val from: BotUserInfo? = null,
    val text: String? = null,
    val caption: String? = null,
    val entities: List<BotMessageEntity> = emptyList(),
    val location: BotLocation? = null,
    val photos: List<BotFileReference> = emptyList(),
    val hasDocument: Boolean = false,
    val isReply: Boolean = false,
)

data class BotCallbackQuery(
    val id: String,
    val data: String?,
    val from: BotUserInfo?,
    val message: BotIncomingMessage?,
)

data class BotChatInfo(
    val id: Long,
    val type: BotChatType,
    val title: String? = null,
    val username: String? = null,
)

enum class BotChatType {
    PRIVATE,
    GROUP,
    SUPERGROUP,
    CHANNEL,
    UNKNOWN,
}

data class BotUserInfo(
    val id: Long,
    val username: String? = null,
    val language: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)

data class BotMessageEntity(
    val type: String,
    val offset: Int,
    val length: Int,
    val url: String? = null,
)

data class BotLocation(
    val latitude: Double,
    val longitude: Double,
)

data class BotChatMemberUpdate(
    val chat: BotChatInfo?,
    val date: Instant? = null,
    val newMember: BotChatMember?,
)

data class BotChatMember(
    val status: BotChatMemberStatus,
    val canPostMessages: Boolean? = null,
)

enum class BotChatMemberStatus {
    CREATOR,
    ADMINISTRATOR,
    MEMBER,
    RESTRICTED,
    LEFT,
    BANNED,
    UNKNOWN,
}

data class BotFileReference(
    val id: String,
    val uniqueId: String? = null,
)

data class BotMessage(
    val messageId: String,
    val chatId: Long,
    val files: List<BotFileReference> = emptyList(),
)
