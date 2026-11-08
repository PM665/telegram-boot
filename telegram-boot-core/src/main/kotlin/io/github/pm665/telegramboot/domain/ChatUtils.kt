@file:Suppress("unused")

package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.annotations.BotAction
import io.github.pm665.telegramboot.domain.annotations.BotCommand
import io.github.pm665.telegramboot.domain.annotations.CallbackOption
import io.github.pm665.telegramboot.domain.bot.BotIncomingMessage
import io.github.pm665.telegramboot.domain.bot.BotLocation
import io.github.pm665.telegramboot.domain.bot.BotMessageEntity
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.UsersService.User
import org.springframework.util.StringUtils

object ChatUtils {
    private val markdownSpecialChars = Regex("([\\\\_*`\\[])")

    fun extractChatId(update: BotUpdate?): Long {
        if (update == null) return -1
        return when {
            Flag.CALLBACK_QUERY.test(update) -> update.callbackQuery?.message?.chat?.id ?: update.callbackQuery?.from?.id ?: -1
            Flag.MESSAGE.test(update) -> update.message?.chat?.id ?: -1
            else -> -1
        }
    }

    fun extractText(update: BotUpdate?): String {
        if (update == null) return ""
        return when {
            Flag.CALLBACK_QUERY.test(update) -> update.callbackQuery?.data.orEmpty()
            Flag.MESSAGE.test(update) -> update.message?.text.orEmpty()
            else -> ""
        }
    }

    fun extractFormattedMessage(update: BotUpdate?): String {
        if (update == null || !Flag.MESSAGE.test(update)) return ""
        val message = requireNotNull(update.message)
        val text = message.text ?: return ""
        val entities = message.entities
        if (entities.isEmpty()) {
            return text
        }

        val sortedEntities =
            entities
                .sortedWith(
                    compareBy<BotMessageEntity> { it.offset }
                        .thenByDescending { it.length },
                )

        val result = StringBuilder()
        var cursor = 0
        for (entity in sortedEntities) {
            val offset = entity.offset
            val length = entity.length
            if (length <= 0 || offset < 0 || offset >= text.length) continue

            val end = (offset + length).coerceAtMost(text.length)
            if (offset < cursor) continue

            if (offset > cursor) {
                result.append(escapeMarkdown(text.substring(cursor, offset)))
            }

            val entityText = text.substring(offset, end)
            result.append(applyEntityFormatting(entity, entityText))
            cursor = end
        }

        if (cursor < text.length) {
            result.append(escapeMarkdown(text.substring(cursor)))
        }

        return result.toString()
    }

    fun createPayloadData(update: BotUpdate): PayloadData {
        val message = extractText(update)
        val location = if (Flag.LOCATION.test(update)) update.message?.location else null
        val isCallback = Flag.CALLBACK_QUERY.test(update)
        return PayloadData(extractChatId(update), update, message, location, isCallback)
    }

    fun extractCommand(update: BotUpdate): String? =
        when {
            Flag.MESSAGE.test(update) -> extractCommandName(update.message?.text)
            Flag.CALLBACK_QUERY.test(update) -> extractCommandName(update.callbackQuery?.data)
            else -> null
        }

    private fun extractCommandName(text: String?): String? =
        text
            ?.split(" ")
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() && it.startsWith("/") }

    fun extractCallbackText(update: BotUpdate): String? = if (Flag.CALLBACK_QUERY.test(update)) extractText(update) else null

    fun extractCommandCallback(update: BotUpdate): CommandCallback? =
        extractCallbackText(update)
            ?.split("|")
            ?.takeIf { it.size == 3 }
            ?.let { CommandCallback(it[0], it[1], it[2]) }

    fun extractUser(update: BotUpdate?): User? =
        when {
            Flag.MESSAGE.test(update) -> update?.message?.from?.toUser()
            Flag.CALLBACK_QUERY.test(update) -> update?.callbackQuery?.from?.toUser()
            else -> null
        }

    private fun io.github.pm665.telegramboot.domain.bot.BotUserInfo.toUser() = User(id, username, language.orEmpty())

    data class PayloadData(
        val chatId: Long,
        val update: BotUpdate,
        val message: String,
        val location: BotLocation?,
        val isCallback: Boolean,
    ) {
        val commandArg: String get() = message.replaceFirst("^\\S+\\s*".toRegex(), "").trim()

        val commandArgsMap: Map<String, String>
            get() {
            val argLine = commandArg
            val tokens = StringUtils.tokenizeToStringArray(argLine, ",;|", true, true) ?: return emptyMap()
            return tokens
                .mapNotNull { entry ->
                    val parts = StringUtils.tokenizeToStringArray(entry, "=", true, false)
                    if (parts != null && parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
            }
    }

    enum class Flag(
        private val predicate: (BotUpdate) -> Boolean,
    ) {
        NONE({ true }),
        MESSAGE({ it.message != null }),
        CALLBACK_QUERY({ it.callbackQuery != null }),
        CHANNEL_POST({ it.channelPost != null }),
        EDITED_CHANNEL_POST({ it.editedChannelPost != null }),
        EDITED_MESSAGE({ it.editedMessage != null }),
        MY_CHAT_MEMBER({ it.myChatMember != null }),
        CHAT_MEMBER({ it.chatMember != null }),
        REPLY({ MESSAGE.test(it) && it.message?.isReply == true }),
        DOCUMENT({ MESSAGE.test(it) && it.message?.hasDocument == true }),
        TEXT({ MESSAGE.test(it) && !it.message?.text.isNullOrBlank() }),
        PHOTO({ MESSAGE.test(it) && it.message?.photos?.isNotEmpty() == true }),
        LOCATION({ MESSAGE.test(it) && it.message?.location != null }),
        CAPTION({ MESSAGE.test(it) && it.message?.caption != null }),
        ;

        fun test(update: BotUpdate?): Boolean = update != null && predicate(update)
    }

    data class CommandCallback(
        val command: String,
        val state: String,
        val callback: String,
    ) {
        constructor(command: BotCommand, botAction: BotAction, callbackOption: CallbackOption) : this(
            command.name,
            botAction.onState,
            callbackOption.code,
        )

        override fun toString(): String = "%s|%s|%s".format(command, state, callback)
    }

    private fun applyEntityFormatting(
        entity: BotMessageEntity,
        text: String,
    ): String =
        when (entity.type.lowercase()) {
            "bold" -> {
                "*${escapeMarkdown(text)}*"
            }

            "italic" -> {
                "_${escapeMarkdown(text)}_"
            }

            "code" -> {
                "`${text.replace("`", "\\`")}`"
            }

            "pre" -> {
                "```$text```"
            }

            "text_link" -> {
                val url = entity.url
                if (!url.isNullOrBlank()) {
                    "[${escapeMarkdown(text)}]($url)"
                } else {
                    text
                }
            }

            else -> {
                text
            }
        }

    private fun escapeMarkdown(text: String): String = text.replace(markdownSpecialChars, "\\\\$1")
}
