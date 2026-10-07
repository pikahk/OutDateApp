package ru.pikahk.outdateapp.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.time.LocalDate
import ru.pikahk.outdateapp.MainActivity
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.domain.daysLeft
import ru.pikahk.outdateapp.ui.ITEM_DEEP_LINK

object ExpiryNotification {

    private const val CHANNEL_ID = "expiry"
    private const val GROUP_KEY = "expiry_items"
    private const val SUMMARY_ID = 0

    fun show(context: Context, items: List<Item>, today: LocalDate) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permission != PackageManager.PERMISSION_GRANTED) return
        if (items.isEmpty()) return

        val manager = NotificationManagerCompat.from(context)

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel))
                .build()
        )

        for (item in items) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(item.name)
                .setContentText(daysText(context, daysLeft(item, today).toInt()))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setGroup(GROUP_KEY)
                .setContentIntent(openItemIntent(context, item.id))
                .setAutoCancel(true)
                .build()
            manager.notify(item.id.hashCode(), notification)
        }

        if (items.size > 1) {
            val style = NotificationCompat.InboxStyle()
            items.forEach { style.addLine(line(context, it, today)) }
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(
                    context.resources.getQuantityString(R.plurals.notification_title, items.size, items.size)
                )
                .setContentText(items.joinToString { it.name })
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .build()
            manager.notify(SUMMARY_ID, summary)
        }
    }

    private fun daysText(context: Context, days: Int): String = when (days) {
        0 -> context.getString(R.string.notification_expires_today)
        1 -> context.getString(R.string.notification_expires_tomorrow)
        else -> context.resources.getQuantityString(R.plurals.notification_expires_in, days, days)
    }

    private fun line(context: Context, item: Item, today: LocalDate): String {
        val days = daysLeft(item, today).toInt()
        return when (days) {
            0 -> context.getString(R.string.notification_today, item.name)
            1 -> context.getString(R.string.notification_tomorrow, item.name)
            else -> context.resources.getQuantityString(R.plurals.notification_in_days, days, item.name, days)
        }
    }

    private fun openItemIntent(context: Context, itemId: String): PendingIntent? {
        val intent = Intent(Intent.ACTION_VIEW, "$ITEM_DEEP_LINK/$itemId".toUri(), context, MainActivity::class.java)
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(itemId.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openAppIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
