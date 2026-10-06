package ru.pikahk.outdateapp.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.DefaultCategory
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@Composable
fun ItemsScreen(
    viewModel: ItemsViewModel,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ItemsScreen(
        state = state,
        query = viewModel.query,
        onAddClick = onAddClick,
        onItemClick = onItemClick,
        onProfileClick = onProfileClick,
        onCategorySelect = viewModel::selectCategory,
        onCategoryDelete = viewModel::deleteCategory,
        onSortSelect = viewModel::selectSort,
        onOpenedSelect = viewModel::selectOpened,
        onResetFilters = viewModel::resetFilters
    )
}

@Composable
private fun ItemsScreen(
    state: ItemsUiState,
    query: TextFieldState,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onCategorySelect: (String?) -> Unit,
    onCategoryDelete: (String) -> Unit,
    onSortSelect: (ItemsSort) -> Unit,
    onOpenedSelect: (OpenedFilter) -> Unit,
    onResetFilters: () -> Unit
) {
    Scaffold(
        topBar = {
            ItemsTopBar(
                query = query,
                sort = state.sort,
                opened = state.opened,
                onSortSelect = onSortSelect,
                onOpenedSelect = onOpenedSelect,
                onProfileClick = onProfileClick
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
            ) {
                CategoryFilter(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = onCategorySelect,
                    onDelete = onCategoryDelete
                )
                if (state.groups.isEmpty()) {
                    val message = when {
                        !state.hasItems -> R.string.items_empty
                        state.isSearching || state.opened != OpenedFilter.ALL -> R.string.search_empty
                        else -> R.string.items_empty_category
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.hasItems) {
                            TextButton(onClick = onResetFilters) {
                                Text(stringResource(R.string.reset_filters))
                            }
                        }
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
            query = rememberTextFieldState(),
            onAddClick = {},
            onItemClick = {},
            onProfileClick = {},
            onCategorySelect = {},
            onCategoryDelete = {},
            onSortSelect = {},
            onOpenedSelect = {},
            onResetFilters = {}
        )
    }
}
