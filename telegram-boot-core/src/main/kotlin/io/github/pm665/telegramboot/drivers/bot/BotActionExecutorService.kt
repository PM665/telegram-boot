package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.ActionExecutionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.CommandAndState
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.drivers.bot.BotErrorEventListener.BotErrorEvent
import io.github.pm665.telegramboot.infrastructure.TaskExecutorService
import io.github.pm665.telegramboot.infrastructure.metrics.CommandMetricsService
import io.github.pm665.telegramboot.ports.ChatStatesService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.MessageSender.SendMessageContext
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class BotActionExecutorService(
    private val chatStatesService: ChatStatesService,
    private val botActionLocatorService: BotActionLocatorService,
    private val messageSender: MessageSender,
    private val taskExecutorService: TaskExecutorService,
    private val permissionsService: PermissionsService,
    private val usersService: UsersService,
    private val commandMetricsService: CommandMetricsService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(BotActionExecutorService::class.java)

    fun executeActionForCommandAndState(
        commandAndState: CommandAndState,
        botUsername: String,
        update: BotUpdate,
    ) {
        val context = ActionExecutionContext().botUsername(botUsername).update(update)
        val botActionExecutor =
            botActionLocatorService.findBotAction(
                botUsername,
                commandAndState.command,
                commandAndState.state,
                commandAndState.callback,
            )
        if (botActionExecutor != null) {
            context.botActionExecutor(botActionExecutor)
            executeActionPreWork(context)
        } else {
            notifyActionNotFound(context)
            executePostAction(context)
        }
    }

    private fun notifyActionNotFound(context: ActionExecutionContext) {
        val payloadData = requireNotNull(context.payloadData) { "Payload data is required" }
        val botUsername = requireNotNull(context.botUsername) { "Bot username is required" }
        taskExecutorService.executeImmediately {
            messageSender.sendCallbackNotification(
                payloadData.update,
                "error.invalidCommand",
                mapOf(),
                botUsername,
            )
        }
    }

    private fun executeActionPreWork(context: ActionExecutionContext) {
        val botUsername = requireNotNull(context.botUsername) { "Bot username is required" }
        val command = requireNotNull(context.command) { "Bot command is required" }
        val maybeUser = ChatUtils.extractUser(context.update)

        val identifiedUser =
            if (maybeUser != null) {
                // extractUser may contain nullable username/locale coming from Telegram API; use safe defaults
                val username = maybeUser.username ?: "unknown"
                val locale = maybeUser.locale
                usersService.getOrCreateUser(botUsername, maybeUser.userId, username, locale)
            } else {
                usersService.createNewUser(botUsername, context.chatId, "<noname>", "")
            }

        val userChat =
            usersService.getOrCreateUserChat(
                identifiedUser.userId,
                identifiedUser.username ?: "unknown",
                identifiedUser.locale,
                botUsername,
                context.chatId,
            )

        val userEligible =
            permissionsService.hasUserAccessToCommand(
                botUsername = botUsername,
                userChat = userChat,
                command = command,
            )
        if (!userEligible) {
            log.warn(
                "User is not eligible to execute the command (bot={}, command={}, username={})",
                botUsername,
                context.actionId,
                maybeUser?.username,
            )
            notifyActionNotFound(context)
            executePostAction(context)
            return
        }

        context.update?.takeIf { it.callbackQuery != null }?.let {
            messageSender.sendCallbackNotification(it, "", emptyMap(), botUsername)
        }

        context.userChat = userChat

        val chatId = context.chatId
        val chatState = getChatState(chatId, botUsername)
        if (chatState != null) context.chatState = chatState

        if (context.shouldDeleteChatStateBeforeExecution) {
            if (!context.requiresConfirmation) {
                val existingState = context.chatState
                if (existingState != null) {
                    commandMetricsService.commandForgotten(botUsername, existingState.chatId, existingState.command)
                }
            }
            deleteChatState(chatId, botUsername)
        }

        taskExecutorService.executeImmediately {
            if (context.shouldDeleteCallbackSource) {
                messageSender.deleteCallbackSource(context.update, botUsername)
                context.setCallbackSourceDeleted()
            }
        }

        if (context.requiresConfirmation) {
            sendConfirmation(context)
        } else {
            taskExecutorService.executeImmediately {
                if (context.shouldExecuteImmediately) {
                    executeAction(context)
                } else {
                    val sentMessage = messageSender.sendTextMessage("common.needToWait", chatId, botUsername)
                    val randomDelay = context.randomDelay()
                    taskExecutorService.executeDelayed({
                        executeAction(context)
                        if (sentMessage != null) {
                            messageSender.deleteMessage(sentMessage.messageId, sentMessage.chatId, botUsername)
                        }
                    }, randomDelay)
                }
            }
        }
    }

    private fun sendConfirmation(context: ActionExecutionContext) {
        val botUsername = requireNotNull(context.botUsername) { "Bot username is required" }
        val command = requireNotNull(context.command) { "Bot command is required" }
        val text = requireNotNull(context.confirmation) { "Confirmation is required" }.message
        val msg = SendMessageMarkup(text = text, menuMarkup = context.confirmationOptions)
        taskExecutorService.executeImmediately {
            messageSender.sendMessage(msg, context.chatId, botUsername)
            messageSender.deleteCallbackSource(context.update, botUsername)
        }
        setChatState(context.chatId, botUsername, command.name, context.confirmationState)
        commandMetricsService.commandInFlight(
            botUsername,
            context.chatId,
            command.name,
            context.confirmationState,
            CommandMetricsService.UNKNOWN_STATUS,
        )
    }

    private fun executeAction(context: ActionExecutionContext) {
        try {
            executeActionInternal(context)
        } catch (e: RuntimeException) {
            log.error("Error occurred during execution of the action", e)
            val botUsername = requireNotNull(context.botUsername) { "Bot username is required" }
            applicationEventPublisher.publishEvent(BotErrorEvent(botUsername, context.update, e))
            messageSender.sendTextMessage("error.unknownError", context.chatId, botUsername)
        }
    }

    private fun executeActionInternal(context: ActionExecutionContext) {
        val botUsername = requireNotNull(context.botUsername) { "Bot username is required" }
        val payloadData = requireNotNull(context.payloadData) { "Payload data is required" }
        log.debug(
            "Executing action bot={} command={} state={} callback={} chatId={} payload='{}'",
            botUsername,
            context.command?.name,
            context.action?.onState,
            context.action?.onCallback,
            context.chatId,
            payloadData.message,
        )
        val execution = requireNotNull(context.botActionExecutor) { "Bot action executor is required" }.execute(botUsername, payloadData)
        context.setActionResult(execution)

        val commandName = requireNotNull(context.command) { "Bot command is required" }.name
        val chatId = context.chatId
        log.debug(
            "Action finished bot={} command={} status={} transitionToState={} messages={} locations={}",
            botUsername,
            commandName,
            context.status,
            context.transitionToState,
            context.messagesToSend.size,
            context.locationsToSend.size,
        )

        if (context.hasMessagesToSend) {
            val ctx =
                SendMessageContext
                    .builder()
                    .actionId(context.actionId)
                    .messagesToSend(context.messagesToSend)
                    .messageArgs(context.messageArgs)
                    .locationsToSend(context.locationsToSend)
                    .chat(context.chatId)
                    .botUsername(botUsername)
                    .build()
            messageSender.sendMessages(ctx)
        }

        if (context.shouldTransitionImmediately) {
            val transitionToState = requireNotNull(context.transitionToState) { "Transition state is required" }
            val update = requireNotNull(context.update) { "Update is required" }
            executeActionForCommandAndState(CommandAndState(commandName, transitionToState, null), botUsername, update)
        } else if (context.hasTransitionToState) {
            val transitionToState = requireNotNull(context.transitionToState) { "Transition state is required" }
            setChatState(chatId, botUsername, commandName, transitionToState)
            commandMetricsService.commandInFlight(
                botUsername,
                chatId,
                commandName,
                transitionToState,
                context.status ?: CommandMetricsService.UNKNOWN_STATUS,
            )
        } else {
            commandMetricsService.commandCompleted(
                botUsername,
                chatId,
                commandName,
                context.status ?: CommandMetricsService.UNKNOWN_STATUS,
            )
            deleteChatState(chatId, botUsername)
            if (context.shouldExecutePostAction) {
                executePostAction(context)
            }
        }
    }

    private fun executePostAction(context: ActionExecutionContext) {
        val botUsername = requireNotNull(context.botUsername) { "Bot username is required" }
        val update = requireNotNull(context.update) { "Update is required" }
        botActionLocatorService
            .findBotAction(botUsername, BotCommand.COMMAND_START, BotAction.INIT_STATE, null)
            ?.let { executor ->
                executeAction(
                    ActionExecutionContext()
                        .botUsername(botUsername)
                        .update(update)
                        .botActionExecutor(executor),
                )
            }
    }

    private fun getChatState(
        chatId: Long,
        botUsername: String,
    ) = chatStatesService.getChatState(chatId, botUsername)

    private fun setChatState(
        chatId: Long,
        botUsername: String,
        commandName: String,
        transitionToState: String,
    ) {
        chatStatesService.setChatState(ChatStatesService.ChatState(chatId, botUsername, commandName, transitionToState))
    }

    private fun deleteChatState(
        chatId: Long,
        botUsername: String,
    ) {
        chatStatesService.deleteChatState(chatId, botUsername)
    }
}
