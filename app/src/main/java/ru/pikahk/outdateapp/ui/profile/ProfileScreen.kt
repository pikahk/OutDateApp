package ru.pikahk.outdateapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.ItemStats
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.LinkRow
import ru.pikahk.outdateapp.ui.SectionTitle
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onSettingsClick: () -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    ProfileScreen(stats = stats, month = viewModel.month, onBack = onBack, onSettingsClick = onSettingsClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(stats: ItemStats?, month: YearMonth, onBack: () -> Unit, onSettingsClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            AccountRow()
            if (stats != null) {
                SectionTitle(stringResource(R.string.stats_now))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        value = stats.tracked,
                        label = stringResource(R.string.stats_tracked),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = stats.expiringThisWeek,
                        label = stringResource(R.string.stats_this_week),
                        dot = Urgency.SOON.color(),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = stats.expired,
                        label = stringResource(R.string.stats_expired),
                        dot = Urgency.EXPIRED.color(),
                        modifier = Modifier.weight(1f)
                    )
                }
                SectionTitle(rememberMonthName(month))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        value = stats.usedThisMonth,
                        label = stringResource(R.string.stats_used),
                        dot = Urgency.OK.color(),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        value = stats.wastedThisMonth,
                        label = stringResource(R.string.stats_wasted),
                        dot = Urgency.EXPIRED.color(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            LinkRow(
                title = stringResource(R.string.settings_title),
                onClick = onSettingsClick,
                modifier = Modifier.padding(top = 28.dp)
            )
        }
    }
}

@Composable
private fun AccountRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.profile_signed_out),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier = Modifier, dot: Color? = null) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (dot != null) {
                Box(modifier = Modifier.size(8.dp).background(dot, CircleShape))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rememberMonthName(month: YearMonth): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, month) { month.format(DateTimeFormatter.ofPattern("LLLL", locale)) }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    OutDateAppTheme {
        ProfileScreen(
            stats = ItemStats(
                tracked = 14,
                expiringThisWeek = 3,
                expired = 1,
                usedThisMonth = 9,
                wastedThisMonth = 2
            ),
            month = YearMonth.of(2026, 10),
            onBack = {},
            onSettingsClick = {}
        )
    }
}
