package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pikahk.outdateapp.data.model.Session

interface SessionStore {
    val session: Flow<Session?>

    suspend fun save(session: Session?)
}
