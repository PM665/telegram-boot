package io.github.pm665.telegramboot.admin.web

import io.github.pm665.telegramboot.admin.service.BotOverviewService
import io.github.pm665.telegramboot.admin.web.page.BotAdminPage
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/bots")
class BotDetailsController(
    private val botOverviewService: BotOverviewService,
    botAdminPages: List<BotAdminPage>,
) {
    private val sortedBotPages = botAdminPages.sortedBy { it.order() }

    @GetMapping("/{username}", produces = [MediaType.TEXT_HTML_VALUE])
    fun botDetails(
        @PathVariable username: String,
        model: Model,
    ): String {
        model.addAttribute("selectedBot", botOverviewService.getBot(username))
        model.addAttribute("botPages", sortedBotPages)
        return "fragments/bot-content :: botContent"
    }
}
