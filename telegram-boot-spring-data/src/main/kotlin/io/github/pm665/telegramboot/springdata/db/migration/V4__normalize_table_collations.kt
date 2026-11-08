package io.github.pm665.telegramboot.springdata.db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Suppress("ClassName")
class V4__normalize_table_collations : BaseJavaMigration() {
    override fun migrate(context: Context) {
        if (!isMySqlFamily(context)) {
            return
        }
        executeStatements(
            context,
            "ALTER TABLE bot_chats CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE bot_user_permissions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE chat_state_cache CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE bot_payment CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            "ALTER TABLE user_preferences CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
        )
    }

    private fun isMySqlFamily(context: Context): Boolean {
        val productName = context.connection.metaData.databaseProductName.lowercase()
        return productName.contains("mysql") || productName.contains("mariadb")
    }

    private fun executeStatements(
        context: Context,
        vararg statements: String,
    ) {
        context.connection.createStatement().use { statement ->
            statements.forEach(statement::execute)
        }
    }
}
