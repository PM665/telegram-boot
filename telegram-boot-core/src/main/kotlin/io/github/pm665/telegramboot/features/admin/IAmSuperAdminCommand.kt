package io.github.pm665.telegramboot.features.admin

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils.Flag
import io.github.pm665.telegramboot.domain.Visibility
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.annotations.Reply
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PermissionsService
import org.slf4j.LoggerFactory
import java.util.UUID

@BotCommand(name = IAmSuperAdminCommand.ACTION_ID, visibility = Visibility.HIDDEN, accessType = AccessType.ALL)
class IAmSuperAdminCommand(
    private val permissionsService: PermissionsService,
    private val messageSender: MessageSender,
) {
    companion object {
        const val ACTION_ID = "iAmSuperAdmin"
        private val log = LoggerFactory.getLogger(IAmSuperAdminCommand::class.java)
    }

    @BotAction(
        replies = [
            Reply(messagesToSend = ["superAdmin.userIsAdmin"]),
            Reply(onStatus = ReplyStatus.wait, messagesToSend = ["superAdmin.needApproval"]),
            Reply(onStatus = ReplyStatus.invalidInput, messagesToSend = ["superAdmin.invalidCode"]),
            Reply(onStatus = ReplyStatus.error, messagesToSend = ["superAdmin.noApprovers"]),
        ],
    )
    fun execute(context: BotActionContext): ActionResult {
        val user = context.user?.user ?: return ActionResult.create(ReplyStatus.error).build()

        val approvers = permissionsService.findCreators(context.botUsername)
        val chatIds = approvers.map { it.chatId }.toSet()

        var receivedCode = context.payloadData.commandArg
        if (receivedCode.isNotBlank()) {
            receivedCode = receivedCode.trim()
            if (permissionsService.validateApproveCodeForUser(user, AccessType.SERVICE_ADMIN, receivedCode)) {
                permissionsService.setUserAccess(user, AccessType.SERVICE_ADMIN)
                if (!chatIds.isEmpty()) {
                    val sendMessageContext =
                        MessageSender.SendMessageContext
                            .builder()
                            .actionId(ACTION_ID)
                            .messageToSend(SendMessageMarkup("superAdmin.notifySuccess"))
                            .messageArg("username", user.username ?: "unknown")
                            .chatId(chatIds)
                            .botUsername(context.botUsername)
                            .build()
                    messageSender.sendMessages(sendMessageContext)
                }
                return ActionResult.create(ReplyStatus.ok).build()
            } else {
                return ActionResult.create(ReplyStatus.invalidInput).build()
            }
        }

        if (permissionsService.isUserServiceAdmin(user)) {
            return ActionResult.create(ReplyStatus.ok).build()
        }

        if (chatIds.isEmpty()) {
            log.error("No approvers found for user {}", user)
            return ActionResult.create(ReplyStatus.error).build()
        }

        val newApproveCode = UUID.randomUUID().toString()

        permissionsService.saveApproveCodeForUser(user, AccessType.SERVICE_ADMIN, newApproveCode)

        val messageArgs = fillMessageArgs(context, newApproveCode)

        val sendMessageContext =
            MessageSender.SendMessageContext
                .builder()
                .actionId(ACTION_ID)
                .messageToSend(SendMessageMarkup("superAdmin.approvalCode"))
                .messageArgs(messageArgs)
                .chatId(chatIds)
                .botUsername(context.botUsername)
                .build()
        messageSender.sendMessages(sendMessageContext)

        return ActionResult.create(ReplyStatus.wait).build()
    }

    private fun fillMessageArgs(
        context: BotActionContext,
        newApproveCode: String,
    ): Map<String, String> {
        val user = context.user?.user
        val tgu = getBotUser(context)
        val firstName = tgu?.firstName ?: "Unknown"
        val lastName = tgu?.lastName ?: "Unknown"
        val username = user?.username ?: "Unknown"
        val userId = user?.userId ?: 0L
        return mapOf(
            "code" to newApproveCode,
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username,
            "userId" to userId.toString(),
        )
    }

    private fun getBotUser(context: BotActionContext): BotUserInfo? =
        if (Flag.MESSAGE.test(context.payloadData.update)) {
            context.payloadData.update.message?.from
        } else {
            null
        }
}
