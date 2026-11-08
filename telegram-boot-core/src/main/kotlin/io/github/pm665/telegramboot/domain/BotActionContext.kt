package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.ChatUtils.PayloadData
import io.github.pm665.telegramboot.ports.UsersService.UserChat

open class BotActionContext(
    val botUsername: String,
    val payloadData: PayloadData,
    val user: UserChat?,
    val baseMessageArgs: Map<String, Any> = emptyMap(),
)
