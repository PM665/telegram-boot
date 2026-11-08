package io.github.pm665.telegramboot.admin.web

import io.github.pm665.telegramboot.admin.web.page.NewBotPage
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/bots/new")
class NewBotPageController(
    private val newBotPage: NewBotPage,
) {
    @GetMapping(produces = [MediaType.TEXT_HTML_VALUE])
    fun newBotPage(): String = "${newBotPage.template()} :: newBotPage"
}
