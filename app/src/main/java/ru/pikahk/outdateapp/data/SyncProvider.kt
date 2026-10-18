package ru.pikahk.outdateapp.data

import android.content.Context
import io.ktor.client.engine.okhttp.OkHttp
import ru.pikahk.outdateapp.BuildConfig
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.data.repository.DataStoreSessionStore
import ru.pikahk.outdateapp.data.repository.SyncRepository

object SyncProvider {

    @Volatile
    private var instance: SyncRepository? = null

    fun get(context: Context): SyncRepository = instance ?: synchronized(this) {
        instance ?: build(context).also { instance = it }
    }

    private fun build(context: Context) = SyncRepository(
        api = OutDateApi(BuildConfig.API_URL, OkHttp.create()),
        store = DataStoreSessionStore(context),
        dao = DatabaseProvider.get(context).syncDao()
    )
}
