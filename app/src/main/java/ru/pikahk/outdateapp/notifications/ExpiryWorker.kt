package ru.pikahk.outdateapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.ItemRepository
import ru.pikahk.outdateapp.domain.itemsToRemind

class ExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = DatabaseProvider.get(applicationContext)
        val today = LocalDate.now()
        val items = itemsToRemind(ItemRepository(database.itemDao()).observeAll().first(), today)
        if (items.isNotEmpty()) {
            ExpiryNotification.show(applicationContext, items, today)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "expiry_reminders"
        private val REMIND_AT: LocalTime = LocalTime.of(9, 0)

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryWorker>(Duration.ofDays(1))
                .setInitialDelay(delayUntil(REMIND_AT))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ExpiryWorker>().build())
        }

        private fun delayUntil(time: LocalTime): Duration {
            val now = LocalDateTime.now()
            val todayAt = now.toLocalDate().atTime(time)
            val next = if (now.isBefore(todayAt)) todayAt else todayAt.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
