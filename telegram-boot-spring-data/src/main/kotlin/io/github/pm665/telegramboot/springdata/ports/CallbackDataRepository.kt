package io.github.pm665.telegramboot.springdata.ports

import io.github.pm665.telegramboot.springdata.domain.DBCallbackData
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository

interface CallbackDataRepository : ListCrudRepository<DBCallbackData, String> {
    @Query(
        """
            INSERT INTO callback_data (id, data)
            VALUES (:#{#callbackData.id}, :#{#callbackData.data})
            ON DUPLICATE KEY UPDATE data = :#{#callbackData.data}
            """,
    )
    @Modifying
    fun createNew(callbackData: DBCallbackData?)
}
