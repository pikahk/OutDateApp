package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.Session

interface SessionStore {
    val session: Flow<Session?>

    val syncVersion: Flow<Long>

    suspend fun save(session: Session?)

    suspend fun saveSyncVersion(version: Long)

    val family: Flow<Family?>

    val familyVersion: Flow<Long>

    suspend fun saveFamily(family: Family?)

    suspend fun saveFamilyVersion(version: Long)
}
