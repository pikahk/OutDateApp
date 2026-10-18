package ru.pikahk.outdate.server.mail

data class EmailMessage(val subject: String, val text: String)

interface Mailer {
    fun send(to: String, message: EmailMessage)
}
