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
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.MainActivity
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.UpcomingCharge
import ru.pikahk.outdateapp.domain.formatAmount
import ru.pikahk.outdateapp.ui.SUBSCRIPTIONS_DEEP_LINK

object ChargeNotification {

    private const val CHANNEL_ID = "charges"
    private const val GROUP_KEY = "charges"
    private const val SUMMARY_ID = 0

    fun show(context: Context, charges: List<UpcomingCharge>, today: LocalDate) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permission != PackageManager.PERMISSION_GRANTED) return
        if (charges.isEmpty()) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.charge_channel))
                .build()
        )
        val openSubscriptions = openSubscriptionsIntent(context)

        for (charge in charges) {
            val text = context.getString(R.string.charge_text, whenText(context, charge, today), price(context, charge))
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_credit_card)
                .setContentTitle(charge.subscription.name)
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setGroup(GROUP_KEY)
                .setContentIntent(openSubscriptions)
                .setAutoCancel(true)
                .build()
            manager.notify(CHANNEL_ID, charge.subscription.id.hashCode(), notification)
        }

        if (charges.size > 1) {
            val style = NotificationCompat.InboxStyle()
            charges.forEach { style.addLine(line(context, it, today)) }
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_credit_card)
                .setContentTitle(context.getString(R.string.charge_summary, charges.size))
                .setContentText(charges.joinToString { it.subscription.name })
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setContentIntent(openSubscriptions)
                .setAutoCancel(true)
                .build()
            manager.notify(CHANNEL_ID, SUMMARY_ID, summary)
        }
    }

    private fun whenText(context: Context, charge: UpcomingCharge, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, charge.date).toInt()
        return when (days) {
            0 -> context.getString(R.string.charge_when_today)
            1 -> context.getString(R.string.charge_when_tomorrow)
            else -> context.resources.getQuantityString(R.plurals.charge_when_days, days, days)
        }
    }

    private fun price(context: Context, charge: UpcomingCharge): String {
        val locale = context.resources.configuration.locales[0]
        return context.getString(R.string.price, formatAmount(charge.subscription.priceMinor, locale))
    }

    private fun line(context: Context, charge: UpcomingCharge, today: LocalDate): String {
        val name = charge.subscription.name
        return context.getString(R.string.charge_line, name, whenText(context, charge, today), price(context, charge))
    }

    private fun openSubscriptionsIntent(context: Context): PendingIntent? {
        val intent = Intent(Intent.ACTION_VIEW, SUBSCRIPTIONS_DEEP_LINK.toUri(), context, MainActivity::class.java)
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
