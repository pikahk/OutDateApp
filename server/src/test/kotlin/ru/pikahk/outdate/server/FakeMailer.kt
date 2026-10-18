package ru.pikahk.outdate.server

import java.util.concurrent.CopyOnWriteArrayList
import ru.pikahk.outdate.server.mail.EmailMessage
import ru.pikahk.outdate.server.mail.Mailer

class FakeMailer : Mailer {

    private val sent = CopyOnWriteArrayList<Pair<String, EmailMessage>>()

    @Volatile
    var failing = false

    override fun send(to: String, message: EmailMessage) {
        check(!failing) { "Mail server is unavailable" }
        sent += to to message
    }

    fun messagesTo(email: String): List<EmailMessage> = sent.filter { it.first == email }.map { it.second }

    fun lastCode(email: String): String = checkNotNull(Regex("""\d{6}""").find(messagesTo(email).last().text)).value
}
