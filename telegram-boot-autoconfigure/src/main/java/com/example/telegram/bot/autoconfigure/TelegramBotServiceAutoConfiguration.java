package com.example.telegram.bot.autoconfigure;

import com.example.telegram.bot.service.TelegramBotProperties;
import com.example.telegram.bot.service.TelegramBotService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that exposes {@link TelegramBotService} when the starter is on the
 * classpath.
 */
@AutoConfiguration
@ConditionalOnClass(TelegramBotService.class)
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramBotServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TelegramBotService telegramBotService(TelegramBotProperties properties) {
        return new TelegramBotService(properties);
    }
}
