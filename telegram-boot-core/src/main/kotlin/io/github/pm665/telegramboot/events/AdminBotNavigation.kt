package io.github.pm665.telegramboot.events

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class AdminBotNavigation {
    // ponytail: process-local history; persist only if back must survive restarts.
    private val history = ConcurrentHashMap<MessageKey, List<NavigationEntry>>()
    private val pending = ConcurrentHashMap<UserChatKey, PendingAction>()

    fun start(
        chatId: Long,
        messageId: Int,
        command: String = START_CALLBACK,
        response: SendAdminMessageEvent? = null,
    ) {
        history[MessageKey(chatId, messageId)] = listOf(NavigationEntry(command, response?.let(::cachedResponse)))
    }

    fun track(command: AdminBotCommand, action: AdminBotActionEvent) {
        if (command.replyInNewMessage || action.callbackData == null || action.sourceMessageId == null) return
        pending[UserChatKey(action.userId, action.chatId)] = PendingAction(command, action)
    }

    fun resolve(event: AdminBotActionEvent): ResolvedAction {
        if (event.callbackData != BACK_CALLBACK) return ResolvedAction(event)
        val key = event.key() ?: return ResolvedAction(event)
        val entries = history[key].orEmpty()
        if (entries.size < 2) return ResolvedAction(event)
        val previous = entries[entries.lastIndex - 1]
        history[key] = entries.dropLast(1)
        return ResolvedAction(
            action = event.copy(callbackData = previous.command, navigationBack = true),
            cachedResponse = previous.cachedResponse?.takeIf { it.expiresAt.isAfter(Instant.now()) }?.event,
        )
    }

    fun prepare(recipientUserId: Long, chatId: Long, response: SendAdminMessageEvent): SendAdminMessageEvent {
        val pendingAction = pending.remove(UserChatKey(recipientUserId, chatId))
        if (response.navigationTerminal) {
            val action = pendingAction?.action
            val key = response.editMessageId?.takeIf { response.editChatId == chatId }?.let { MessageKey(chatId, it) } ?: action?.key()
            key?.let(history::remove)
            if (pendingAction == null || pendingAction.command.replyInNewMessage || action?.callbackData == null) return response
            return response.copy(editChatId = action.chatId, editMessageId = action.sourceMessageId)
        }
        if (pendingAction == null) {
            response.editMessageId?.let { messageId ->
                if (response.editChatId == chatId) cache(MessageKey(chatId, messageId), response)
            }
            return response
        }
        val command = pendingAction.command
        val action = pendingAction.action
        if (command.replyInNewMessage || action.callbackData == null) return response
        val key = action.key() ?: return response
        val entries = history[key].orEmpty()
        if (response.navigationReplace && !action.navigationBack && entries.size >= 2) {
            history[key] = entries.dropLast(1) + NavigationEntry(action.callbackData)
        } else if (!action.navigationBack) {
            history[key] = entries + NavigationEntry(action.callbackData)
        }
        cache(key, response)
        return response.copy(editChatId = action.chatId, editMessageId = action.sourceMessageId)
    }

    fun withBackButton(
        chatId: Long,
        response: SendAdminMessageEvent,
    ): SendAdminMessageEvent {
        if (response.navigationTerminal) return response.copy(buttons = response.buttons.filterNot { it.callbackData == BACK_CALLBACK })
        val messageId = response.editMessageId ?: return response
        if (response.editChatId != chatId) return response
        if (history[MessageKey(chatId, messageId)].orEmpty().size < 2) return response
        if (response.buttons.any { it.callbackData == BACK_CALLBACK }) return response
        return response.copy(buttons = response.buttons + AdminMessageButton("Назад", BACK_CALLBACK))
    }

    private fun AdminBotActionEvent.key(): MessageKey? = sourceMessageId?.let { MessageKey(chatId, it) }

    private fun cache(key: MessageKey, response: SendAdminMessageEvent) {
        val entries = history[key].orEmpty()
        if (entries.isEmpty()) return
        history[key] =
            entries.dropLast(1) +
                entries.last().copy(
                    cachedResponse = cachedResponse(response),
                )
    }

    private fun cachedResponse(response: SendAdminMessageEvent) =
        CachedResponse(
            response.copy(
                recipientsUserIds = emptyList(),
                tag = null,
                deleteMessageId = null,
                editChatId = null,
                editMessageId = null,
                requestId = null,
            ),
            Instant.now().plus(CACHE_TTL),
        )

    private data class MessageKey(
        val chatId: Long,
        val messageId: Int,
    )

    private data class UserChatKey(val userId: Long, val chatId: Long)

    private data class PendingAction(val command: AdminBotCommand, val action: AdminBotActionEvent)

    data class ResolvedAction(
        val action: AdminBotActionEvent,
        val cachedResponse: SendAdminMessageEvent? = null,
    )

    private data class NavigationEntry(
        val command: String,
        val cachedResponse: CachedResponse? = null,
    )

    private data class CachedResponse(
        val event: SendAdminMessageEvent,
        val expiresAt: Instant,
    )

    companion object {
        const val BACK_CALLBACK = "admin:back"
        const val START_CALLBACK = "/start"
        private val CACHE_TTL: Duration = Duration.ofMinutes(5)
    }
}
