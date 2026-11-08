package io.github.pm665.telegramboot.springdata.db

import java.sql.Connection

object MySqlTextSchema {
    fun normalizeDatabase(connection: Connection) {
        if (!isMySql(connection)) return
        val tables =
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT TABLE_NAME FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
                      AND TABLE_NAME NOT LIKE '%flyway_schema_history%'
                    """.trimIndent(),
                ).use { rows -> buildSet { while (rows.next()) add(rows.getString(1)) } }
            }
        normalizeTables(connection, tables)
        connection.createStatement().use {
            it.execute("ALTER DATABASE ${quote(connection.catalog)} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
        }
    }

    fun normalizeTables(
        connection: Connection,
        tables: Set<String>,
    ) {
        if (!isMySql(connection)) return
        val pending =
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT t.TABLE_NAME FROM information_schema.TABLES t
                    WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_TYPE = 'BASE TABLE'
                      AND (t.TABLE_COLLATION <> 'utf8mb4_unicode_ci' OR EXISTS (
                          SELECT 1 FROM information_schema.COLUMNS c
                          WHERE c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
                            AND c.COLLATION_NAME <> 'utf8mb4_unicode_ci'
                      ))
                    ORDER BY t.TABLE_NAME
                    """.trimIndent(),
                ).use { rows -> buildList { while (rows.next()) if (rows.getString(1) in tables) add(rows.getString(1)) } }
            }
        // MySQL DDL commits independently; reject conflicting unique keys before converting any table.
        pending.forEach { checkUniqueKeys(connection, it) }
        pending.forEach { table ->
            connection.createStatement().use {
                it.execute("ALTER TABLE ${quote(table)} CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
            }
        }
    }

    private fun checkUniqueKeys(
        connection: Connection,
        table: String,
    ) {
        val indexes = linkedMapOf<String, MutableList<IndexColumn>>()
        connection.prepareStatement(
            """
            SELECT s.INDEX_NAME, s.COLUMN_NAME, s.SUB_PART, c.COLLATION_NAME
            FROM information_schema.STATISTICS s
            LEFT JOIN information_schema.COLUMNS c
              ON c.TABLE_SCHEMA = s.TABLE_SCHEMA AND c.TABLE_NAME = s.TABLE_NAME AND c.COLUMN_NAME = s.COLUMN_NAME
            WHERE s.TABLE_SCHEMA = DATABASE() AND s.TABLE_NAME = ? AND s.NON_UNIQUE = 0
            ORDER BY s.INDEX_NAME, s.SEQ_IN_INDEX
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, table)
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    val index = rows.getString("INDEX_NAME")
                    val column = rows.getString("COLUMN_NAME")
                    requireNotNull(column) { "Cannot preflight functional unique index $table.$index" }
                    indexes.getOrPut(index, ::mutableListOf).add(
                        IndexColumn(column, rows.getInt("SUB_PART").takeIf { it > 0 }, rows.getString("COLLATION_NAME") != null),
                    )
                }
            }
        }
        indexes.filterValues { columns -> columns.any { it.text } }.forEach { (index, columns) ->
            val expressions =
                columns.joinToString { column ->
                    val value = column.prefixLength?.let { "LEFT(${quote(column.name)}, $it)" } ?: quote(column.name)
                    if (column.text) "CONVERT($value USING utf8mb4) COLLATE utf8mb4_unicode_ci" else value
                }
            val nonNull = columns.joinToString(" AND ") { "${quote(it.name)} IS NOT NULL" }
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT 1 FROM ${quote(table)} WHERE $nonNull GROUP BY $expressions HAVING COUNT(*) > 1 LIMIT 1",
                ).use { rows ->
                    check(!rows.next()) { "Text normalization would duplicate unique key $table.$index; resolve conflicting rows first" }
                }
            }
        }
    }

    private fun isMySql(connection: Connection): Boolean =
        connection.metaData.databaseProductName.lowercase().let { it.contains("mysql") || it.contains("mariadb") }

    private fun quote(identifier: String): String = "`${identifier.replace("`", "``")}`"

    private data class IndexColumn(
        val name: String,
        val prefixLength: Int?,
        val text: Boolean,
    )
}
