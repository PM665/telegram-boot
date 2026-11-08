package io.github.pm665.telegramboot.features.experiment

import io.github.pm665.telegramboot.domain.AccessType
import io.github.pm665.telegramboot.domain.BotActionContext
import io.github.pm665.telegramboot.domain.ChatUtils
import io.github.pm665.telegramboot.domain.Visibility
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuButton
import io.github.pm665.telegramboot.domain.actions.SendMessageMarkup.MenuMarkup
import io.github.pm665.telegramboot.domain.bot.BotUpdate
import io.github.pm665.telegramboot.features.experiment.Actions.ACTION_DISCARD
import io.github.pm665.telegramboot.features.experiment.Actions.ACTION_SAVE
import io.github.pm665.telegramboot.features.experiment.Actions.ACTION_TOGGLE
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

class IncomingMessageFactory {
    fun processUpdate(update: BotUpdate): IncomingMessage? {
        if (ChatUtils.Flag.TEXT.test(update) || ChatUtils.Flag.CALLBACK_QUERY.test(update)) {
            val messageText = ChatUtils.extractText(update)
            if (messageText.startsWith("/")) {
                val parts = messageText.split(" ")
                val command = parts[0].replaceFirst("/", "")
                val args = parts.drop(1)
                return if (args.isEmpty()) {
                    CalledCommand(update, command, args)
                } else {
                    CalledAction(update, command, args)
                }
            } else {
                return IncomingText(update, messageText)
            }
        } else {
            // Handle other kinds of messages (locations, etc.) as needed
            return null
        }
    }
}

class OutgoingMessage {
    // text --done
    // text + menu --done
    // Responses
    // multiple messages
    // message operation (delete message + edit message)
    // other kinds of messages (locations, images)
}

abstract class Command(
    var name: String,
    var visibility: Visibility = Visibility.HIDDEN,
    var accessType: AccessType = AccessType.CREATOR,
    var contextClass: KClass<out BotActionContext> = BotActionContext::class,
) {
    abstract fun callCommand(
        context: BotActionContext,
        arguments: List<String>,
    )
}

abstract class AccessibleCommand(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    accessType: AccessType = AccessType.USER,
    contextClass: KClass<out BotActionContext>,
    val displayName: String = "$name.command",
    val description: String = displayName,
) : Command(name = name, visibility = visibility, accessType = accessType, contextClass = contextClass)

interface CustomBehavior {
    val maxDelaySeconds: Long
        get() = 0
    val keepOriginalMessage: Boolean
        get() = false
}

abstract class Menu(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    accessType: AccessType = AccessType.USER,
    contextClass: KClass<out BotActionContext>,
    displayName: String = "$name.command",
    description: String = displayName,
    parentMenu: String = "start",
    row: Long = 0,
    order: Long = 0,
) : MenuItem(
        name = name,
        visibility = visibility,
        accessType = accessType,
        contextClass = contextClass,
        displayName = displayName,
        description = description,
        parentMenu = parentMenu,
        row = row,
        order = order,
    ) {
    abstract fun addBackButton(): Boolean

    abstract fun getMenuItems(): Set<MenuItem>

    abstract fun callAction(
        context: BotActionContext,
        action: String,
        arguments: List<String>,
    ): Set<MenuItem>

    override fun callCommand(
        context: BotActionContext,
        arguments: List<String>,
    ) {
        TODO("Not yet implemented. Not supposed to be called")
    }

    override fun getMarkup(context: BotActionContext): SendMessageMarkup.MenuButton = MenuButton(displayName, callbackData = "/$name")
}

abstract class ToggleMenu(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    accessType: AccessType = AccessType.USER,
    contextClass: KClass<out BotActionContext>,
    displayName: String = "$name.command",
    description: String = displayName,
    parentMenu: String = "start",
    row: Long = 0,
    order: Long = 0,
) : Menu(
        name = name,
        visibility = visibility,
        accessType = accessType,
        contextClass = contextClass,
        displayName = displayName,
        description = description,
        parentMenu = parentMenu,
        row = row,
        order = order,
    ),
    CustomBehavior {
    override val keepOriginalMessage: Boolean
        get() = true

    abstract fun toggle(
        context: BotActionContext,
        toggleId: String,
    ): Set<ToggleMenuItem>

    abstract fun save(context: BotActionContext): Set<ToggleMenuItem>

    abstract fun discard(context: BotActionContext): Set<ToggleMenuItem>

    open fun hasChanges(context: BotActionContext): Boolean = false

    fun getSaveButton(context: BotActionContext): MenuButton =
        MenuButton(
            text = "common.save",
            callbackData = "$name $ACTION_SAVE",
        )

    fun getDiscardButton(context: BotActionContext): MenuButton =
        MenuButton(
            text = "common.backNoSave",
            callbackData = "$name $ACTION_DISCARD",
        )

    override fun getMenuItems(): Set<MenuItem> {
        TODO("Not yet implemented. Not supposed to be called")
    }

    override fun callAction(
        context: BotActionContext,
        action: String,
        arguments: List<String>,
    ): Set<MenuItem> {
        TODO("Not yet implemented")
    }
}

