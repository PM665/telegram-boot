package io.github.pm665.telegramboot.features.admin

class BroadcastMessage(
    val sourceChatId: Long,
    val botUsername: String,
    val message: String,
)
