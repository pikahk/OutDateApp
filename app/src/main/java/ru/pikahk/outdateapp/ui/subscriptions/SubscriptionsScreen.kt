package ru.pikahk.outdateapp.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.PeriodType
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.SectionTitle
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private const val CANCELLED_ALPHA = 0.55f

@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onAddClick: () -> Unit,
    onSubscriptionClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubscriptionsScreen(state = state, onAddClick = onAddClick, onSubscriptionClick = onSubscriptionClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionsScreen(
    state: SubscriptionsUiState?,
    onAddClick: () -> Unit,
    onSubscriptionClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.subscriptions_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_item)
                )
            }
        }
    ) { padding ->
        when {
            state == null -> Unit

            state.active.isEmpty() && state.cancelled.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.subscriptions_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (state.active.isNotEmpty()) {
                    item(key = "total") { MonthlyTotal(state.monthlyTotalMinor) }
                }
                items(items = state.active, key = { it.id }) { subscription ->
                    SubscriptionRow(subscription, onClick = { onSubscriptionClick(subscription.id) })
                }
                if (state.cancelled.isNotEmpty()) {
                    item(key = "cancelled") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle(stringResource(R.string.subscriptions_cancelled))
                        }
                    }
                    items(items = state.cancelled, key = { it.id }) { subscription ->
                        SubscriptionRow(subscription, onClick = { onSubscriptionClick(subscription.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(subscription: SubscriptionUi, onClick: () -> Unit) {
    val subtitle = if (subscription.isCancelled) {
        stringResource(R.string.subscription_cancelled, periodPriceText(subscription))
    } else {
        periodPriceText(subscription)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (subscription.isCancelled) CANCELLED_ALPHA else 1f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = subscription.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subscription.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!subscription.isCancelled) {
            ChargeBadge(daysUntil = subscription.daysUntilCharge, dueSoon = subscription.isDueSoon)
        }
    }
}

@Composable
private fun ChargeBadge(daysUntil: Long, dueSoon: Boolean) {
    val days = daysUntil.toInt()
    val text = when (days) {
        0 -> stringResource(R.string.badge_today)
        1 -> stringResource(R.string.badge_tomorrow)
        else -> pluralStringResource(R.plurals.days, days, days)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (dueSoon) Urgency.SOON.color() else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = if (dueSoon) Urgency.SOON.containerColor() else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun MonthlyTotal(totalMinor: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.subscriptions_monthly),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = priceText(totalMinor, roundToMajor = true),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionsScreenPreview() {
    OutDateAppTheme {
        SubscriptionsScreen(
            state = SubscriptionsUiState(
                active = listOf(
                    SubscriptionUi("1", "Яндекс Плюс", 39_900, PeriodType.MONTHLY, null, 1, true, false),
                    SubscriptionUi("2", "Спортзал", 350_000, PeriodType.CUSTOM_DAYS, 30, 12, false, false),
                    SubscriptionUi("3", "Telegram Premium", 299_000, PeriodType.YEARLY, null, 47, false, false)
                ),
                cancelled = listOf(
                    SubscriptionUi("4", "Литрес", 39_900, PeriodType.MONTHLY, null, 20, false, true)
                ),
                monthlyTotalMinor = 419_921
            ),
            onAddClick = {},
            onSubscriptionClick = {}
        )
    }
}
