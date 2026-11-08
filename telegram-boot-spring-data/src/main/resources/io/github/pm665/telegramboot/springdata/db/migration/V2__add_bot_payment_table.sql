CREATE TABLE IF NOT EXISTS bot_payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bot_username VARCHAR(255) NOT NULL,
    next_payment_date DATE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bot_payment_bot_username (bot_username)
);
