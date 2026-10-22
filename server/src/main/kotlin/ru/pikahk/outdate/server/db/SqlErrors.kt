package ru.pikahk.outdate.server.db

import java.sql.SQLException

private const val UNIQUE_VIOLATION = "23505"

fun Throwable.isUniqueViolation(): Boolean =
    generateSequence(this) { it.cause }.any { it is SQLException && it.sqlState == UNIQUE_VIOLATION }
