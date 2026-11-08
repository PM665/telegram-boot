package io.github.pm665.telegramboot.admin.web.page

import org.springframework.stereotype.Component

@Component
class DummySupportServicePage : ServiceAdminPage {
    override fun title(): String = "Support center"

    override fun order(): Int = 1

    override fun template(): String = "fragments/service-pages/dummy-support"
}
