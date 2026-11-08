package io.github.pm665.telegramboot.admin.web.page

interface AdminUIPageHolder {
    /**
     * Human readable title that will be shown inside the tab switcher.
     */
    fun title(): String

    /**
     * The order of the tab on the UI. Lower numbers appear first.
     */
    fun order(): Int

    /**
     * Thymeleaf template (optionally with fragment selector) to be rendered for this page.
     */
    fun template(): String

    /**
     * Generates a URL-friendly slug for addressing the page.
     */
    fun slug(): String =
        title()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty {
                template()
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .ifEmpty { "service-page" }
            }
}

/**
 * Represents a service-level admin page not tied to a specific bot.
 */
interface ServiceAdminPage : AdminUIPageHolder

/**
 * Represents a page that allows creation of a new bot instance.
 */
interface NewBotPage : AdminUIPageHolder {
    override fun title(): String = "adminUi.newBotPage.title"

    override fun order(): Int = 0

    override fun template(): String = "fragments/new-bot/dummy-new-bot"
}

/**
 * Represents a tab that can be shown on the bot details page.
 */
interface BotAdminPage : AdminUIPageHolder
