CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bot_username VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    preference_name VARCHAR(255) NOT NULL,
    value TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_preferences_bot_username_user_id_preference_name (bot_username, user_id, preference_name),
    KEY idx_user_preferences_bot_username_user_id (bot_username, user_id),
    KEY idx_user_preferences_bot_username_preference_name (bot_username, preference_name)
);
