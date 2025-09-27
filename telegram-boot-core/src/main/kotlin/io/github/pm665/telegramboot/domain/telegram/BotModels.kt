package io.github.pm665.telegramboot.domain.telegram

data class Bot(
    val botUsername: String,
    val botToken: String,
    val enabled: Boolean,
)

data class BotUser(
    val botUserId: Long,
    val botUserName: String,
    val registered: Boolean,
)

data class BotChat(
    val botUsername: String,
    val botUserId: Long,
    val botChatId: Long,
)
