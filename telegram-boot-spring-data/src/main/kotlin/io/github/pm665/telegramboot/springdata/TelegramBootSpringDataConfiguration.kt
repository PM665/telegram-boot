package io.github.pm665.telegramboot.springdata

import io.github.pm665.telegramboot.ports.BotsService
import io.github.pm665.telegramboot.ports.PermissionsServiceAdapter
import io.github.pm665.telegramboot.ports.UsersService
import io.github.pm665.telegramboot.springdata.adapters.DBBotPaymentsService
import io.github.pm665.telegramboot.springdata.adapters.DBCallbackDataService
import io.github.pm665.telegramboot.springdata.adapters.DBChatStateService
import io.github.pm665.telegramboot.springdata.adapters.DBPermissionsService
import io.github.pm665.telegramboot.springdata.adapters.DBUserPreferencesService
import io.github.pm665.telegramboot.springdata.adapters.DBUsersService
import io.github.pm665.telegramboot.springdata.ports.BotPaymentsRepository
import io.github.pm665.telegramboot.springdata.ports.BotUserPermissionsRepository
import io.github.pm665.telegramboot.springdata.ports.CallbackDataRepository
import io.github.pm665.telegramboot.springdata.ports.ChatStatesRepository
import io.github.pm665.telegramboot.springdata.ports.ChatsRepository
import io.github.pm665.telegramboot.springdata.ports.UserPreferencesRepository
import io.github.pm665.telegramboot.springdata.ports.UsersRepository
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@Configuration
@EnableJdbcRepositories(basePackages = ["io.github.pm665.telegramboot.springdata.ports"])
@Import(TelegramBootFlywayConfiguration::class)
class TelegramBootSpringDataConfiguration {
    @Bean
    fun dbUsersService(
        usersRepository: UsersRepository,
        chatsRepository: ChatsRepository,
    ): DBUsersService = DBUsersService(usersRepository, chatsRepository)

    @Bean
    fun dbChatStateService(chatStatesRepository: ChatStatesRepository): DBChatStateService = DBChatStateService(chatStatesRepository)

    @Bean
    fun dbBotPaymentsService(botPaymentsRepository: BotPaymentsRepository): DBBotPaymentsService =
        DBBotPaymentsService(botPaymentsRepository)

    @Bean
    fun dbPermissionsService(
        permissionsRepository: BotUserPermissionsRepository,
        usersService: UsersService,
        permissionsServiceAdapter: ObjectProvider<PermissionsServiceAdapter>,
        botsService: ObjectProvider<BotsService>,
    ): DBPermissionsService =
        DBPermissionsService(
            permissionsRepository,
            usersService,
            permissionsServiceAdapter.ifAvailable,
            botsService.ifAvailable,
        )

    @Bean
    fun dbCallbackDataService(callbackDataRepository: CallbackDataRepository): DBCallbackDataService =
        DBCallbackDataService(callbackDataRepository)

    @Bean
    fun dbUserPreferencesService(userPreferencesRepository: UserPreferencesRepository): DBUserPreferencesService =
        DBUserPreferencesService(userPreferencesRepository)
}
