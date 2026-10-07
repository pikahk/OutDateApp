package ru.pikahk.outdateapp.ui.subscriptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.data.model.Subscription
import ru.pikahk.outdateapp.domain.formatAmount
import ru.pikahk.outdateapp.domain.nextChargeDate

data class SubscriptionUi(
    val id: String,
    val name: String,
    val priceMinor: Long,
    val periodType: PeriodType,
    val periodDays: Int?,
    val daysUntilCharge: Long,
    val isDueSoon: Boolean,
    val isCancelled: Boolean
)

data class SubscriptionsUiState(
    val active: List<SubscriptionUi>,
    val cancelled: List<SubscriptionUi>,
    val monthlyTotalMinor: Long
)

fun Subscription.toUi(today: LocalDate): SubscriptionUi {
    val daysUntil = ChronoUnit.DAYS.between(today, nextChargeDate(this, today))
    return SubscriptionUi(
        id = id,
        name = name,
        priceMinor = priceMinor,
        periodType = periodType,
        periodDays = periodDays,
        daysUntilCharge = daysUntil,
        isDueSoon = daysUntil in 0..notifyDaysBefore,
        isCancelled = isCancelled
    )
}

@Composable
fun priceText(minor: Long, roundToMajor: Boolean = false): String {
    val locale = LocalConfiguration.current.locales[0]
    val amount = remember(minor, locale, roundToMajor) { formatAmount(minor, locale, roundToMajor) }
    return stringResource(R.string.price, amount)
}

@Composable
fun periodPriceText(subscription: SubscriptionUi): String {
    val price = priceText(subscription.priceMinor)
    return when (subscription.periodType) {
        PeriodType.MONTHLY -> stringResource(R.string.period_month, price)

        PeriodType.YEARLY -> stringResource(R.string.period_year, price)

        PeriodType.CUSTOM_DAYS -> {
            val days = subscription.periodDays ?: 1
            pluralStringResource(R.plurals.period_days, days, price, days)
        }
    }
}
