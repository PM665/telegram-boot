package io.github.pm665.telegramboot.ports

import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils.PayloadData
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.ports.support.ActionResults

interface BotActionContextProvider<T : BotActionContext> {
    @Throws(IllegalArgumentException::class)
    fun resolve(
        botUsername: String,
        payloadData: PayloadData,
    ): T

    fun errorResult(
        botUsername: String,
        actionId: String,
    ): ActionResult = ActionResults.contextError(actionId)
}
