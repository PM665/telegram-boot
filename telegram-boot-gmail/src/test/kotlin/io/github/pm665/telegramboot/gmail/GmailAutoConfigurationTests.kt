package io.github.pm665.telegramboot.gmail

import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.io.InputStream
import java.util.Properties

class GmailAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GmailAutoConfiguration::class.java))

    @Test
    fun `uses no-op sender without Gmail credentials`() {
        contextRunner.run { context ->
            assertThat(context.getBean(EmailSender::class.java)).isInstanceOf(NoOpEmailSender::class.java)
        }
    }

    @Test
    fun `creates Gmail sender when credentials configured`() {
        contextRunner
            .withPropertyValues(
                "telegram-boot.gmail.sender=sender@gmail.com",
                "telegram-boot.gmail.app-password=test-app-password",
            ).run { context ->
                assertThat(context.getBean(EmailSender::class.java)).isInstanceOf(GmailEmailSender::class.java)
            }
    }

    @Test
    fun `sends rendered HTML through Gmail sender`() {
        val mailSender = RecordingMailSender()

        GmailEmailSender(mailSender, "sender@gmail.com").send(
            recipient = "recipient@example.com",
            subject = "Welcome",
            htmlTemplate = "<h1>Hello {{name}}</h1>",
            model = mapOf("name" to "Pavel"),
        )

        mailSender.sent.single().also { message ->
            assertThat((message.allRecipients.single() as InternetAddress).address).isEqualTo("recipient@example.com")
            assertThat(message.subject).isEqualTo("Welcome")
            assertThat((message.from.single() as InternetAddress).address).isEqualTo("sender@gmail.com")
            assertThat(message.content).isEqualTo("<h1>Hello Pavel</h1>")
        }
    }

    private class RecordingMailSender : JavaMailSender {
        val sent = mutableListOf<MimeMessage>()

        override fun createMimeMessage(): MimeMessage = MimeMessage(Session.getInstance(Properties()))

        override fun createMimeMessage(contentStream: InputStream): MimeMessage = MimeMessage(null, contentStream)

        override fun send(mimeMessage: MimeMessage) {
            sent += mimeMessage
        }

        override fun send(vararg mimeMessages: MimeMessage) {
            sent += mimeMessages
        }

        override fun send(simpleMessage: SimpleMailMessage) = Unit

        override fun send(vararg simpleMessages: SimpleMailMessage) = Unit
    }
}
