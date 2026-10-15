package ru.pikahk.outdate.server.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

object PasswordHasher {

    private const val MEMORY_KB = 19_456
    private const val ITERATIONS = 2
    private const val PARALLELISM = 1
    private const val SALT_BYTES = 16
    private const val HASH_BYTES = 32
    private const val PARTS = 6

    private val random = SecureRandom()
    private val encoder = Base64.getEncoder().withoutPadding()
    private val decoder = Base64.getDecoder()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = argon2(password, salt, MEMORY_KB, ITERATIONS, PARALLELISM, HASH_BYTES)
        val params = "m=$MEMORY_KB,t=$ITERATIONS,p=$PARALLELISM"
        return "\$argon2id\$v=19\$$params\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(hash)}"
    }

    fun verify(password: String, stored: String): Boolean = runCatching {
        val parts = stored.split('$')
        if (parts.size != PARTS || parts[1] != "argon2id") return false
        val params = parts[3].split(',').associate { it.substringBefore('=') to it.substringAfter('=').toInt() }
        val salt = decoder.decode(parts[4])
        val expected = decoder.decode(parts[5])
        val actual = argon2(
            password = password,
            salt = salt,
            memoryKb = params.getValue("m"),
            iterations = params.getValue("t"),
            parallelism = params.getValue("p"),
            length = expected.size
        )
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)

    private fun argon2(
        password: String,
        salt: ByteArray,
        memoryKb: Int,
        iterations: Int,
        parallelism: Int,
        length: Int
    ): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(memoryKb)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()
        val result = ByteArray(length)
        Argon2BytesGenerator().apply { init(parameters) }.generateBytes(password.toCharArray(), result)
        return result
    }
}
