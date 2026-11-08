package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.annotations.BotCommand

interface PermissionsServiceAdapter {
    fun checkUserAccessToCommand(
        userChat: UsersService.UserChat,
        command: BotCommand,
        permissionsService: PermissionsService,
    ): Boolean
}
