package io.github.pm665.telegramboot.springdata.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("callback_data")
class DBCallbackData(
    @Id
    val id: String,
    val data: String,
)
