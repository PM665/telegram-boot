package io.github.pm665.telegramboot.admin.web

import io.github.pm665.telegramboot.admin.service.BotOverviewService
import io.github.pm665.telegramboot.admin.web.page.BotAdminPage
import io.github.pm665.telegramboot.admin.web.page.NewBotPage
import io.github.pm665.telegramboot.admin.web.page.ServiceAdminPage
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin")
class AdminPageController(
    private val botOverviewService: BotOverviewService,
    botAdminPages: List<BotAdminPage>,
    private val newBotPage: NewBotPage,
    serviceAdminPages: List<ServiceAdminPage>,
) {
    private val sortedBotPages = botAdminPages.sortedBy { it.order() }
    private val servicePageLinks =
        serviceAdminPages
            .sortedBy { it.order() }
            .map { ServicePageLink(slug = it.slug(), title = it.title()) }

    @GetMapping
    fun adminPage(model: Model): String {
        val bots = botOverviewService.listBots()
        model.addAttribute("bots", bots)
        model.addAttribute("botPages", sortedBotPages)
        model.addAttribute("servicePages", servicePageLinks)
        model.addAttribute("selectedBot", bots.firstOrNull())
        model.addAttribute("newBotPage", newBotPage)
        return "admin"
    }

    data class ServicePageLink(
        val slug: String,
        val title: String,
    )
}
