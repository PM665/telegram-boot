package io.github.pm665.telegramboot.gmail

import com.samskivert.mustache.Mustache
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import java.nio.charset.StandardCharsets

@ConfigurationProperties("telegram-boot.gmail")
data class GmailProperties(
    val sender: String,
    val appPassword: String,
)

@AutoConfiguration
class GmailAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(EmailSender::class)
    fun noOpEmailSender(): EmailSender = NoOpEmailSender

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "telegram-boot.gmail", name = ["sender", "app-password"])
    @EnableConfigurationProperties(GmailProperties::class)
    class GmailConfiguration {
        @Bean
        @ConditionalOnMissingBean(EmailSender::class)
        fun gmailEmailSender(properties: GmailProperties): EmailSender = GmailEmailSender(gmailMailSender(properties), properties.sender)

        private fun gmailMailSender(properties: GmailProperties): JavaMailSender =
            JavaMailSenderImpl().apply {
                host = "smtp.gmail.com"
                port = 587
                username = properties.sender
                password = properties.appPassword
                javaMailProperties["mail.smtp.auth"] = "true"
                javaMailProperties["mail.smtp.starttls.enable"] = "true"
            }
        }
}

class GmailEmailSender(
    private val mailSender: JavaMailSender,
    private val sender: String,
) : EmailSender {
    override fun send(
        recipient: String,
        subject: String,
        htmlTemplate: String,
        model: Map<String, Any?>,
    ) {
        val message = mailSender.createMimeMessage()
        MimeMessageHelper(message, StandardCharsets.UTF_8.name()).apply {
            setFrom(sender)
            setTo(recipient)
            setSubject(subject)
            setText(Mustache.compiler().compile(htmlTemplate).execute(model), true)
        }
        mailSender.send(message)
    }
}

object NoOpEmailSender : EmailSender {
    override fun send(
        recipient: String,
        subject: String,
        htmlTemplate: String,
        model: Map<String, Any?>,
    ) = Unit
}
