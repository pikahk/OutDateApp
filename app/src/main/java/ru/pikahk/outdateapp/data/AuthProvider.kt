package ru.pikahk.outdateapp.data

import android.content.Context
import io.ktor.client.engine.okhttp.OkHttp
import ru.pikahk.outdateapp.BuildConfig
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.data.repository.AuthRepository
import ru.pikahk.outdateapp.data.repository.DataStoreSessionStore

object AuthProvider {

    @Volatile
    private var instance: AuthRepository? = null

    fun get(context: Context): AuthRepository = instance ?: synchronized(this) {
        instance ?: build(context).also { instance = it }
    }

    private fun build(context: Context) = AuthRepository(
        api = OutDateApi(BuildConfig.API_URL, OkHttp.create()),
        store = DataStoreSessionStore(context)
    )
}
