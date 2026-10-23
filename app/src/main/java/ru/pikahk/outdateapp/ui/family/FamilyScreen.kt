package ru.pikahk.outdateapp.ui.family

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.DefaultCategory
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.FamilyMember
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.items.CategoryFilter
import ru.pikahk.outdateapp.ui.items.DeleteItemDialog
import ru.pikahk.outdateapp.ui.items.ItemRow
import ru.pikahk.outdateapp.ui.items.ItemUi
import ru.pikahk.outdateapp.ui.items.ItemsGroup
import ru.pikahk.outdateapp.ui.items.ItemsSection
import ru.pikahk.outdateapp.ui.items.SectionHeader
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    state: FamilyUiState,
    profileAction: @Composable () -> Unit,
    onSignIn: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onMembers: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onItemDelete: (String) -> Unit,
    onCategorySelect: (String?) -> Unit
) {
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val active = state as? FamilyUiState.Active

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = active?.family?.name ?: stringResource(R.string.tab_family),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (active != null) {
                        IconButton(onClick = onMembers) {
                            Icon(
                                painter = painterResource(R.drawable.ic_group),
                                contentDescription = stringResource(R.string.family_members)
                            )
                        }
                    }
                    profileAction()
                }
            )
        },
        floatingActionButton = {
            if (active?.hasItems == true) {
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
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (state) {
            FamilyUiState.Loading -> Unit
            FamilyUiState.SignedOut -> FamilyIntro(
                icon = R.drawable.ic_home,
                text = stringResource(R.string.family_signed_out_text),
                primary = stringResource(R.string.auth_sign_in),
                onPrimary = onSignIn,
                modifier = modifier
            )
            FamilyUiState.NoFamily -> FamilyIntro(
                icon = R.drawable.ic_group,
                text = stringResource(R.string.family_intro_text),
                primary = stringResource(R.string.family_create),
                onPrimary = onCreate,
                secondary = stringResource(R.string.family_join),
                onSecondary = onJoin,
                modifier = modifier
            )
            is FamilyUiState.Active -> Column(modifier = modifier) {
                CategoryFilter(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = onCategorySelect,
                    onDelete = {}
                )
                if (state.hasItems) {
                    FamilyItems(
                        groups = state.groups,
                        onItemClick = onItemClick,
                        onItemLongClick = { deletingId = it },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                } else {
                    FamilyEmpty(onAddClick = onAddClick, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }

    val deleting = active?.groups?.flatMap { it.items }?.firstOrNull { it.id == deletingId }
    if (deleting != null) {
        DeleteItemDialog(
            name = deleting.name,
            onConfirm = {
                onItemDelete(deleting.id)
                deletingId = null
            },
            onDismiss = { deletingId = null }
        )
    }
}

@Composable
private fun FamilyItems(
    groups: List<ItemsGroup>,
    onItemClick: (String) -> Unit,
    onItemLongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groups.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.items_empty_category),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 88.dp)) {
        groups.forEach { group ->
            item(key = "section_${group.section.name}") { SectionHeader(group.section) }
            items(items = group.items, key = { it.id }) { item ->
                ItemRow(
                    item = item,
                    onClick = { onItemClick(item.id) },
                    onLongClick = { onItemLongClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun FamilyEmpty(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.family_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.family_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onAddClick, shape = RoundedCornerShape(13.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.items_add_first))
        }
    }
}

@Composable
private fun FamilyIntro(
    @DrawableRes icon: Int,
    text: String,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onSecondary: () -> Unit = {}
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = stringResource(R.string.family_intro_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onPrimary,
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Text(primary)
        }
        if (secondary != null) {
            OutlinedButton(
                onClick = onSecondary,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(secondary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FamilyScreenPreview() {
    val today = LocalDate.of(2026, 10, 23)
    val food = CategoryUi("food", "food", DefaultCategory.FOOD)
    val medicine = CategoryUi("medicine", "medicine", DefaultCategory.MEDICINE)
    val members = listOf(FamilyMember("1", "Миша", isOwner = true, isMe = true, joinedAt = 0))
    OutDateAppTheme {
        FamilyScreen(
            state = FamilyUiState.Active(
                family = Family("f1", "Квартира на Ленина", isOwner = true, members = members),
                categories = listOf(food, medicine),
                selectedCategoryId = null,
                groups = listOf(
                    ItemsGroup(
                        ItemsSection.SOON,
                        listOf(ItemUi("1", "Молоко", 1, Urgency.CRITICAL, today.minusDays(1), true, food, "Миша"))
                    ),
                    ItemsGroup(
                        ItemsSection.LATER,
                        listOf(ItemUi("2", "Нурофен", 365, Urgency.OK, null, false, medicine, "Мама"))
                    )
                ),
                hasItems = true
            ),
            profileAction = {},
            onSignIn = {},
            onCreate = {},
            onJoin = {},
            onMembers = {},
            onAddClick = {},
            onItemClick = {},
            onItemDelete = {},
            onCategorySelect = {}
        )
    }
}
