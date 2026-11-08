package io.github.pm665.telegramboot.max

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

class MaxPlatformBotTokenValidatorTest {
    @Test
    fun `validates MAX token through its account endpoint and matches bot username`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("https://platform-api2.max.ru/me"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header("Authorization", "max-token"))
            .andRespond(withSuccess("""{"username":"max_driver","is_bot":true}""", MediaType.APPLICATION_JSON))

        MaxPlatformBotTokenValidator(MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()))
            .validate("max-token", "@max_driver")

        server.verify()
    }

    @Test
    fun `rejects MAX token registered under another bot username`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("https://platform-api2.max.ru/me"))
            .andRespond(withSuccess("""{"username":"other_bot","is_bot":true}""", MediaType.APPLICATION_JSON))

        assertThrows<IllegalArgumentException> {
            MaxPlatformBotTokenValidator(MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()))
                .validate("max-token", "max_driver")
        }

        server.verify()
    }

    @Test
    fun `normalizes invalid token response as a validation error`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("https://platform-api2.max.ru/me"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        assertThrows<IllegalArgumentException> {
            MaxPlatformBotTokenValidator(MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()))
                .validate("invalid-token", "max_driver")
        }

        server.verify()
    }

    @Test
    fun `preserves MAX server errors while validating token`() {
        val builder = RestClient.builder().baseUrl("https://platform-api2.max.ru")
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("https://platform-api2.max.ru/me"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows<RestClientResponseException> {
            MaxPlatformBotTokenValidator(MaxApiClient(builder.build(), com.fasterxml.jackson.databind.ObjectMapper()))
                .validate("token", "max_driver")
        }

        server.verify()
    }
}
