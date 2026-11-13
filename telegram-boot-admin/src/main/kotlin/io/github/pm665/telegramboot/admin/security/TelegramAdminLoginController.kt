package io.github.pm665.telegramboot.admin.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Controller
class TelegramAdminLoginController(
    private val telegramLoginVerifier: TelegramLoginVerifier,
    private val properties: TelegramAdminProperties,
    private val usersService: UsersService,
    private val permissionsService: PermissionsService,
) {
    @GetMapping("/login")
    fun loginPage(model: Model): String {
        val botUsername = properties.telegramLoginBotUsername.trim()
        model.addAttribute("telegramLoginBotUsername", botUsername)
        model.addAttribute("telegramLoginWidgetEnabled", botUsername.isNotEmpty())
        model.addAttribute("telegramMiniAppAllowedOrigin", properties.miniAppUrl)
        return "login"
    }

    @RequestMapping("/login/telegram", method = [RequestMethod.GET, RequestMethod.POST])
    fun telegramLogin(
        @RequestParam params: Map<String, String>,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val missingFields = listOf("id", "auth_date", "hash").filterNot { params.containsKey(it) }
        if (missingFields.isNotEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Telegram auth fields: ${missingFields.joinToString(", ")}")
        }

        val id =
            params["id"]?.toLongOrNull()
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Telegram auth field: id")
        val authDate =
            params["auth_date"]?.toLongOrNull()
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Telegram auth field: auth_date")
        val hash = params["hash"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Telegram auth field: hash")
        val firstName = params["first_name"]
        val lastName = params["last_name"]
        val username = params["username"]
        val photoUrl = params["photo_url"]

        val payload =
            TelegramLoginPayload(
                id = id,
                firstName = firstName,
                lastName = lastName,
                username = username,
                photoUrl = photoUrl,
                authDate = authDate,
                hash = hash,
            )

        if (!telegramLoginVerifier.verify(payload)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Telegram auth payload")
        }

        loginUser(id, username, request, response)
        return "redirect:/admin"
    }

    @PostMapping("/mini-app")
    fun telegramMiniAppLogin(
        @RequestParam params: Map<String, String>,
        @RequestHeader(value = "Origin", required = false) origin: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val resolvedInitData = resolveInitData(params, request)
        if (resolvedInitData.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Telegram mini app field: initData")
        }

        if (!isAllowedOrigin(origin)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Mini app origin is not allowed")
        }

        val miniAppPayload =
            telegramLoginVerifier.verifyMiniApp(resolvedInitData)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Telegram mini app payload")

        loginUser(miniAppPayload.userId, miniAppPayload.username, request, response)
        return "redirect:/admin"
    }

    @GetMapping("/mini-app")
    fun telegramMiniAppLoginPage(): String = "mini-app-login"

    private fun isAllowedOrigin(origin: String?): Boolean {
        val requestOrigin = normalizeOrigin(origin)
        val allowedOrigins = listOfNotNull(normalizeOrigin(properties.miniAppUrl))
        if (allowedOrigins.isEmpty()) {
            return false
        }

        return requestOrigin != null && allowedOrigins.any { it.equals(requestOrigin, ignoreCase = true) }
    }

    private fun normalizeOrigin(value: String?): String? {
        if (value.isNullOrBlank()) {
            return null
        }

        val trimmed = value.trim()
        val uri =
            runCatching {
                URI(trimmed)
            }.getOrNull()
        if (uri?.scheme != null && uri.host != null) {
            val port = if (uri.port > 0) ":${uri.port}" else ""
            return "${uri.scheme.lowercase()}://${uri.host.lowercase()}$port"
        }

        return trimmed.removeSuffix("/").lowercase()
    }

    private fun resolveInitData(
        params: Map<String, String>,
        request: HttpServletRequest,
    ): String? {
        listOf("initData", "init_data", "tgWebAppData").forEach { key ->
            val value = params[key]
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        listOf("X-Telegram-Init-Data", "X-Telegram-WebApp-InitData").forEach { header ->
            val value = request.getHeader(header)
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        val rawBody = runCatching { request.reader.readText() }.getOrNull()?.trim().orEmpty()
        if (rawBody.isBlank()) {
            return null
        }

        val contentType = request.contentType ?: return null
        if (contentType.startsWith("application/json", ignoreCase = true)) {
            return runCatching {
                val node = ObjectMapper().readTree(rawBody)
                when {
                    node.hasNonNull("initData") -> node.path("initData").asText(null)
                    node.hasNonNull("init_data") -> node.path("init_data").asText(null)
                    node.hasNonNull("tgWebAppData") -> node.path("tgWebAppData").asText(null)
                    else -> null
                }
            }.getOrNull()
        }

        val queryMap =
            rawBody
                .split("&")
                .mapNotNull { segment ->
                    val idx = segment.indexOf('=')
                    if (idx <= 0) {
                        null
                    } else {
                        val key = URLDecoder.decode(segment.substring(0, idx), StandardCharsets.UTF_8)
                        val value = URLDecoder.decode(segment.substring(idx + 1), StandardCharsets.UTF_8)
                        key to value
                    }
                }.toMap()

        return queryMap["initData"]
            ?: queryMap["init_data"]
            ?: queryMap["tgWebAppData"]
            ?: rawBody.takeIf { it.contains("auth_date=") && it.contains("hash=") }
    }

    private fun loginUser(
        telegramUserId: Long,
        username: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val botUsername = properties.telegramLoginBotUsername.trim()
        val user = usersService.getOrCreateUser(botUsername, telegramUserId, username, "en")
        if (!permissionsService.isUserServiceAdmin(user)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access denied")
        }

        val principal = TelegramAdminPrincipal(userId = user.userId, username = user.username)
        val authentication =
            UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                listOf(SimpleGrantedAuthority("ROLE_SERVICE_ADMIN")),
            )
        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)
        request.getSession(true)
        request.changeSessionId()
        HttpSessionSecurityContextRepository().saveContext(securityContext, request, response)
    }
}

data class TelegramAdminPrincipal(
    val userId: Long,
    val username: String?,
)
