package io.github.pm665.telegramboot.features.start

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.Visibility
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.ports.BotCommandsProvider
import io.github.pm665.telegramboot.ports.BotFeatureSupport
import io.github.pm665.telegramboot.ports.PermissionsService

@BotCommand(name = "start", accessType = AccessType.ALL, visibility = Visibility.HIDDEN, keepMessage = true)
class StartCommand(
    private val commands: BotCommandsProvider,
    private val permissions: PermissionsService,
    private val features: BotFeatureSupport,
) {
    @BotAction
    fun start(context: BotActionContext): ActionResult {
        val menu = SendMessageMarkup.MenuMarkup(rich = true)
        commands.findAllBotCommands()
            .filter { it.visibility == Visibility.VISIBLE }
            .filter { permissions.hasUserAccessToCommand(context.botUsername, context.user, it) }
            .filter { it.features.all { feature -> features.isSupported(context.botUsername, feature) } }
            .groupBy { it.row }.toSortedMap()
            .forEach { (_, commands) ->
                commands.sortedBy { it.order }.chunked(8).forEach { group ->
                    val row = menu.addRow()
                    group.forEach { command ->
                        row.addButton(command.displayName.ifBlank { command.name }, "/${command.name}")
                    }
                }
            }
        return ActionResult.builder().messagesToSend(listOf(SendMessageMarkup("Commands", menuMarkup = menu))).build()
    }
}
