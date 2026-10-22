package ru.pikahk.outdate.server.sync

import io.ktor.http.HttpStatusCode
import java.util.UUID
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import ru.pikahk.outdate.server.db.SyncedTable
import ru.pikahk.outdate.server.plugins.ApiException

private const val MAX_RECORDS = 1000
private const val MAX_KEY_LENGTH = 64

fun checkRecords(vararg lists: List<SyncRecord>) {
    if (lists.any { it.size > MAX_RECORDS }) throw ApiException(HttpStatusCode.BadRequest, "too_many_changes")
    if (lists.flatMap { it }.any { it.key.isBlank() || it.key.length > MAX_KEY_LENGTH }) {
        throw ApiException(HttpStatusCode.BadRequest, "bad_request")
    }
}

fun <T : SyncRecord> SyncedTable.save(
    ownerId: UUID,
    newVersion: Long,
    records: List<T>,
    keepOnUpdate: List<Column<*>> = emptyList(),
    fill: UpdateBuilder<*>.(T) -> Unit
): Int {
    if (records.isEmpty()) return 0
    val latest = records.sortedBy { it.updatedAt }.associateBy { it.key }
    val stored = select(key, updatedAt)
        .where { (owner eq ownerId) and (key inList latest.keys) }
        .associate { it[key] to it[updatedAt] }
    val (newer, outdated) = latest.values.partition { record ->
        stored[record.key]?.let { record.updatedAt > it } ?: true
    }
    batchUpsert(newer, onUpdateExclude = keepOnUpdate.ifEmpty { null }) { record ->
        this[owner] = ownerId
        this[key] = record.key
        this[updatedAt] = record.updatedAt
        this[version] = newVersion
        fill(record)
    }
    if (outdated.isNotEmpty()) {
        update({ (owner eq ownerId) and (key inList outdated.map { it.key }) }) { it[version] = newVersion }
    }
    return latest.size
}

fun <T> SyncedTable.changedSince(ownerId: UUID, since: Long, map: (ResultRow) -> T): List<T> =
    selectAll().where { (owner eq ownerId) and (version greater since) }.map(map)
