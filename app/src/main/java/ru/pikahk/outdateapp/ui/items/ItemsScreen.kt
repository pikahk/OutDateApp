package ru.pikahk.outdateapp.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.DefaultCategory
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.iconRes
import ru.pikahk.outdateapp.ui.label
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@Composable
fun ItemsScreen(viewModel: ItemsViewModel, onAddClick: () -> Unit, onItemClick: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ItemsScreen(
        state = state,
        onAddClick = onAddClick,
        onItemClick = onItemClick,
        onCategorySelect = viewModel::selectCategory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemsScreen(
    state: ItemsUiState,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onCategorySelect: (String?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.items_title),
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
        if (!state.isLoading) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                CategoryFilter(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = onCategorySelect
                )
                if (state.groups.isEmpty()) {
                    val message = if (state.hasItems) R.string.items_empty_category else R.string.items_empty
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        state.groups.forEach { group ->
                            item(key = "section_${group.section.name}") {
                                SectionHeader(group.section)
                            }
                            items(items = group.items, key = { it.id }) { item ->
                                ItemRow(item = item, onClick = { onItemClick(item.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilter(categories: List<CategoryUi>, selectedId: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CategoryChip(
                text = stringResource(R.string.category_all),
                selected = selectedId == null,
                onClick = { onSelect(null) }
            )
        }
        items(items = categories, key = { it.id }) { category ->
            CategoryChip(
                text = category.label(),
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = content
        )
    }
}

@Composable
private fun SectionHeader(section: ItemsSection) {
    val title = when (section) {
        ItemsSection.EXPIRED -> stringResource(R.string.section_expired)
        ItemsSection.SOON -> stringResource(R.string.section_soon)
        ItemsSection.LATER -> stringResource(R.string.section_later)
    }
    val color = if (section == ItemsSection.EXPIRED) {
        Urgency.EXPIRED.color()
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ItemRow(item: ItemUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ItemBadge(item)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DaysBadge(daysLeft = item.daysLeft, urgency = item.urgency)
    }
}

@Composable
private fun ItemBadge(item: ItemUi) {
    val expired = item.urgency == Urgency.EXPIRED
    val content = if (expired) item.urgency.color() else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (expired) item.urgency.containerColor() else MaterialTheme.colorScheme.surfaceVariant
    val builtIn = item.category?.builtIn
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = background, shape = RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (builtIn != null) {
            Icon(
                painter = painterResource(builtIn.iconRes()),
                contentDescription = item.category?.label(),
                tint = content,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = item.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

@Composable
private fun DaysBadge(daysLeft: Long, urgency: Urgency) {
    Text(
        text = badgeText(daysLeft),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = urgency.color(),
        modifier = Modifier
            .background(color = urgency.containerColor(), shape = RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun subtitle(item: ItemUi): String {
    val openedAt = item.openedAt ?: return stringResource(R.string.list_sealed)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val date = openedAt.format(formatter)
    return if (item.limitedByOpening) {
        stringResource(R.string.list_opened_limited, date)
    } else {
        stringResource(R.string.list_opened, date)
    }
}

@Composable
private fun badgeText(daysLeft: Long): String {
    val days = abs(daysLeft).toInt()
    return when {
        daysLeft < 0 -> pluralStringResource(R.plurals.days_ago, days, days)
        daysLeft == 0L -> stringResource(R.string.badge_today)
        daysLeft == 1L -> stringResource(R.string.badge_tomorrow)
        else -> pluralStringResource(R.plurals.days, days, days)
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemsScreenPreview() {
    val today = LocalDate.of(2026, 9, 25)
    val food = CategoryUi("food", "food", DefaultCategory.FOOD)
    val medicine = CategoryUi("medicine", "medicine", DefaultCategory.MEDICINE)
    val cosmetics = CategoryUi("cosmetics", "cosmetics", DefaultCategory.COSMETICS)
    OutDateAppTheme {
        ItemsScreen(
            state = ItemsUiState(
                isLoading = false,
                categories = listOf(food, medicine, cosmetics),
                hasItems = true,
                groups = listOf(
                    ItemsGroup(
                        ItemsSection.EXPIRED,
                        listOf(ItemUi("1", "Сметана", -2, Urgency.EXPIRED, today.minusDays(5), true, food))
                    ),
                    ItemsGroup(
                        ItemsSection.SOON,
                        listOf(
                            ItemUi("2", "Молоко", 1, Urgency.CRITICAL, today.minusDays(1), true, food),
                            ItemUi("3", "Капли", 4, Urgency.SOON, null, false, medicine)
                        )
                    ),
                    ItemsGroup(
                        ItemsSection.LATER,
                        listOf(ItemUi("4", "Крем для рук", 63, Urgency.OK, null, false, cosmetics))
                    )
                )
            ),
            onAddClick = {},
            onItemClick = {},
            onCategorySelect = {}
        )
    }
}
