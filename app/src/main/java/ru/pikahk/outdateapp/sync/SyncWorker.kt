package ru.pikahk.outdateapp.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.pikahk.outdateapp.data.SyncProvider
import ru.pikahk.outdateapp.data.remote.ApiException

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        SyncProvider.get(applicationContext).sync()
        Result.success()
    } catch (e: ApiException) {
        if (e.code == ApiException.NETWORK || e.code == ApiException.SERVER) Result.retry() else Result.failure()
    }

    companion object {
        private const val WORK_NAME = "sync"

        fun request(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
