package io.github.pm665.telegramboot.admin.web.page

import org.springframework.stereotype.Component

@Component
class DummyStatusBotPage : BotAdminPage {
    override fun title(): String = "Status"

    override fun order(): Int = 0

    override fun template(): String = "fragments/bot-pages/dummy-status"
}
