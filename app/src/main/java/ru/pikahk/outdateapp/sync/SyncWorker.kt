package ru.pikahk.outdateapp.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
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
        private const val PERIODIC_WORK_NAME = "sync_periodic"
        private val PERIODIC_INTERVAL: Duration = Duration.ofHours(3)

        fun request(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(online())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL)
                .setConstraints(online())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun online() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}
