package io.github.pm665.telegramboot.springdata.autoconfigure

import io.github.pm665.telegramboot.core.autoconfigure.TelegramBootCoreAutoConfiguration
import io.github.pm665.telegramboot.springdata.TelegramBootSpringDataConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Import

@AutoConfiguration(before = [TelegramBootCoreAutoConfiguration::class])
@Import(TelegramBootSpringDataAutoConfiguration.SpringDataConfigurationImport::class)
class TelegramBootSpringDataAutoConfiguration {
    @ConditionalOnClass(
        name = [
            "org.springframework.data.repository.CrudRepository",
            "io.github.pm665.telegramboot.springdata.TelegramBootSpringDataConfiguration",
        ],
    )
    @Import(TelegramBootSpringDataConfiguration::class)
    class SpringDataConfigurationImport
}
