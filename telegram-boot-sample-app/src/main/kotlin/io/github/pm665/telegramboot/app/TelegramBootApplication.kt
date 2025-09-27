package io.github.pm665.telegramboot.app

import io.github.pm665.telegramboot.domain.configuration.TelegramBootService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TelegramBootApplication {
    @Bean
    fun telegramLogger(service: TelegramBootService): CommandLineRunner =
        CommandLineRunner {
            service.logConfiguredName()
        }
}

fun main(args: Array<String>) {
    runApplication<TelegramBootApplication>(*args)
}
