package sample

import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.ports.UsersService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HelloCommandTest {
    @Test
    fun `greets the sender and handles a missing username`() {
        fun greeting(username: String?): String {
            val user = UsersService.User(1, username, "en")
            val chat = UsersService.Chat(1, "bot", 1, true)
            val context = BotActionContext("bot", ChatUtils.createPayloadData(BotUpdate()), UsersService.UserChat(user, chat))
            return HelloCommand().hello(context).messagesToSend.single().text
        }

        assertEquals("Hello @test\\_user", greeting("test_user"))
        assertEquals("Hello there", greeting(null))
    }
}
