package io.github.pm665.telegramboot.springdata

import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class TelegramBootFlywayConfiguration {
    @Bean
    fun telegramBootFlyway(dataSource: DataSource): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:io/github/pm665/telegramboot/springdata/db/migration")
            .table("flyway_schema_history_tg_boot")
            // When multiple independent Flyway configurations run against the same
            // test database, baselining to version 0 ensures a non-empty schema
            // will get initialized without skipping migrations that start at v1.
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .also { flyway -> flyway.migrate() }
}
