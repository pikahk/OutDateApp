package ru.pikahk.outdateapp.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.Item
import ru.pikahk.outdateapp.domain.daysLeft

object ExpiryNotification {

    private const val CHANNEL_ID = "expiry"
    private const val NOTIFICATION_ID = 1

    fun show(context: Context, items: List<Item>, today: LocalDate) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permission != PackageManager.PERMISSION_GRANTED) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel))
                .build()
        )

        val style = NotificationCompat.InboxStyle()
        items.forEach { style.addLine(line(context, it, today)) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.resources.getQuantityString(R.plurals.notification_title, items.size, items.size))
            .setContentText(items.joinToString { it.name })
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun line(context: Context, item: Item, today: LocalDate): String {
        val days = daysLeft(item, today).toInt()
        return when (days) {
            0 -> context.getString(R.string.notification_today, item.name)
            1 -> context.getString(R.string.notification_tomorrow, item.name)
            else -> context.resources.getQuantityString(R.plurals.notification_in_days, days, item.name, days)
        }
    }

    private fun openAppIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
