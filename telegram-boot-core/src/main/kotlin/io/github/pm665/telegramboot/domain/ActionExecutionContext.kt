package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.ChatUtils.PayloadData
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendLocationItem
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuButton
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuRow
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.annotations.CallbackOption
import io.github.pm665.telegramboot.domain.annotations.Confirmation
import io.github.pm665.telegramboot.domain.annotations.Reply
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.ChatStatesService.ChatState
import io.github.pm665.telegramboot.ports.UsersService.UserChat
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class ActionExecutionContext internal constructor() {
    private val afterCallbackProcessed = AtomicBoolean(false)

    var botUsername: String? = null
        private set
    var userChat: UserChat? = null
    var chatState: ChatState? = null
    var botActionExecutor: BotActionExecutor? = null
        private set
    var command: BotCommand? = null
        private set
    private var maxDelay: Duration = Duration.ZERO
    var shouldExecuteImmediately: Boolean = true
        private set
    var action: BotAction? = null
        private set
    var confirmation: Confirmation? = null
        private set

    var update: BotUpdate? = null
        private set
    private var extractedChatId: Long? = null
    var payloadData: PayloadData? = null
        private set

    private var actionResult: ActionResult? = null
    var status: String? = null
        private set
    var messagesToSend: List<SendMessageMarkup> = emptyList()
        private set
    var messageArgs: MutableMap<String, Any?> = mutableMapOf()
        private set
    var locationsToSend: List<SendLocationItem> = emptyList()
        private set
    private var reply: Reply? = null
    var transitionToState: String? = null
        private set
    private var immediateTransition: Boolean = false

    val actionId: String get() = command?.name ?: ""

    val requiresConfirmation: Boolean
        get() {
            val c = confirmation
            val st = chatState
            val cmd = command
            val act = action
            return c != null &&
                !c.message.isNullOrBlank() &&
                c.callbacks.isNotEmpty() &&
                st != null &&
                cmd != null &&
                act != null &&
                st.`is`(cmd.name, confirmationState)
        }

    fun botUsername(botUsername: String): ActionExecutionContext =
        apply {
            this.botUsername = botUsername
        }

    fun botActionExecutor(botActionExecutor: BotActionExecutor): ActionExecutionContext =
        apply {
            this.botActionExecutor = botActionExecutor
            command(botActionExecutor.botCommand)
            action = botActionExecutor.botAction
            confirmation = botActionExecutor.botAction.confirmation
        }

    fun command(command: BotCommand): ActionExecutionContext =
        apply {
            this.command = command
            this.maxDelay = Duration.ofSeconds(command.maxDelaySeconds)
            this.shouldExecuteImmediately = !maxDelay.isPositive
        }

    fun update(update: BotUpdate): ActionExecutionContext =
        apply {
            this.update = update
            this.payloadData = ChatUtils.createPayloadData(update)
            this.extractedChatId = ChatUtils.extractChatId(update)
        }

    fun setActionResult(actionResult: ActionResult) {
        this.actionResult = actionResult
        this.status = actionResult.status
        val act = action ?: return
        val msgs = mutableListOf<SendMessageMarkup>()
        actionResult.messagesToSend
            .filter { !it.text.isNullOrBlank() }
            .forEach { msgs.add(it) }
        act.replies
            .firstOrNull { r -> status == r.onStatus }
            ?.let { r ->
                setReply(r)
                r.messagesToSend
                    .filter { !it.isNullOrBlank() }
                    .map { SendMessageMarkup(it) }
                    .forEach { msgs.add(it) }
            }
        locationsToSend = actionResult.locationsToSend
        messageArgs.putAll(actionResult.messageArgs)
        messagesToSend = msgs.distinct()
    }

    private fun setReply(reply: Reply) {
        this.reply = reply
        this.transitionToState = reply.transitionToState
        this.immediateTransition = reply.immediateTransition
    }

    val hasTransitionToState: Boolean get() = !transitionToState.isNullOrBlank()

    val hasMessagesToSend: Boolean get() = messagesToSend.isNotEmpty()

    val shouldTransitionImmediately: Boolean get() = !hasMessagesToSend && hasTransitionToState && immediateTransition

    val shouldExecutePostAction: Boolean
        get() = command?.let { !it.keepMessage && !BotCommand.COMMAND_START.equals(it.name, ignoreCase = true) } ?: false

    val shouldDeleteChatStateBeforeExecution: Boolean
        get() = ((isInit && action?.offState == false) || requiresConfirmation)

    val shouldDeleteCallbackSource: Boolean
        get() {
            val upd = update ?: return false
            val act = action ?: return false
            val cmd = command ?: return false
            if (upd.callbackQuery == null) return false
            if (isCallbackSourceDeleted) return false
            if (cmd.keepMessage && !isInit) return false
            if (act.offState) return false
            return isInit
        }

    private val isInit: Boolean get() = action?.onState == BotAction.INIT_STATE && payloadData?.commandArg?.isEmpty() ?: false

    val isCallbackSourceDeleted: Boolean get() = afterCallbackProcessed.get()

    fun setCallbackSourceDeleted() {
        afterCallbackProcessed.set(true)
    }

    val chatId: Long get() = extractedChatId ?: ChatUtils.extractChatId(update)

    val confirmationOptions: MenuMarkup
        get() {
        val conf = confirmation ?: return MenuMarkup()
        val cmd = command ?: return MenuMarkup()
        val act = action ?: return MenuMarkup()
        val row =
            MenuRow(
                conf.callbacks
                    .map { cb: CallbackOption -> MenuButton(cb.text, getCallbackCode(cb, cmd, act)) }
                    .toMutableList(),
            )
        return MenuMarkup(rows = mutableListOf(row))
        }

    private fun getCallbackCode(
        callbackOption: CallbackOption,
        cmd: BotCommand,
        act: BotAction,
    ): String = ChatUtils.CommandCallback(cmd, act, callbackOption).toString()

    fun randomDelay(): Duration = Duration.ofSeconds((Math.random() * maxDelay.seconds).toLong())

    val confirmationState: String get() = (action?.onState ?: "") + BotAction.CONFIRM_SUFFIX
}
