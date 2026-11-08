package io.github.pm665.telegramboot.admin.web.page

import org.springframework.stereotype.Component

@Component
class DummyMetricsServicePage : ServiceAdminPage {
    override fun title(): String = "Metrics overview"

    override fun order(): Int = 0

    override fun template(): String = "fragments/service-pages/dummy-metrics"
}
