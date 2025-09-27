package io.github.pm665.telegramboot.app

import io.github.pm665.telegramboot.bot.service.TelegramBotService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TelegramBootApplication {
    @Bean
    fun telegramLogger(service: TelegramBotService): CommandLineRunner =
        CommandLineRunner {
            service.logConfiguredName()
        }
}

fun main(args: Array<String>) {
    runApplication<TelegramBootApplication>(*args)
}
