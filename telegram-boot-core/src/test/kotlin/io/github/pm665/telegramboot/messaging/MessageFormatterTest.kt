package io.github.pm665.telegramboot.messaging

import io.github.pm665.telegramboot.features.messaging.MessageFormatter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Map

class MessageFormatterTest {
    @Test
    fun test() {
        val formatter = MessageFormatter()
        val result =
            formatter.formatMessage(
                TEMPLATE,
                Map.of<String, String?>(
                    "order",
                    "1234567890",
                    "price",
                    "1000",
                    "isCorp",
                    "yes",
                    "points",
                    "Точка 1Точка 2Точка 3",
                ),
            )
        Assertions.assertEquals(RESULT1, result)
    }

    @Test
    fun test2() {
        val formatter = MessageFormatter()
        val result =
            formatter.formatMessage(
                TEMPLATE,
                Map.of<String, String?>(
                    "order",
                    "1234567890",
                    "price",
                    "1000",
                    "points",
                    "Точка 1Точка 2Точка 3",
                ),
            )
        Assertions.assertEquals(RESULT2, result)
    }

    @Test
    fun test3() {
        val formatter = MessageFormatter()
        val template =
            """
            {{#health}}
            name: {{name}}, value: {{value}}
            {{/health}}
            
            """.trimIndent()
        val actual =
            formatter.formatMessage(
                template,
                Map.of<String, MutableList<Arg?>?>(
                    "health",
                    mutableListOf<Arg?>(Arg("health1", 100500L), Arg("health2", 500100L)),
                ),
            )
        val expected =
            """
            name: health1, value: 100500
            name: health2, value: 500100
            
            """.trimIndent()
        Assertions.assertEquals(expected, actual)
    }

    internal data class Arg(
        val name: String?,
        val value: Long,
    )

    companion object {
        val TEMPLATE: String =
            """
            *Текущий заказ*: {{order}}
            *Предварительная стоимость поездки*: {{price}}
            {{#isCorp}}
            *ЗАКАЗ КОРПОРАТИВНЫЙ!*
            Для водителя цена может быть меньше, чем платит компания Яндексу!
            {{/isCorp}}
            *Точки маршрута*:
            {{points}}
            
            """.trimIndent()
        val RESULT1: String =
            """
            *Текущий заказ*: 1234567890
            *Предварительная стоимость поездки*: 1000
            *ЗАКАЗ КОРПОРАТИВНЫЙ!*
            Для водителя цена может быть меньше, чем платит компания Яндексу!
            *Точки маршрута*:
            Точка 1Точка 2Точка 3
            
            """.trimIndent()
        val RESULT2: String =
            """
            *Текущий заказ*: 1234567890
            *Предварительная стоимость поездки*: 1000
            *Точки маршрута*:
            Точка 1Точка 2Точка 3
            
            """.trimIndent()
    }
}
