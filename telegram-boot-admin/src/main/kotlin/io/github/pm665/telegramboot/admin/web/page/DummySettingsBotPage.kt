package io.github.pm665.telegramboot.admin.web.page

import org.springframework.stereotype.Component

@Component
class DummySettingsBotPage : BotAdminPage {
    override fun title(): String = "Settings"

    override fun order(): Int = 1

    override fun template(): String = "fragments/bot-pages/dummy-settings"
}
