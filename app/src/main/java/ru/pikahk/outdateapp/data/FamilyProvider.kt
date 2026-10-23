package ru.pikahk.outdateapp.data

import android.content.Context
import io.ktor.client.engine.okhttp.OkHttp
import ru.pikahk.outdateapp.BuildConfig
import ru.pikahk.outdateapp.data.remote.OutDateApi
import ru.pikahk.outdateapp.data.repository.DataStoreSessionStore
import ru.pikahk.outdateapp.data.repository.FamilyRepository

object FamilyProvider {

    @Volatile
    private var instance: FamilyRepository? = null

    fun get(context: Context): FamilyRepository = instance ?: synchronized(this) {
        instance ?: build(context).also { instance = it }
    }

    private fun build(context: Context) = FamilyRepository(
        api = OutDateApi(BuildConfig.API_URL, OkHttp.create()),
        store = DataStoreSessionStore(context),
        sync = SyncProvider.get(context)
    )
}
