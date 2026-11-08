-- README: Future schema changes for telegram-boot spring-data entities must be added to this module's migration set to keep it self-contained.

CREATE TABLE IF NOT EXISTS bot_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    locale VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bot_users_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS bot_chats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chat_id BIGINT NOT NULL,
    bot_username VARCHAR(255),
    user_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bot_chats_chat_id_bot_username (chat_id, bot_username),
    KEY idx_bot_chats_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS bot_user_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bot_username VARCHAR(255),
    access_type VARCHAR(255) NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uq_bot_user_permissions_user_id_bot_username_access_type (user_id, bot_username, access_type),
    KEY idx_bot_user_permissions_user_id_access_type (user_id, access_type)
);

CREATE TABLE IF NOT EXISTS chat_state_cache (
    chat_id BIGINT NOT NULL,
    bot_username VARCHAR(255) NOT NULL,
    command VARCHAR(65),
    state VARCHAR(255) NOT NULL,
    PRIMARY KEY (chat_id, bot_username)
);

CREATE TABLE IF NOT EXISTS callback_data (
    id VARCHAR(64) NOT NULL,
    data TEXT NOT NULL,
    PRIMARY KEY (id)
);
