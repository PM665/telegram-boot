package io.github.pm665.telegramboot.admin.security

import io.github.pm665.telegramboot.ports.PermissionsService
import io.github.pm665.telegramboot.ports.UsersService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.ui.ConcurrentModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

@SpringBootTest(
    classes = [
        AdminUISecurityConfiguration::class,
        TelegramAdminLoginController::class,
        TelegramLoginVerifier::class,
        AdminUISecurityConfigurationTest.TestControllers::class,
        AdminUISecurityConfigurationTest.TestBeans::class,
    ],
    properties = [
        "telegram-boot.admin.telegram-login-bot-token=test-token",
        "telegram-boot.admin.telegram-login-bot-username=test_bot",
        "telegram-boot.admin.mini-app-url=https://miniapp.example/mini-app",
    ],
)
@AutoConfigureMockMvc
class AdminUISecurityConfigurationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var usersService: UsersService

    @MockitoBean
    private lateinit var permissionsService: PermissionsService

    @Autowired
    private lateinit var telegramLoginVerifier: TelegramLoginVerifier

    @Autowired
    private lateinit var telegramAdminLoginController: TelegramAdminLoginController

    @Test
    fun `anonymous redirected to login for protected routes`() {
        mockMvc
            .perform(get("/operator-admin"))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("/login"))

        mockMvc
            .perform(get("/admin"))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("/login"))

        mockMvc
            .perform(get("/park-game-configs"))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("/login"))

        mockMvc
            .perform(get("/park-game-debug"))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("/login"))
    }

    @Test
    fun `anonymous allowed for static paths and login`() {
        mockMvc
            .perform(get("/op-admin/ping"))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/tg-boot-admin/ping"))
            .andExpect(status().isOk)
    }

    @Test
    fun `login page renders telegram widget config`() {
        val model = ConcurrentModel()
        val view = telegramAdminLoginController.loginPage(model)

        assertThat(view).isEqualTo("login")
        assertThat(model.getAttribute("telegramLoginBotUsername")).isEqualTo("test_bot")
        assertThat(model.getAttribute("telegramLoginWidgetEnabled")).isEqualTo(true)
        assertThat(model.getAttribute("telegramMiniAppAllowedOrigin")).isEqualTo("https://miniapp.example/mini-app")
    }

    @Test
    fun `mini app login entrypoint page is public`() {
        mockMvc
            .perform(get("/mini-app"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `authenticated non-admin gets forbidden`() {
        mockMvc
            .perform(get("/operator-admin"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["SERVICE_ADMIN"])
    fun `service-admin gets access to protected routes`() {
        mockMvc
            .perform(get("/operator-admin"))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/admin"))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/park-game-configs"))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/park-game-debug"))
            .andExpect(status().isOk)
    }

    @Test
    fun `valid telegram login creates authenticated session`() {
        val user = UsersService.User(userId = 42, username = "alice", locale = "en")
        `when`(usersService.getOrCreateUser("test_bot", 42, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)

        val authDate = System.currentTimeMillis() / 1000
        val hash =
            telegramLoginVerifier.calculateHash(
                TelegramLoginPayload(id = 42, username = "alice", authDate = authDate, hash = ""),
                "test-token",
            )

        val loginResult =
            mockMvc
                .perform(
                    post("/login/telegram")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "42")
                        .param("username", "alice")
                        .param("auth_date", authDate.toString())
                        .param("hash", hash),
                ).andExpect(status().isFound)
                .andExpect(redirectedUrl("/admin"))
                .andReturn()

        mockMvc
            .perform(get("/operator-admin").session(loginResult.request.session as org.springframework.mock.web.MockHttpSession))
            .andExpect(status().isOk)
    }

    @Test
    fun `valid mini app login creates authenticated session`() {
        val user = UsersService.User(userId = 42, username = "alice", locale = "en")
        `when`(usersService.getOrCreateUser("test_bot", 42, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)

        val authDate = System.currentTimeMillis() / 1000
        val initData =
            "auth_date=$authDate&query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A42%2C%22username%22%3A%22alice%22%7D"
        val hashDataCheckString =
            "auth_date=$authDate\nquery_id=AAHdF6IQAAAAAN0XohDhrOrc\nuser={\"id\":42,\"username\":\"alice\"}"
        val hash = telegramLoginVerifier.calculateMiniAppHash(hashDataCheckString, "test-token")

        val loginResult =
            mockMvc
                .perform(
                    post("/mini-app")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Origin", "https://miniapp.example")
                        .param("initData", "$initData&hash=$hash"),
                ).andExpect(status().isFound)
                .andExpect(redirectedUrl("/admin"))
                .andReturn()

        mockMvc
            .perform(get("/operator-admin").session(loginResult.request.session as org.springframework.mock.web.MockHttpSession))
            .andExpect(status().isOk)
    }

    @Test
    fun `valid mini app json login creates authenticated session`() {
        val user = UsersService.User(userId = 42, username = "alice", locale = "en")
        `when`(usersService.getOrCreateUser("test_bot", 42, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)

        val authDate = System.currentTimeMillis() / 1000
        val initData =
            "auth_date=$authDate&query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A42%2C%22username%22%3A%22alice%22%7D"
        val hashDataCheckString =
            "auth_date=$authDate\nquery_id=AAHdF6IQAAAAAN0XohDhrOrc\nuser={\"id\":42,\"username\":\"alice\"}"
        val hash = telegramLoginVerifier.calculateMiniAppHash(hashDataCheckString, "test-token")

        val loginResult =
            mockMvc
                .perform(
                    post("/mini-app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://miniapp.example")
                        .content("""{"initData":"$initData&hash=$hash"}"""),
                ).andExpect(status().isFound)
                .andExpect(redirectedUrl("/admin"))
                .andReturn()

        mockMvc
            .perform(get("/operator-admin").session(loginResult.request.session as org.springframework.mock.web.MockHttpSession))
            .andExpect(status().isOk)
    }

    @Test
    fun `valid mini app alias field login creates authenticated session`() {
        val user = UsersService.User(userId = 42, username = "alice", locale = "en")
        `when`(usersService.getOrCreateUser("test_bot", 42, "alice", "en")).thenReturn(user)
        `when`(permissionsService.isUserServiceAdmin(user)).thenReturn(true)

        val authDate = System.currentTimeMillis() / 1000
        val initData =
            "auth_date=$authDate&query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A42%2C%22username%22%3A%22alice%22%7D"
        val hashDataCheckString =
            "auth_date=$authDate\nquery_id=AAHdF6IQAAAAAN0XohDhrOrc\nuser={\"id\":42,\"username\":\"alice\"}"
        val hash = telegramLoginVerifier.calculateMiniAppHash(hashDataCheckString, "test-token")

        val loginResult =
            mockMvc
                .perform(
                    post("/mini-app")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Origin", "https://miniapp.example")
                        .param("init_data", "$initData&hash=$hash"),
                ).andExpect(status().isFound)
                .andExpect(redirectedUrl("/admin"))
                .andReturn()

        mockMvc
            .perform(get("/operator-admin").session(loginResult.request.session as org.springframework.mock.web.MockHttpSession))
            .andExpect(status().isOk)
    }

    @Test
    fun `mini app login without init data returns bad request`() {
        mockMvc
            .perform(
                post("/mini-app")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Origin", "https://miniapp.example"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `mini app login forbidden when origin not allowed`() {
        val authDate = System.currentTimeMillis() / 1000
        mockMvc
            .perform(
                post("/mini-app")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Origin", "https://bad.example")
                    .param("initData", "auth_date=$authDate"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `invalid telegram payload rejected`() {
        mockMvc
            .perform(
                post("/login/telegram")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("id", "42")
                    .param("auth_date", (System.currentTimeMillis() / 1000).toString())
                    .param("hash", "bad"),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `missing telegram id returns bad request with explicit message`() {
        mockMvc
            .perform(
                post("/login/telegram")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("auth_date", (System.currentTimeMillis() / 1000).toString())
                    .param("hash", "bad"),
            ).andExpect(status().isBadRequest)
    }

    @Configuration
    class TestBeans {
        @Bean
        fun mvcHandlerMappingIntrospector(): HandlerMappingIntrospector = HandlerMappingIntrospector()

        @Bean
        fun telegramAdminProperties(): TelegramAdminProperties =
            TelegramAdminProperties(
                telegramLoginBotToken = "test-token",
                telegramLoginBotUsername = "test_bot",
                miniAppUrl = "https://miniapp.example/mini-app",
            )
    }

    @RestController
    class TestControllers {
        @GetMapping("/operator-admin")
        fun operatorAdmin(): String = "ok"

        @GetMapping("/admin")
        fun admin(): String = "ok"

        @GetMapping("/park-game-configs")
        fun parkGameConfigs(): String = "ok"

        @GetMapping("/park-game-debug")
        fun parkGameDebug(): String = "ok"

        @GetMapping("/op-admin/ping")
        fun opStatic(): String = "ok"

        @GetMapping("/tg-boot-admin/ping")
        fun tgStatic(): String = "ok"
    }
}
