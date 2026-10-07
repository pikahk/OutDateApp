package ru.pikahk.outdateapp.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
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
import ru.pikahk.outdateapp.data.repository.ItemRepository
import ru.pikahk.outdateapp.data.repository.SettingsRepository
import ru.pikahk.outdateapp.data.repository.SubscriptionRepository
import ru.pikahk.outdateapp.domain.itemsToRemind
import ru.pikahk.outdateapp.domain.upcomingCharges

class ExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = DatabaseProvider.get(applicationContext)
        val today = LocalDate.now()
        val items = itemsToRemind(ItemRepository(database.itemDao()).observeAll().first(), today)
        val charges = upcomingCharges(SubscriptionRepository(database.subscriptionDao()).observeAll().first(), today)
        NotificationManagerCompat.from(applicationContext).cancelAll()
        ExpiryNotification.show(applicationContext, items, today)
        ChargeNotification.show(applicationContext, charges, today)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "expiry_reminders"

        suspend fun schedule(context: Context) {
            val time = SettingsRepository(context).settings.first().reminderTime
            enqueue(context, time, ExistingPeriodicWorkPolicy.KEEP)
        }

        fun reschedule(context: Context, time: LocalTime) {
            enqueue(context, time, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ExpiryWorker>().build())
        }

        private fun enqueue(context: Context, time: LocalTime, policy: ExistingPeriodicWorkPolicy) {
            val request = PeriodicWorkRequestBuilder<ExpiryWorker>(Duration.ofDays(1))
                .setInitialDelay(delayUntil(time))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        private fun delayUntil(time: LocalTime): Duration {
            val now = LocalDateTime.now()
            val todayAt = now.toLocalDate().atTime(time)
            val next = if (now.isBefore(todayAt)) todayAt else todayAt.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
