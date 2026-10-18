package ru.pikahk.outdate.server.mail

import io.ktor.server.config.ApplicationConfig
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

private const val TIMEOUT_MS = "10000"

enum class MailSecurity { NONE, STARTTLS, SSL }

data class MailSettings(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val security: MailSecurity,
    val from: String
)

fun ApplicationConfig.mailSettings() = MailSettings(
    host = property("mail.host").getString(),
    port = property("mail.port").getString().toInt(),
    user = property("mail.user").getString(),
    password = property("mail.password").getString(),
    security = MailSecurity.valueOf(property("mail.security").getString().uppercase()),
    from = property("mail.from").getString()
)

class SmtpMailer(private val settings: MailSettings) : Mailer {

    private val session: Session = Session.getInstance(properties(), authenticator())

    override fun send(to: String, message: EmailMessage) {
        val mime = MimeMessage(session).apply {
            setFrom(InternetAddress(settings.from))
            setRecipient(Message.RecipientType.TO, InternetAddress(to))
            setSubject(message.subject, Charsets.UTF_8.name())
            setText(message.text, Charsets.UTF_8.name())
        }
        Transport.send(mime)
    }

    private fun properties() = Properties().apply {
        setProperty("mail.smtp.host", settings.host)
        setProperty("mail.smtp.port", settings.port.toString())
        setProperty("mail.smtp.auth", settings.user.isNotEmpty().toString())
        setProperty("mail.smtp.connectiontimeout", TIMEOUT_MS)
        setProperty("mail.smtp.timeout", TIMEOUT_MS)
        setProperty("mail.smtp.writetimeout", TIMEOUT_MS)
        when (settings.security) {
            MailSecurity.NONE -> Unit
            MailSecurity.STARTTLS -> {
                setProperty("mail.smtp.starttls.enable", "true")
                setProperty("mail.smtp.starttls.required", "true")
                setProperty("mail.smtp.ssl.checkserveridentity", "true")
            }
            MailSecurity.SSL -> {
                setProperty("mail.smtp.ssl.enable", "true")
                setProperty("mail.smtp.ssl.checkserveridentity", "true")
            }
        }
    }

    private fun authenticator(): Authenticator? {
        if (settings.user.isEmpty()) return null
        return object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(settings.user, settings.password)
        }
    }
}
