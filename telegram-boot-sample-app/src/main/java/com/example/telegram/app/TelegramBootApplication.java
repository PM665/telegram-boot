package com.example.telegram.app;

import com.example.telegram.bot.service.TelegramBotService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TelegramBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBootApplication.class, args);
    }

    @Bean
    CommandLineRunner telegramLogger(TelegramBotService service) {
        return args -> service.logConfiguredName();
    }
}
