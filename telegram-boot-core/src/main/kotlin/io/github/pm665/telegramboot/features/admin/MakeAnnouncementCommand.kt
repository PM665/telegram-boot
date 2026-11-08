package io.github.pm665.telegramboot.features.admin

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuButton
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuRow
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.annotations.Reply
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.UsersService

@BotCommand(
    name = MakeAnnouncementCommand.ACTION_ID,
    accessType = AccessType.BOT_ADMIN,
    displayName = "broadcast.command",
    row = 1498L,
)
class MakeAnnouncementCommand(
    private val broadcastMessagesService: BroadcastMessagesService,
    private val messageSender: MessageSender,
    private val usersService: UsersService,
) {
    companion object {
        const val ACTION_ID = "broadcast"
    }

    @BotAction(
        replies = [
            Reply(messagesToSend = ["broadcast.sent"]),
            Reply(onStatus = ReplyStatus.wait, transitionToState = "broadcastMessage", messagesToSend = ["broadcast.enterMessage"]),
        ],
    )
    fun execute(context: BotActionContext): ActionResult {
        val commandArg = context.payloadData.commandArg
        if (commandArg.isBlank()) {
            return ActionResult.create(ReplyStatus.wait).build()
        }
        val botUsername = context.botUsername
        val builder = ActionResult.builder().actionId(ACTION_ID)
        val broadcastMessage = broadcastMessagesService.findById(commandArg)
        if (broadcastMessage == null) {
            return builder.status(ReplyStatus.error).build()
        }
        try {
            val chatIds = usersService.getChats(botUsername, AccessType.USER).map { it.chatId }.toSet()
            messageSender.sendMessages(
                MessageSender.SendMessageContext
                    .builder()
                    .actionId(ACTION_ID)
            .messagesToSend(listOf(SendMessageMarkup(broadcastMessage.message)))
                    .messageArgs(emptyMap<String, Any?>())
                    .chatId(chatIds)
                    .botUsername(botUsername)
                    .build(),
            )
            builder.status(ReplyStatus.ok)
            builder.messageArgs(mapOf("count" to chatIds.size.toString()))
            return builder.build()
        } finally {
            broadcastMessagesService.remove(commandArg)
        }
    }

    @BotAction(
        onState = "broadcastMessage",
        replies = [
            Reply(transitionToState = "waitingForConfirmation"),
            Reply(onStatus = ReplyStatus.error, messagesToSend = ["broadcast.error"]),
        ],
    )
    fun receiveBroadcastMessage(context: BotActionContext): ActionResult {
        val message = ChatUtils.extractFormattedMessage(context.payloadData.update).ifBlank { context.payloadData.message }
        val userChat = context.user?.chat ?: return ActionResult.create(ReplyStatus.error).build()
        val msg = BroadcastMessage(userChat.chatId, context.botUsername, message)
        val broadcastMessageId = broadcastMessagesService.save(msg)
        val replyToMessageId =
            context
                .payloadData
                .update
                .message?.messageId
        return ActionResult
            .create(ReplyStatus.ok)
            .messagesToSend(
                listOf(
                    SendMessageMarkup(
                        text = "broadcast.confirm",
                        replyToMessageId = replyToMessageId,
                        menuMarkup =
                            MenuMarkup(
                                rows =
                                    mutableListOf(
                                        MenuRow(
                                            mutableListOf(
                                                MenuButton("confirm.reply_yes", "/%s %s".format(ACTION_ID, broadcastMessageId)),
                                                MenuButton("confirm.reply_no", SendMessageMarkup.CALLBACK_NO),
                                            ),
                                        ),
                                    ),
                            ),
                    ),
                ),
            ).build()
    }

    @BotAction(
        onState = "waitingForConfirmation",
        onCallback = SendMessageMarkup.CALLBACK_NO,
        replies = [Reply(messagesToSend = ["broadcast.cancel"])],
    )
    fun confirmed(ignored: BotActionContext): ActionResult = ActionResult.create(ReplyStatus.ok).build()
}
