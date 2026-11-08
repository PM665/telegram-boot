package io.github.pm665.telegramboot.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AdminBotNavigationTest {
    private val navigation = AdminBotNavigation()
    private val command = TestCommand()

    @Test
    fun `callback response edits its source and back returns to the start menu`() {
        val list = action("/registrations", 77)
        navigation.start(420, 77)
        navigation.track(command, list)
        val listReply =
            navigation.withBackButton(
                420,
                navigation.prepare(42, 420, SendAdminMessageEvent("List", listOf(42), null)),
            )

        assertThat(listReply.editMessageId).isEqualTo(77)
        assertThat(listReply.buttons).containsExactly(AdminMessageButton("Back", AdminBotNavigation.BACK_CALLBACK))

        val details = action("/registration 7", 77)
        navigation.track(command, details)
        val detailsReply =
            navigation.withBackButton(
                420,
                navigation.prepare(42, 420, SendAdminMessageEvent("Details", listOf(42), null)),
            )

        assertThat(detailsReply.buttons).containsExactly(AdminMessageButton("Back", AdminBotNavigation.BACK_CALLBACK))

        val back = navigation.resolve(action(AdminBotNavigation.BACK_CALLBACK, 77))

        assertThat(back.action.callbackData).isEqualTo("/registrations")
        assertThat(back.cachedResponse?.message).isEqualTo("List")
    }

    @Test
    fun `command opting out of in-place replies leaves callback response unchanged`() {
        navigation.track(NewMessageCommand(), action("/health", 77))
        val reply = navigation.withBackButton(420, navigation.prepare(42, 420, SendAdminMessageEvent("Health", listOf(42), null)))

        assertThat(reply.editMessageId).isNull()
        assertThat(reply.buttons).isEmpty()
    }

    @Test
    fun `terminal callback response edits source without back and clears history`() {
        navigation.start(420, 77)
        navigation.track(command, action("/confirmPaymentDate 01.02.2027", 77))

        val reply =
            navigation.withBackButton(
                420,
                navigation.prepare(42, 420, SendAdminMessageEvent("Paid", listOf(42), null, navigationTerminal = true)),
            )

        assertThat(reply.editMessageId).isEqualTo(77)
        assertThat(reply.buttons).isEmpty()
        assertThat(navigation.resolve(action(AdminBotNavigation.BACK_CALLBACK, 77)).action.callbackData).isEqualTo(AdminBotNavigation.BACK_CALLBACK)
    }

    @Test
    fun `navigation adds back only as a button model`() {
        val list = action("/registrations", 77)
        navigation.start(420, 77)
        navigation.track(command, list)

        val prepared = navigation.prepare(42, 420, SendAdminMessageEvent("List", listOf(42), null, richHtml = "<p>List</p>"))
        val reply = navigation.withBackButton(420, prepared)

        assertThat(prepared.buttons).isEmpty()
        assertThat(reply.buttons).containsExactly(AdminMessageButton("Back", AdminBotNavigation.BACK_CALLBACK))
        assertThat(reply.richHtml).doesNotContain(AdminBotNavigation.BACK_CALLBACK)
    }

    @Test
    fun `progress edit preserves back for its tracked message without growing history`() {
        navigation.start(420, 77)
        navigation.track(command, action("/health", 77))
        navigation.prepare(42, 420, SendAdminMessageEvent("Checking health", listOf(42), null))

        val progress =
            navigation.withBackButton(
                420,
                navigation.prepare(
                    42,
                    420,
                    SendAdminMessageEvent(
                        message = "Checking alpha bot",
                        recipientsUserIds = listOf(42),
                        tag = null,
                        editChatId = 420,
                        editMessageId = 77,
                    ),
                ),
            )

        assertThat(progress.buttons).containsExactly(AdminMessageButton("Back", AdminBotNavigation.BACK_CALLBACK))
        assertThat(navigation.resolve(action(AdminBotNavigation.BACK_CALLBACK, 77)).action.callbackData).isEqualTo("/start")
    }

    @Test
    fun `progress edit does not add back to an unrelated message`() {
        navigation.start(420, 77)
        navigation.track(command, action("/health", 77))
        navigation.prepare(42, 420, SendAdminMessageEvent("Checking health", listOf(42), null))

        val progress =
            navigation.withBackButton(
                420,
                navigation.prepare(
                    42,
                    420,
                    SendAdminMessageEvent(
                        message = "Checking alpha bot",
                        recipientsUserIds = listOf(42),
                        tag = null,
                        editChatId = 420,
                        editMessageId = 78,
                    ),
                ),
            )

        assertThat(progress.buttons).isEmpty()
    }

    @Test
    fun `replacement callback response keeps back at the parent menu`() {
        navigation.start(420, 77)
        navigation.track(command, action("/settings", 77))
        navigation.prepare(42, 420, SendAdminMessageEvent("Settings", listOf(42), null))
        navigation.track(command, action("/settings/checks", 77))
        navigation.prepare(42, 420, SendAdminMessageEvent("Checks", listOf(42), null))

        navigation.track(command, action("/settings/checks toggle-a", 77))
        navigation.prepare(42, 420, SendAdminMessageEvent("Checks A", listOf(42), null, navigationReplace = true))
        navigation.track(command, action("/settings/checks toggle-b", 77))
        navigation.prepare(42, 420, SendAdminMessageEvent("Checks B", listOf(42), null, navigationReplace = true))

        val back = navigation.resolve(action(AdminBotNavigation.BACK_CALLBACK, 77))

        assertThat(back.action.callbackData).isEqualTo("/settings")
        assertThat(back.cachedResponse?.message).isEqualTo("Settings")
    }

    private fun action(
        callback: String,
        messageId: Int,
    ) = AdminBotActionEvent(42, 420, null, callback, messageId)

    private class TestCommand : AdminBotCommand(listOf("/registrations", "/registration"), "Registrations")

    private class NewMessageCommand : AdminBotCommand(listOf("/health"), "Health", replyInNewMessage = true)
}
