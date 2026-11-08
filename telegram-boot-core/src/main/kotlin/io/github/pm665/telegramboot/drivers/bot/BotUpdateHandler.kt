package io.github.pm665.telegramboot.drivers.bot

import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.CommandAndState
import io.github.pm665.telegramboot.domain.bot.BotChatType
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.drivers.bot.BotActionExecutorService
import io.github.pm665.telegramboot.drivers.bot.BotErrorEventListener.BotErrorEvent
import io.github.pm665.telegramboot.features.callbacks.CallbackDataService
import io.github.pm665.telegramboot.ports.BotUpdateObserver
import io.github.pm665.telegramboot.ports.ChatStatesService
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.util.Base64

class BotUpdateHandler(
    private val chatStatesService: ChatStatesService,
    private val botActionExecutorService: BotActionExecutorService,
    private val callbackDataService: CallbackDataService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val updateObservers: List<BotUpdateObserver> = emptyList(),
    private val observationRegistry: ObservationRegistry = ObservationRegistry.NOOP,
) {
    private val log = LoggerFactory.getLogger(BotUpdateHandler::class.java)

    fun handleUpdate(
        botUsername: String,
        incomingUpdate: BotUpdate,
    ) {
        val update = resolveDeepLink(resolveCallbackData(incomingUpdate))
        val chatId = ChatUtils.extractChatId(update)
        val calledCommand = ChatUtils.extractCommand(update)
        val callback = ChatUtils.extractCallbackText(update)
        try {
            Observation.createNotStarted("bot.update", observationRegistry)
                .contextualName("bot update")
                .observe {
                    BotUpdateMdc.withUpdateContext(botUsername, update, calledCommand, callback) {
                        if (updateObservers.any { it.observeUpdate(botUsername, update) }) {
                            log.debug("Update handled by bot update observer")
                            return@withUpdateContext
                        }
                        if (!isPrivateUserInteraction(update)) {
                            log.debug("Ignoring update outside private user chat: updateId={}", update.updateId)
                            return@withUpdateContext
                        }
                        val chatState = chatStatesService.getChatState(chatId, botUsername)
                        val callbackObj = ChatUtils.extractCommandCallback(update)

                        log.debug(
                            "Update received: chatId={} chatState={} calledCommand={} callback={}",
                            chatId,
                            chatState,
                            calledCommand,
                            callback,
                        )

                        val commandAndState =
                            when {
                                callbackObj != null -> CommandAndState(callbackObj.command, callbackObj.state, callbackObj.callback)
                                calledCommand != null -> CommandAndState(calledCommand)
                                chatState != null -> CommandAndState(chatState, callback)
                                else -> CommandAndState.INVALID
                            }
                        botActionExecutorService.executeActionForCommandAndState(commandAndState, botUsername, update)
                    }
                }
        } catch (e: RuntimeException) {
            log.error("Error while processing update", e)
            applicationEventPublisher.publishEvent(BotErrorEvent(botUsername, update, e))
        }
    }

    private fun isPrivateUserInteraction(update: BotUpdate): Boolean =
        update.message?.let { it.chat?.type == BotChatType.PRIVATE && it.from != null }
            ?: (update.callbackQuery?.let { it.message?.chat?.type == BotChatType.PRIVATE && it.from != null } ?: false)

    private fun resolveCallbackData(update: BotUpdate): BotUpdate {
        val callbackQuery = update.callbackQuery ?: return update
        val callbackData = callbackQuery.data
        if (callbackData.isNullOrBlank() || !callbackData.startsWith(CallbackDataService.PREFIX)) return update
        val existing = callbackDataService.findById(callbackData) ?: return update
        return update.copy(callbackQuery = callbackQuery.copy(data = existing.data))
    }

    private fun resolveDeepLink(update: BotUpdate): BotUpdate {
        val message = update.message ?: return update
        val text = message.text ?: return update
        if (!text.startsWith("/start ")) return update
        val deepLink = text.substring(7).trim()
        if (deepLink.isBlank()) return update
        val decodedLink = String(Base64.getDecoder().decode(deepLink))
        if (decodedLink.isBlank() || !decodedLink.startsWith("/")) return update
        return update.copy(message = message.copy(text = decodedLink))
    }
}
