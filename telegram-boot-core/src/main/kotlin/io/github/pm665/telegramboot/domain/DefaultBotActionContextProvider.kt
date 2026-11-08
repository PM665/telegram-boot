package io.github.pm665.telegramboot.domain

import io.github.pm665.telegramboot.domain.ChatUtils.PayloadData
import io.github.pm665.telegramboot.ports.BotActionContextProvider
import io.github.pm665.telegramboot.ports.UsersService
import org.slf4j.LoggerFactory

class DefaultBotActionContextProvider(
    private val usersService: UsersService,
) : BotActionContextProvider<BotActionContext> {
    private val log = LoggerFactory.getLogger(DefaultBotActionContextProvider::class.java)

    override fun resolve(
        botUsername: String,
        payloadData: PayloadData,
    ): BotActionContext {
        val tgUser = ChatUtils.extractUser(payloadData.update)
        val chatId = ChatUtils.extractChatId(payloadData.update)
        val user =
            tgUser?.let { tg ->
                usersService.getOrCreateUserChat(tg.userId, tg.username, tg.locale, botUsername, chatId)
            } ?: run {
                log.warn("User not found in payload: {}", payloadData)
                null
            }
        return BotActionContext(botUsername, payloadData, user)
    }
}
