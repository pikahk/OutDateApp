package ru.pikahk.outdate.server.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `верный пароль проходит проверку`() {
        val hash = PasswordHasher.hash("correct horse")
        assertTrue(PasswordHasher.verify("correct horse", hash))
    }

    @Test
    fun `неверный пароль не проходит проверку`() {
        val hash = PasswordHasher.hash("correct horse")
        assertFalse(PasswordHasher.verify("wrong horse", hash))
    }

    @Test
    fun `один и тот же пароль даёт разные хеши`() {
        assertNotEquals(PasswordHasher.hash("same password"), PasswordHasher.hash("same password"))
    }

    @Test
    fun `испорченный хеш не проходит проверку`() {
        assertFalse(PasswordHasher.verify("password", "not a hash"))
        assertFalse(PasswordHasher.verify("password", "\$argon2id\$v=19\$m=x\$salt\$hash"))
    }
}
