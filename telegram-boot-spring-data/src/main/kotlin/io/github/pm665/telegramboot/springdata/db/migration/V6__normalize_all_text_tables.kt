package io.github.pm665.telegramboot.springdata.db.migration

import io.github.pm665.telegramboot.springdata.db.MySqlTextSchema
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Suppress("ClassName")
class V6__normalize_all_text_tables : BaseJavaMigration() {
    override fun migrate(context: Context) {
        MySqlTextSchema.normalizeTables(
            context.connection,
            setOf("bot_users", "bot_chats", "bot_user_permissions", "chat_state_cache", "callback_data", "bot_payment", "user_preferences"),
        )
    }
}
