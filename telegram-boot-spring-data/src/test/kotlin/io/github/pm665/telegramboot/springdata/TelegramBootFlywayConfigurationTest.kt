package io.github.pm665.telegramboot.springdata

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(
    classes = [TelegramBootFlywayTestApplication::class],
    properties = [
        "spring.datasource.url=jdbc:h2:mem:telegramboot;MODE=MYSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.sql.init.mode=never",
    ],
)
class TelegramBootFlywayConfigurationTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var flyway: Flyway

    @Test
    fun `flyway respects configured datasource and creates tables`() {
        val flywayDataSource = flyway.configuration.dataSource
        assertThat(flywayDataSource)
            .describedAs("Flyway data source should come from spring.datasource properties")
            .isNotNull

        flywayDataSource!!.connection.use { connection ->
            assertThat(connection.metaData.url)
                .describedAs("Flyway should use the configured spring.datasource URL")
                .contains("jdbc:h2:mem:telegramboot")
        }
        assertThat(flyway.configuration.table).isEqualTo("flyway_schema_history_tg_boot")

        val actualTables =
            jdbcTemplate
                .queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                ).map { (it["TABLE_NAME"] as String).lowercase() }

        val expectedTables =
            listOf(
                "bot_users",
                "bot_chats",
                "bot_user_permissions",
                "chat_state_cache",
                "callback_data",
            )

        expectedTables.forEach { table ->
            assertThat(actualTables)
                .describedAs("Expected table %s to exist", table)
                .contains(table)
        }
    }
}

@SpringBootApplication
@Import(TelegramBootSpringDataConfiguration::class)
internal class TelegramBootFlywayTestApplication
