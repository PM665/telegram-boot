package io.github.pm665.telegramboot.admin.web

import io.github.pm665.telegramboot.admin.web.page.ServiceAdminPage
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("/admin/services")
class ServicePageController(
    serviceAdminPages: List<ServiceAdminPage>,
) {
    private val pagesBySlug =
        serviceAdminPages
            .sortedBy { it.order() }
            .associateBy { it.slug() }

    @GetMapping("/{slug}")
    fun servicePage(
        @PathVariable slug: String,
        model: Model,
    ): String {
        val page =
            pagesBySlug[slug]
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Service page $slug not found")

        model.addAttribute("page", page)
        return "fragments/service-content :: serviceContent"
    }
}
