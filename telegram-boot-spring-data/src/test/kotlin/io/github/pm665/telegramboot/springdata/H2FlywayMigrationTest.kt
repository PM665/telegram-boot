package io.github.pm665.telegramboot.springdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:telegramboottest;MODE=MYSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.flyway.enabled=true",
        "spring.sql.init.mode=never",
    ],
)
class H2FlywayMigrationTest {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `flyway migrations execute against h2 mysql mode`() {
        val tableCount =
            checkNotNull(
                jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?",
                    Long::class.java,
                    "BOT_USERS",
                ),
            )

        assertThat(tableCount).isEqualTo(1L)
    }
}
