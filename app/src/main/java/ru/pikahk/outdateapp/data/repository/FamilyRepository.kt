package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.remote.InviteResponse
import ru.pikahk.outdateapp.data.remote.OutDateApi

class FamilyRepository(private val api: OutDateApi, private val store: SessionStore, private val sync: SyncRepository) {

    val family: Flow<Family?> = sync.family

    suspend fun create(name: String, memberName: String) {
        sync.updateFamily(call { api.createFamily(it, name.trim(), memberName.trim()) })
    }

    suspend fun join(code: String, memberName: String) {
        sync.updateFamily(call { api.joinFamily(it, code, memberName.trim()) })
    }

    suspend fun invite(): InviteResponse = call { api.invite(it) }

    suspend fun removeMember(memberId: String) {
        call { api.removeMember(it, memberId) }
        refresh()
    }

    suspend fun leave() {
        call { api.leaveFamily(it) }
        sync.updateFamily(null)
    }

    suspend fun delete() {
        call { api.deleteFamily(it) }
        sync.updateFamily(null)
    }

    suspend fun refresh() {
        sync.updateFamily(call { api.family(it) })
    }

    private suspend fun <T> call(request: suspend (token: String) -> T): T {
        val session = store.session.first() ?: throw ApiException(ApiException.UNAUTHORIZED)
        return try {
            request(session.token)
        } catch (e: ApiException) {
            when (e.code) {
                ApiException.UNAUTHORIZED -> store.save(null)
                ApiException.NO_FAMILY -> sync.updateFamily(null)
            }
            throw e
        }
    }
}
