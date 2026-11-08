package io.github.pm665.telegramboot.domain.actions

open class SendMessageMarkup(
    open val text: String,
    open val photo: Photo? = null,
    open val replyToMessageId: String? = null,
    open val requestLocation: Boolean = false,
    open val menuMarkup: MenuMarkup? = null,
    open val replyKeyboard: List<String> = mutableListOf(),
) {
    val isRequestLocation: Boolean get() = requestLocation

    val isPhoto: Boolean get() = photo != null

    companion object {
        const val CALLBACK_YES: String = "cb_yes"
        const val CALLBACK_NO: String = "cb_no"

        val INLINE_KEYBOARD_YES_NO: MenuMarkup =
            MenuMarkup(
                rows =
                    mutableListOf(
                        MenuRow(
                            mutableListOf(
                                MenuButton("confirm.reply_yes", CALLBACK_YES),
                                MenuButton("confirm.reply_no", CALLBACK_NO),
                            ),
                        ),
                    ),
            )

        fun builder(): Builder = Builder()

        fun create(text: String): Builder = builder().text(text)
    }

    class Builder {
        private var text: String? = null
        private var photo: Photo? = null
        private var replyToMessageId: String? = null
        private var requestLocation: Boolean = false
        private var menuMarkup: MenuMarkup? = null
        private val replyKeyboard: MutableList<String> = mutableListOf()

        fun text(text: String?) = apply { this.text = text }

        fun photo(photo: Photo?) = apply { this.photo = photo }

        fun replyToMessageId(replyToMessageId: String?) = apply { this.replyToMessageId = replyToMessageId }

        fun requestLocation(requestLocation: Boolean) = apply { this.requestLocation = requestLocation }

        fun inlineKeyboardMarkup(menuMarkup: MenuMarkup?) =
            apply {
                this.menuMarkup = menuMarkup
            }

        fun build(): SendMessageMarkup =
            SendMessageMarkup(
                text = text ?: "",
                photo = photo,
                replyToMessageId = replyToMessageId,
                requestLocation = requestLocation,
                menuMarkup = menuMarkup,
            )
    }

    sealed interface Photo {
        data class Bytes(
            val filename: String,
            val contentType: String,
            val bytes: ByteArray,
        ) : Photo

        data class ProviderFileId(
            val value: String,
        ) : Photo
    }

    data class MenuMarkup(
        val title: String? = null,
        val rows: MutableList<MenuRow> = mutableListOf(),
        val maxButtonsPerRow: Int = 1,
        val rich: Boolean = false,
    ) {
        fun addRow(): MenuRow {
            val row = MenuRow()
            rows += row
            return row
        }

        fun addButtonToLastRow(
            text: String,
            callbackData: String? = null,
            url: String? = null,
        ): MenuMarkup = addButtonToLastRow(MenuButton(text, callbackData, url))

        fun addButtonToLastRow(button: MenuButton): MenuMarkup {
            val row =
                if (rows.isEmpty()) {
                    addRow()
                } else if (rows.last().buttons.size >= maxButtonsPerRow) {
                    addRow()
                } else {
                    rows.last()
                }
            row.buttons += button
            return this
        }

        companion object {
            fun builder(): Builder = Builder()
        }

        class Builder {
            private val rows: MutableList<MenuRow> = mutableListOf()

            fun row(row: MenuRow) = apply { rows += row }

            fun rows(rows: Collection<MenuRow>) = apply { this.rows += rows }

            fun build(): MenuMarkup = MenuMarkup(rows = rows.toMutableList())
        }
    }

    data class MenuRow(
        val buttons: MutableList<MenuButton> = mutableListOf(),
    ) {
        fun addButton(button: MenuButton): MenuRow {
            buttons += button
            return this
        }

        fun addButton(
            text: String,
            callbackData: String? = null,
            url: String? = null,
        ): MenuRow = addButton(MenuButton(text, callbackData, url))

        companion object {
            fun builder(): KeyboardRowBuilder = KeyboardRowBuilder()
        }

        class KeyboardRowBuilder {
            private val buttons: MutableList<MenuButton> = mutableListOf()

            fun button(button: MenuButton) = apply { buttons += button }

            fun buttons(buttons: Collection<MenuButton>) = apply { this.buttons += buttons }

            fun build(): MenuRow = MenuRow(buttons.toMutableList())
        }
    }

    data class MenuButton(
        val text: String,
        val callbackData: String? = null,
        val url: String? = null,
        val buttonArgs: Map<String, Any> = mutableMapOf(),
    ) {
        companion object {
            fun builder(): Builder = Builder()
        }

        class Builder {
            private var text: String? = null
            private var callbackData: String? = null
            private var url: String? = null

            fun text(text: String) = apply { this.text = text }

            fun callbackData(callbackData: String?) = apply { this.callbackData = callbackData }

            fun url(url: String?) = apply { this.url = url }

            fun build(): MenuButton = MenuButton(text ?: "undefined", callbackData, url)
        }
    }
}
