package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.test.util.ReflectionTestUtils
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

class MaxConfigurationTest {
    @Test
    fun `MAX uses bounded JDK transport for bot info and command PATCH requests`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val objectMapper = ObjectMapper()
        val properties = MaxProperties().apply { apiUrl = "http://127.0.0.1:${server.address.port}" }
        val api = MaxConfiguration().maxApiClient(properties, objectMapper)
        val restClient = ReflectionTestUtils.getField(api, "restClient")!!
        val factory = ReflectionTestUtils.getField(restClient, "clientRequestFactory")
        assertThat(factory).isInstanceOf(JdkClientHttpRequestFactory::class.java)
        val httpClient = ReflectionTestUtils.getField(factory!!, "httpClient") as HttpClient
        httpClient.use {
            try {
                assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(5))
                assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(Duration.ofSeconds(20))
                val requests = CopyOnWriteArrayList<List<String>>()
                server.createContext("/me") { exchange ->
                    requests.add(
                        listOf(
                            exchange.requestMethod,
                            exchange.requestURI.path,
                            exchange.requestHeaders.getFirst("Authorization"),
                            exchange.requestBody.bufferedReader().readText(),
                        ),
                    )
                    val response = "{\"user_id\":123}".toByteArray()
                    exchange.sendResponseHeaders(200, response.size.toLong())
                    exchange.responseBody.use { it.write(response) }
                }
                server.start()

                assertThat(api.botInfo("test-token").path("user_id").asLong()).isEqualTo(123)
                api.setCommands("test-token", listOf(mapOf("name" to "start", "description" to "Start")))

                assertThat(requests.map { it.take(3) }).containsExactly(
                    listOf("GET", "/me", "test-token"),
                    listOf("PATCH", "/me/commands", "test-token"),
                )
                assertThat(objectMapper.readTree(requests.last()[3]).path("commands")[0].path("name").asText()).isEqualTo("start")
            } finally {
                server.stop(0)
            }
        }
    }
}
