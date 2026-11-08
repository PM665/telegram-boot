package io.github.pm665.telegramboot.drivers.telegram

import io.github.pm665.telegramboot.domain.bot.BotCallbackQuery
import io.github.pm665.telegramboot.domain.bot.BotChatInfo
import io.github.pm665.telegramboot.domain.bot.BotChatMember
import io.github.pm665.telegramboot.domain.bot.BotChatMemberStatus
import io.github.pm665.telegramboot.domain.bot.BotChatMemberUpdate
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotFileReference
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotLocation
import io.github.pm665.telegramboot.domain.bot.BotMessageEntity
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.domain.bot.BotUserInfo
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberRestricted
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.time.Instant

class TelegramUpdateMapper {
    fun from(update: Update): BotUpdate =
        BotUpdate(
            updateId = update.updateId?.toString(),
            message = update.message?.toBotMessage(),
            editedMessage = update.editedMessage?.toBotMessage(),
            channelPost = update.channelPost?.toBotMessage(),
            editedChannelPost = update.editedChannelPost?.toBotMessage(),
            callbackQuery =
                update.callbackQuery?.let { callback ->
                    BotCallbackQuery(
                        id = callback.id,
                        data = callback.data,
                        from = callback.from?.toBotUser(),
                        message = callback.message?.toBotIncomingMessage(),
                    )
                },
            myChatMember = update.myChatMember?.let { member -> member.toBotMemberUpdate() },
            chatMember = update.chatMember?.let { member -> member.toBotMemberUpdate() },
        )

    private fun Message.toBotMessage() =
        BotIncomingMessage(
            messageId = messageId?.toString(),
            chat = chat?.toBotChat(),
            from = from?.toBotUser(),
            text = text,
            caption = caption,
            entities = entities.orEmpty().map { BotMessageEntity(it.type, it.offset, it.length, it.url) },
            location = location?.let { BotLocation(it.latitude, it.longitude) },
            photos = photo.orEmpty().map { BotFileReference(it.fileId, it.fileUniqueId) },
            hasDocument = document != null,
            isReply = replyToMessage != null,
        )

    private fun MaybeInaccessibleMessage.toBotIncomingMessage() =
        BotIncomingMessage(
            messageId = messageId?.toString(),
            chat = chat?.toBotChat(),
        )

    private fun Chat.toBotChat() =
        BotChatInfo(
            id = id,
            type =
                when (type) {
                    "private" -> BotChatType.PRIVATE
                    "group" -> BotChatType.GROUP
                    "supergroup" -> BotChatType.SUPERGROUP
                    "channel" -> BotChatType.CHANNEL
                    else -> BotChatType.UNKNOWN
                },
            title = title,
            username = userName,
        )

    private fun org.telegram.telegrambots.meta.api.objects.User.toBotUser() =
        BotUserInfo(
            id = id,
            username = userName,
            language = languageCode,
            firstName = firstName,
            lastName = lastName,
        )

    private fun org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated.toBotMemberUpdate() =
        BotChatMemberUpdate(
            chat = chat?.toBotChat(),
            date = date?.toLong()?.let(Instant::ofEpochSecond),
            newMember = newChatMember?.toBotMember(),
        )

    private fun ChatMember.toBotMember() =
        BotChatMember(
            status =
                when (this) {
                    is ChatMemberOwner -> BotChatMemberStatus.CREATOR
                    is ChatMemberAdministrator -> BotChatMemberStatus.ADMINISTRATOR
                    is ChatMemberMember -> BotChatMemberStatus.MEMBER
                    is ChatMemberRestricted -> BotChatMemberStatus.RESTRICTED
                    is ChatMemberLeft -> BotChatMemberStatus.LEFT
                    is ChatMemberBanned -> BotChatMemberStatus.BANNED
                    else -> BotChatMemberStatus.UNKNOWN
                },
            canPostMessages = (this as? ChatMemberAdministrator)?.canPostMessages,
        )
}
