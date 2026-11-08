package io.github.pm665.telegramboot.max

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@AutoConfiguration
@ConditionalOnClass(RestClient::class)
@EnableConfigurationProperties(MaxProperties::class)
@Import(
    MaxBotManager::class,
    MaxMessageSender::class,
    MaxWebhookController::class,
    MaxPlatformBotTokenValidator::class,
)
class MaxConfiguration {
    @Bean(name = ["maxApiClient"])
    internal fun maxApiClient(
        properties: MaxProperties,
        objectMapper: ObjectMapper,
    ): MaxApiClient {
        val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(Duration.ofSeconds(20)) }
        return MaxApiClient(
            RestClient.builder().requestFactory(requestFactory).baseUrl(properties.apiUrl).build(),
            objectMapper,
        )
    }

    @Bean(name = ["maxUpdateMapper"])
    internal fun maxUpdateMapper(objectMapper: ObjectMapper): MaxUpdateMapper = MaxUpdateMapper(objectMapper)

    @Bean
    internal fun maxMessageRateLimiter(): MaxMessageRateLimiter = MaxMessageRateLimiter()
}
