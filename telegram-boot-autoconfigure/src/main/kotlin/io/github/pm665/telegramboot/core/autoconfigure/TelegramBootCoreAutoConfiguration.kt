package io.github.pm665.telegramboot.core.autoconfigure

import io.github.pm665.telegramboot.TelegramBootConfiguration
import io.github.pm665.telegramboot.infrastructure.BackendConfigProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

@AutoConfiguration
@ConditionalOnClass(TelegramBootConfiguration::class)
@Import(TelegramBootConfiguration::class)
@EnableConfigurationProperties(BackendConfigProperties::class)
class TelegramBootCoreAutoConfiguration
