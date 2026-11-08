package io.github.pm665.telegramboot.springdata.db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.sql.SQLException

@Suppress("ClassName")
class V5__scope_bot_users_by_bot_username : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        if (!hasColumn(connection, "bot_users", "bot_username")) {
            connection.createStatement().use { it.execute("ALTER TABLE bot_users ADD COLUMN bot_username VARCHAR(255) NULL") }
        }

        // Drop the old global key before cloning users into their bot-specific rows.
        dropGlobalUserIdUniqueIndexes(connection)

        val usernamesByUserId = mutableMapOf<Long, MutableSet<String>>()
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    "SELECT DISTINCT user_id, bot_username FROM bot_chats WHERE bot_username IS NOT NULL ORDER BY user_id, bot_username",
                ).use { rows ->
                    while (rows.next()) {
                        usernamesByUserId.getOrPut(rows.getLong("user_id"), ::linkedSetOf).add(rows.getString("bot_username"))
                    }
                }
        }

        val existingUsers =
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id, user_id, username, locale, enabled, bot_username FROM bot_users ORDER BY id").use { rows ->
                    buildList {
                        while (rows.next()) {
                            add(
                                ExistingBotUser(
                                    id = rows.getLong("id"),
                                    userId = rows.getLong("user_id"),
                                    username = rows.getString("username"),
                                    locale = rows.getString("locale"),
                                    enabled = rows.getBoolean("enabled"),
                                    botUsername = rows.getString("bot_username"),
                                ),
                            )
                        }
                    }
                }
            }

        connection.prepareStatement("UPDATE bot_users SET bot_username = ? WHERE id = ?").use { update ->
            connection.prepareStatement(
                "INSERT INTO bot_users (bot_username, user_id, username, locale, enabled) " +
                    "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS " +
                    "(SELECT 1 FROM bot_users WHERE bot_username = ? AND user_id = ?)",
            ).use { insert ->
                existingUsers.forEach { user ->
                    val botUsernames = usernamesByUserId[user.userId].orEmpty().toList()
                    if (botUsernames.isEmpty()) return@forEach

                    val retainedBotUsername = user.botUsername?.takeIf(botUsernames::contains) ?: botUsernames.first()
                    if (user.botUsername != retainedBotUsername) {
                        update.setString(1, retainedBotUsername)
                        update.setLong(2, user.id)
                        update.executeUpdate()
                    }

                    botUsernames.filterNot { it == retainedBotUsername }.forEach { botUsername ->
                        insert.setString(1, botUsername)
                        insert.setLong(2, user.userId)
                        insert.setString(3, user.username)
                        insert.setString(4, user.locale)
                        insert.setBoolean(5, user.enabled)
                        insert.setString(6, botUsername)
                        insert.setLong(7, user.userId)
                        insert.executeUpdate()
                    }
                }
            }
        }

        if (!hasIndex(connection, "bot_users", "uq_bot_users_bot_username_user_id")) {
            connection.createStatement().use {
                it.execute("CREATE UNIQUE INDEX uq_bot_users_bot_username_user_id ON bot_users (bot_username, user_id)")
            }
        }
    }

    private data class ExistingBotUser(
        val id: Long,
        val userId: Long,
        val username: String?,
        val locale: String?,
        val enabled: Boolean,
        val botUsername: String? = null,
    )

    private fun dropGlobalUserIdUniqueIndexes(connection: Connection) {
        val indexes = linkedMapOf<String, MutableList<String>>()
        val tableName =
            connection.metaData
                .getTables(connection.catalog, null, "%", arrayOf("TABLE"))
                .use { tables ->
                    generateSequence { if (tables.next()) tables.getString("TABLE_NAME") else null }
                        .first { it.equals("bot_users", ignoreCase = true) }
                }
        connection.metaData.getIndexInfo(connection.catalog, null, tableName, true, false).use { rows ->
            while (rows.next()) {
                val name = rows.getString("INDEX_NAME") ?: continue
                val column = rows.getString("COLUMN_NAME") ?: continue
                if (!rows.getBoolean("NON_UNIQUE")) indexes.getOrPut(name, ::mutableListOf).add(column)
            }
        }
        indexes
            .filterValues { columns -> columns.size == 1 && columns.single().equals("user_id", ignoreCase = true) }
            .keys
            .filterNot { it.equals("PRIMARY", ignoreCase = true) }
            .forEach { name ->
                val safeName = name.takeIf { it.matches(Regex("[A-Za-z0-9_]+")) } ?: error("Unexpected bot_users index name")
                if (isMySqlFamily(connection)) {
                    connection.createStatement().use { it.execute("ALTER TABLE bot_users DROP INDEX `$safeName`") }
                } else {
                    dropH2Unique(connection, safeName)
                }
            }
    }

    private fun hasColumn(
        connection: Connection,
        tableName: String,
        columnName: String,
    ): Boolean =
        connection.metaData.getColumns(connection.catalog, null, null, null).use { columns ->
            while (columns.next()) {
                if (columns.getString("TABLE_NAME").equals(tableName, ignoreCase = true) &&
                    columns.getString("COLUMN_NAME").equals(columnName, ignoreCase = true)
                ) {
                    return true
                }
            }
            false
        }

    private fun hasIndex(
        connection: Connection,
        tableName: String,
        indexName: String,
    ): Boolean =
        connection.metaData.getIndexInfo(connection.catalog, null, tableName, false, false).use { indexes ->
            while (indexes.next()) {
                if (indexes.getString("INDEX_NAME").equals(indexName, ignoreCase = true)) return true
            }
            false
        }

    private fun dropH2Unique(
        connection: Connection,
        name: String,
    ) {
        try {
            connection.createStatement().use { it.execute("ALTER TABLE bot_users DROP CONSTRAINT \"$name\"") }
        } catch (_: SQLException) {
            val constraintName = name.replace(Regex("_INDEX_\\d+$"), "")
            try {
                connection.createStatement().use { it.execute("ALTER TABLE bot_users DROP CONSTRAINT \"$constraintName\"") }
            } catch (_: SQLException) {
                connection.createStatement().use { it.execute("DROP INDEX \"$name\"") }
            }
        }
    }

    private fun isMySqlFamily(connection: Connection): Boolean {
        val productName = connection.metaData.databaseProductName.lowercase()
        return productName.contains("mysql") || productName.contains("mariadb")
    }
}
