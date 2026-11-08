package io.github.pm665.telegramboot.springdata.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class TelegramBootSpringDataAutoConfigurationTests {
    @Test
    fun `backs off when telegram boot spring data module missing`() {
        ApplicationContextRunner()
            .withClassLoader(FilteredClassLoader("io.github.pm665.telegramboot.springdata.TelegramBootSpringDataConfiguration"))
            .withConfiguration(AutoConfigurations.of(TelegramBootSpringDataAutoConfiguration::class.java))
            .run { context ->
                assertThat(context.containsBean("springDataMarker")).isFalse()
            }
    }
}
