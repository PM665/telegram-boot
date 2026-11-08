import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import sample.SampleApplication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

@Testcontainers
@SpringBootTest(classes = [SampleApplication::class])
class TelegramBotE2eTest {
    companion object {
        @Container
        @JvmStatic
        val telegram = GenericContainer("ghcr.io/evilfreelancer/tgfake:1.4.0")
            .withExposedPorts(18790)
            .withCommand("--token", "123:fake", "--bot-username", "tgfake_bot")
            .waitingFor(Wait.forHttp("/sim/state"))

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("sample.bot.token") { "123:fake" }
            registry.add("sample.bot.username") { "tgfake_bot" }
            registry.add("telegram-boot.telegram.api-url") { "http://${telegram.host}:${telegram.getMappedPort(18790)}" }
        }
    }

    @Test
    fun `start sends rich menu and invokes a command on callback`() {
        val baseUrl = "http://${telegram.host}:${telegram.getMappedPort(18790)}"
        val client = HttpClient.newHttpClient()
        fun request(path: String, body: String? = null): HttpResponse<String> {
            val builder = HttpRequest.newBuilder(URI.create("$baseUrl$path")).timeout(Duration.ofSeconds(5))
            if (body != null) builder.POST(HttpRequest.BodyPublishers.ofString(body))
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        }
        fun awaitChat(predicate: (String) -> Boolean): String {
            val deadline = Instant.now().plusSeconds(10)
            var chat: String
            do {
                chat = request("/sim/chat/4244").body()
                if (predicate(chat)) return chat
                Thread.sleep(100)
            } while (Instant.now().isBefore(deadline))
            return chat
        }

        assertThat(request("/sim/message", "{\"text\":\"/start\",\"user_id\":4244,\"username\":\"startuser\"}").statusCode()).isEqualTo(200)
        val menu = awaitChat { it.contains("\"rich\":true") && it.contains("/hello") }
        assertThat(menu).contains("\"rich\":true", "/hello")
        assertThat(request("/sim/callback", "{\"data\":\"/hello\",\"message_id\":2,\"user_id\":4244,\"username\":\"startuser\"}").statusCode()).isEqualTo(200)
        val chat = awaitChat { it.contains("Hello startuser") && it.contains("cbq-") }
        assertThat(chat).contains("Hello startuser", "cbq-")
    }

    @Test
    fun `hello command returns a greeting`() {
        val baseUrl = "http://${telegram.host}:${telegram.getMappedPort(18790)}"
        val client = HttpClient.newHttpClient()
        fun request(path: String, body: String? = null): HttpResponse<String> {
            val builder = HttpRequest.newBuilder(URI.create("$baseUrl$path")).timeout(Duration.ofSeconds(5))
            if (body != null) builder.POST(HttpRequest.BodyPublishers.ofString(body))
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        }

        assertThat(request("/sim/message", "{\"text\":\"/hello\"}").statusCode()).isEqualTo(200)
        val deadline = Instant.now().plusSeconds(10)
        var chat: String
        do {
            chat = request("/sim/chat/4242?format=text").body()
            if (chat.contains("Hello @alice")) break
            Thread.sleep(100)
        } while (Instant.now().isBefore(deadline))
        assertThat(chat).contains("Hello @alice")
    }

    @Test
    fun `hello command sends an escaped username in Markdown`() {
        val baseUrl = "http://${telegram.host}:${telegram.getMappedPort(18790)}"
        val client = HttpClient.newHttpClient()
        fun request(path: String, body: String? = null): HttpResponse<String> {
            val builder = HttpRequest.newBuilder(URI.create("$baseUrl$path")).timeout(Duration.ofSeconds(5))
            if (body != null) builder.POST(HttpRequest.BodyPublishers.ofString(body))
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        }

        assertThat(request("/sim/message", "{\"text\":\"/hello\",\"user_id\":4243,\"username\":\"test_user\"}").statusCode()).isEqualTo(200)
        val deadline = Instant.now().plusSeconds(10)
        var chat: String
        do {
            chat = request("/sim/chat/4243").body()
            if (chat.contains("Hello @test\\\\_user")) break
            Thread.sleep(100)
        } while (Instant.now().isBefore(deadline))
        assertThat(chat).contains("Hello @test\\\\_user", "\"parse_mode\":\"Markdown\"")
    }
}