abstract class MenuItem(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    accessType: AccessType = AccessType.USER,
    contextClass: KClass<out BotActionContext>,
    displayName: String = "$name.command",
    description: String = displayName,
    var parentMenu: String = "start",
    var row: Long = 0,
    var order: Long = 0,
) : AccessibleCommand(
        name = name,
        visibility = visibility,
        accessType = accessType,
        contextClass = contextClass,
        displayName = displayName,
        description = description,
    ) {
    abstract fun getMarkup(context: BotActionContext): MenuButton
}

class ToggleMenuItem(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    accessType: AccessType = AccessType.USER,
    contextClass: KClass<out BotActionContext> = BotActionContext::class,
    displayName: String,
    description: String = displayName,
    parentMenu: String = "start",
    row: Long = 0,
    order: Long = 0,
    val toggleId: String,
    val value: Boolean,
) : MenuItem(
        name = name,
        visibility = visibility,
        accessType = accessType,
        contextClass = contextClass,
        displayName = displayName,
        description = description,
        parentMenu = parentMenu,
        row = row,
        order = order,
    ) {
    override fun getMarkup(context: BotActionContext): MenuButton {
        val labelArg = "$name.$toggleId.label"
        val enabledArg = "$name.$toggleId.enabled"
        return MenuButton(
            text = "{{$enabledArg}} {{$labelArg}}",
            callbackData = "/$name $ACTION_TOGGLE $toggleId",
            buttonArgs = mutableMapOf(labelArg to displayName, enabledArg to (if (value) "✅" else "❌")),
        )
    }

    override fun callCommand(
        context: BotActionContext,
        arguments: List<String>,
    ): Unit = TODO("Not implemented")
}

object Actions {
    const val ACTION_SAVE = "save"
    const val ACTION_DISCARD = "discard"
    const val ACTION_TOGGLE = "toggle"
}

@Component
class MenuProcessor(
    val allCommands: Set<Command>,
) {
    val allMenuItems: Set<MenuItem> =
        allCommands
            .filter { it is MenuItem }
            .map { it as MenuItem }
            .toSet()

    fun processIncomingMessage(
        message: IncomingMessage,
        context: BotActionContext,
    ) {
        when (message) {
            is CalledCommand -> processCalledCommand(context = context, calledCommand = message)
            is IncomingText -> processIncomingText(context = context, message = message)
        }
    }

    private fun processCalledCommand(
        context: BotActionContext,
        calledCommand: CalledCommand,
    ) {
        val command = allCommands.first { it.name == calledCommand.command }
        when (command) {
            is Menu -> processMenu(context, calledCommand, command)
            else -> command.callCommand(context, calledCommand.allArguments)
        }
    }

    private fun processIncomingText(
        context: BotActionContext,
        message: IncomingText,
    ) {
        TODO("Not yet implemented")
    }

    private fun processMenu(
        context: BotActionContext,
        calledCommand: CalledCommand,
        menu: Menu,
    ) {
        val menuItems =
            if (calledCommand is CalledAction && calledCommand.valid) {
                when {
                    menu is ToggleMenu && calledCommand.isSave -> menu.save(context)
                    menu is ToggleMenu && calledCommand.isDiscard -> menu.discard(context)
                    menu is ToggleMenu && calledCommand.isToggle ->
                        menu.toggle(context, requireNotNull(calledCommand.toggleId) { "Toggle id is required" })
                    else ->
                        menu.callAction(
                            context,
                            requireNotNull(calledCommand.action) { "Menu action is required" },
                            calledCommand.actionArguments,
                        )
                }
            } else {
                menu.getMenuItems()
            }

        val effectiveMenuItems =
            menuItems +
                allMenuItems
                    .filter { it.parentMenu == menu.name }
                    .toSet()

        val menuMarkup = MenuMarkup()
        effectiveMenuItems
            .groupBy { it.row }
            .toSortedMap()
            .forEach { (_, items) ->
                val row = menuMarkup.addRow()
                items
                    .sortedBy { it.order }
                    .map { it.getMarkup(context) }
                    .forEach { row.addButton(it) }
            }
        if (menu is ToggleMenu && menu.hasChanges(context)) {
            menuMarkup.addRow().addButton(menu.getSaveButton(context))
            menuMarkup.addRow().addButton(menu.getDiscardButton(context))
        }
        if (menu.addBackButton()) {
            allMenuItems.first { it.name == menu.parentMenu }.let {
                menuMarkup.addRow().addButton(it.getMarkup(context))
            }
        }
    }
}

abstract class IncomingMessage(
    val update: BotUpdate,
)

open class IncomingText(
    update: BotUpdate,
    val text: String,
) : IncomingMessage(update)

open class CalledCommand(
    update: BotUpdate,
    val command: String,
    val allArguments: List<String>,
) : IncomingMessage(update)

class CalledAction(
    update: BotUpdate,
    command: String,
    allArguments: List<String>,
    val action: String? = allArguments.firstOrNull(),
) : CalledCommand(update, command, allArguments) {
    val valid = action != null
    val actionArguments = allArguments.toMutableList().drop(2).toList()

    val isDiscard = action == ACTION_DISCARD
    val isSave = action == ACTION_SAVE
    val isToggle = action == ACTION_TOGGLE && !actionArguments.isEmpty()
    val toggleId = argumentAt(0)

    fun argumentAt(index: Int): String? =
        if (actionArguments.size > index) {
            actionArguments[index]
        } else {
            null
        }
}
