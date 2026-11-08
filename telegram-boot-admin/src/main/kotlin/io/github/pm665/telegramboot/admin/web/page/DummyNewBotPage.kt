package io.github.pm665.telegramboot.admin.web.page

import org.springframework.stereotype.Component

@Component
class DummyNewBotPage : NewBotPage {
    override fun template(): String = "fragments/new-bot/dummy-new-bot"
}
