package io.github.pm665.telegramboot.ports

fun interface BotAdminAutoApproval {
    fun isParkContact(
        botUsername: String,
        username: String?,
    ): Boolean
}
