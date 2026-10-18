package ru.pikahk.outdate.server.auth

import ru.pikahk.outdate.server.mail.EmailMessage

fun codeEmail(purpose: CodePurpose, code: String): EmailMessage {
    val lifetime = "Код действует ${CODE_LIFETIME.toMinutes()} минут."
    return when (purpose) {
        CodePurpose.VERIFY_EMAIL -> EmailMessage(
            subject = "Подтверждение почты в outDate",
            text = lines(
                "Код подтверждения: $code",
                lifetime,
                "",
                "Если вы не регистрировались в outDate, просто проигнорируйте это письмо."
            )
        )
        CodePurpose.RESET_PASSWORD -> EmailMessage(
            subject = "Сброс пароля в outDate",
            text = lines(
                "Код для сброса пароля: $code",
                lifetime,
                "",
                "Если вы не запрашивали сброс пароля, просто проигнорируйте это письмо."
            )
        )
    }
}

private fun lines(vararg lines: String) = lines.joinToString("\n")
