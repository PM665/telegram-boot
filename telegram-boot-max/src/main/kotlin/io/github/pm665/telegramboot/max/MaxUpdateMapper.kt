package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pm665.telegramboot.domain.bot.BotCallbackQuery
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotLocation
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo

internal class MaxUpdateMapper(
    private val objectMapper: ObjectMapper,
) {
    fun from(rawUpdate: String): BotUpdate {
        val update = objectMapper.readTree(rawUpdate)
        val updateId = update.path("timestamp").takeUnless(JsonNode::isMissingNode)?.asText()
        return when (update.path("update_type").asText()) {
            "message_created", "message_edited" -> BotUpdate(updateId = updateId, message = message(update.path("message")))
            "message_callback" -> callbackUpdate(update, updateId)
            else -> BotUpdate(updateId = updateId)
        }
    }

    private fun callbackUpdate(
        update: JsonNode,
        updateId: String?,
    ): BotUpdate {
        val callback = update.path("callback")
        val user = user(callback.path("user"))
        val message = message(update.path("message"), user?.id)
        val callbackUser = user ?: message?.from
        val callbackId = callback.path("callback_id").takeUnless(JsonNode::isMissingNode)?.asText() ?: return BotUpdate(updateId)
        return BotUpdate(
            updateId = updateId,
            callbackQuery =
                BotCallbackQuery(
                    id = callbackId,
                    data = callback.path("payload").takeUnless(JsonNode::isMissingNode)?.asText(),
                    from = callbackUser,
                    message = message,
                ),
        )
    }

    private fun message(
        message: JsonNode,
        callbackUserId: Long? = null,
    ): BotIncomingMessage? {
        if (message.isMissingNode || message.isNull) return null
        val recipient = message.path("recipient")
        val sender = message.path("sender")
        val recipientType = recipient.path("chat_type").asText(recipient.path("type").asText())
        val chatType = when (recipientType) {
            "dialog" -> BotChatType.PRIVATE
            "chat" -> BotChatType.GROUP
            "channel" -> BotChatType.CHANNEL
            else -> if (recipient.path("chat_id").isMissingNode) BotChatType.PRIVATE else BotChatType.UNKNOWN
        }
        val chatId =
            if (chatType == BotChatType.PRIVATE) {
                callbackUserId ?: sender.path("user_id").takeUnless(JsonNode::isMissingNode)?.asLong()
            } else {
                recipient.path("chat_id").takeUnless(JsonNode::isMissingNode)?.asLong()
            } ?: return null
        val body = message.path("body")
        val location =
            body.path("attachments")
                .firstOrNull { it.path("type").asText() == "location" }
                ?.path("payload")
                ?.let { payload ->
                    val latitude = payload.path("lat").takeUnless(JsonNode::isMissingNode)?.asDouble()
                    val longitude = payload.path("lon").takeUnless(JsonNode::isMissingNode)?.asDouble()
                    if (latitude != null && longitude != null) BotLocation(latitude, longitude) else null
                }
        return BotIncomingMessage(
            messageId = body.path("mid").takeUnless(JsonNode::isMissingNode)?.asText(),
            chat = BotChatInfo(chatId, chatType, recipient.path("title").textOrNull()),
            from = user(sender),
            text = body.path("text").textOrNull(),
            location = location,
        )
    }

    private fun user(user: JsonNode): BotUserInfo? {
        if (user.isMissingNode || user.isNull) return null
        val userId = user.path("user_id").takeUnless(JsonNode::isMissingNode)?.asLong() ?: return null
        return BotUserInfo(
            id = userId,
            username = user.path("username").textOrNull(),
            firstName = user.path("first_name").textOrNull(),
            lastName = user.path("last_name").textOrNull(),
        )
    }

    private fun JsonNode.textOrNull(): String? = takeUnless(JsonNode::isMissingNode)?.takeUnless(JsonNode::isNull)?.asText()
}
