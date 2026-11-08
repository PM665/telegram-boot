package io.github.pm665.telegramboot.features.admin

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.Visibility
import io.github.pm665.telegramboot.domain.actions.ActionResult
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.annotations.Reply
import io.github.pm665.telegramboot.domain.bot.BotPlatform
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import io.github.pm665.telegramboot.domain.properties.ReplyStatus
import io.github.pm665.telegramboot.events.AdminMessageButton
import io.github.pm665.telegramboot.events.SendAdminMessageEvent
import io.github.pm665.telegramboot.ports.BotAdminAutoApproval
import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.MessageSender
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

@BotCommand(name = IAmBotAdminCommand.ACTION_ID, visibility = Visibility.HIDDEN, accessType = AccessType.ALL)
class IAmBotAdminCommand(
    private val permissionsService: PermissionsService,
    private val usersService: UsersService,
    private val messageSender: MessageSender,
    private val autoApproval: BotAdminAutoApproval,
    private val botsService: BotsService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    companion object {
        const val ACTION_ID = "iAmAdmin"
        const val ADMIN_APPROVE_COMMAND = "/ab"
        private val log = LoggerFactory.getLogger(IAmBotAdminCommand::class.java)
    }

    @BotAction(
        replies = [
            Reply(onStatus = ReplyStatus.wait, messagesToSend = ["botAdmin.needApproval"]),
            Reply(onStatus = ReplyStatus.invalidInput, messagesToSend = ["botAdmin.invalidCode"]),
            Reply(onStatus = ReplyStatus.error, messagesToSend = ["botAdmin.noApprovers"]),
        ],
    )
    fun execute(context: BotActionContext): ActionResult {
        val args = context.payloadData.commandArg.split(" ")
        if (args.size == 2) {
            return performApprove(
                botUsername = context.botUsername,
                approvers = findBotAdminChatIds(context.botUsername),
                approvalCode = args[0].trim(),
                candidateUserId = args[1].toLong(),
            )
        } else {
            val candidate = context.user?.user ?: return ActionResult.create(ReplyStatus.error).build()
            return generateApproveCode(user = candidate, context = context)
        }
    }

    private fun generateApproveCode(
        user: UsersService.User,
        context: BotActionContext,
    ): ActionResult {
        if (permissionsService.isUserBotAdmin(user, context.botUsername)) {
            messageSender.sendMessage(
                SendMessageMarkup("botAdmin.alreadyAdmin"),
                user.userId,
                context.botUsername,
                mapOf("username" to (user.username ?: user.userId.toString())),
            )
            return ActionResult.create(ReplyStatus.ok).build()
        }

        if (permissionsService.isUserServiceAdmin(user) || autoApproval.isParkContact(context.botUsername, user.username)) {
            permissionsService.setUserAccess(user, context.botUsername, AccessType.BOT_ADMIN)
            messageSender.sendMessage(
                SendMessageMarkup("botAdmin.userIsAdmin"),
                context.user?.chat?.chatId ?: user.userId,
                context.botUsername,
                mapOf("username" to (user.username ?: user.userId.toString())),
            )
            return ActionResult.create(ReplyStatus.ok).build()
        }

        val approverChatIds = findBotAdminChatIds(context.botUsername)
        val serviceAdminIds = permissionsService.findServiceAdminUserIds()
        if (approverChatIds.isEmpty() && serviceAdminIds.isEmpty()) {
            log.error("No approvers found for user {}", user)
            return ActionResult.create(ReplyStatus.error).build()
        }

        val newApproveCode = UUID.randomUUID().toString().replace("-", "")

        permissionsService.saveApproveCodeForUser(user, context.botUsername, AccessType.BOT_ADMIN, newApproveCode)

        if (approverChatIds.isEmpty()) {
            eventPublisher.publishEvent(
                SendAdminMessageEvent(
                    message = "Bot administrator role request",
                    recipientsUserIds = emptyList(),
                    tag = null,
                    buttons =
                        listOf(
                            AdminMessageButton(
                                text = "Approve",
                                callbackData = "$ADMIN_APPROVE_COMMAND ${user.botUserId} $newApproveCode",
                            ),
                        ),
                    richHtml = adminApprovalHtml(context.botUsername, user, context),
                ),
            )
            return ActionResult.create(ReplyStatus.wait).build()
        }

        val messageArgs = fillMessageArgs(context)

        val menu =
            SendMessageMarkup.MenuMarkup().addButtonToLastRow(
                text = "botAdmin.approve",
                callbackData = "/$ACTION_ID $newApproveCode ${user.userId}",
            )
        val msg =
            SendMessageMarkup(
                text = "botAdmin.approvalCode",
                menuMarkup = menu,
            )
        val sendMessageContext =
            MessageSender.SendMessageContext
                .builder()
                .actionId(ACTION_ID)
                .messageToSend(msg)
                .messageArgs(messageArgs)
                .chatId(approverChatIds)
                .botUsername(context.botUsername)
                .build()
        messageSender.sendMessages(sendMessageContext)

        return ActionResult.create(ReplyStatus.wait).build()
    }

    private fun findBotAdminChatIds(botUsername: String): Set<Long> =
        permissionsService.findBotAdmins(botUsername).map { it.chatId }.toSet()

    private fun adminApprovalHtml(
        botUsername: String,
        user: UsersService.User,
        context: BotActionContext,
    ): String {
        val platform = botsService.findBotByBotUsername(botUsername)?.platform
        val info = getBotUser(context)
        val displayName =
            listOfNotNull(info?.firstName, info?.lastName)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .joinToString(" ")
                .ifEmpty { user.username?.takeIf(String::isNotBlank)?.let { "@$it" } ?: user.userId.toString() }
                .htmlEscape()
        val userUrl =
            when (platform) {
                BotPlatform.MAX -> "https://max.ru/u/${user.userId}"
                BotPlatform.TELEGRAM -> user.username?.takeIf(String::isNotBlank)?.let { "https://t.me/${it.removePrefix("@")}" }
                else -> null
            }
        val botUrl =
            when (platform) {
                BotPlatform.MAX -> "https://max.ru/${botUsername.removePrefix("@")}"
                BotPlatform.TELEGRAM -> "https://t.me/${botUsername.removePrefix("@")}"
                else -> null
            }
        val linkedUser = userUrl?.let { "<a href=\"${it.htmlEscape()}\">$displayName</a>" } ?: displayName
        val botLabel = "@${botUsername.removePrefix("@").htmlEscape()}"
        val linkedBot = botUrl?.let { "<a href=\"${it.htmlEscape()}\">$botLabel</a>" } ?: botLabel
        return "<h1>Bot administrator role request</h1><p>User $linkedUser requested access to bot $linkedBot.</p>"
    }

    private fun performApprove(
        botUsername: String,
        approvers: Set<Long>,
        approvalCode: String,
        candidateUserId: Long,
    ): ActionResult {
        val candidate =
            usersService.getUser(botUsername, candidateUserId)
                ?: throw IllegalStateException("User $candidateUserId not found")
        if (permissionsService.validateApproveCodeForUser(
                candidate,
                botUsername,
                AccessType.BOT_ADMIN,
                approvalCode,
            )
        ) {
            permissionsService.setUserAccess(candidate, botUsername, AccessType.BOT_ADMIN)
            if (!approvers.isEmpty()) {
                val recipients = approvers.toMutableSet()
                usersService.getChat(botUsername, candidateUserId)?.let { recipients.add(it.chatId) }
                val sendMessageContext =
                    MessageSender.SendMessageContext
                        .builder()
                        .actionId(ACTION_ID)
                        .messageToSend(SendMessageMarkup("botAdmin.userIsAdmin"))
                        .messageArg("username", candidate.username ?: "unknown")
                        .chatId(recipients)
                        .botUsername(botUsername)
                        .build()
                messageSender.sendMessages(sendMessageContext)
            }
            return ActionResult.create(ReplyStatus.ok).build()
        } else {
            return ActionResult.create(ReplyStatus.invalidInput).build()
        }
    }

    private fun fillMessageArgs(context: BotActionContext): Map<String, String> {
        val user = context.user?.user
        val tgu = getBotUser(context)
        val firstName = tgu?.firstName ?: "-"
        val lastName = tgu?.lastName ?: "-"
        val username = user?.username ?: "-"
        return mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username,
        )
    }

    private fun getBotUser(context: BotActionContext): BotUserInfo? =
        with(context.payloadData.update) {
            message?.from ?: callbackQuery?.from
        }

    private fun String.htmlEscape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
