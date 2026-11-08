package io.github.pm665.telegramboot.ports.support

import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.properties.ReplyStatus

object ActionResults {
    fun contextError(actionId: String): ActionResult = ActionResult.create(ReplyStatus.contextError).actionId(actionId).build()
}
