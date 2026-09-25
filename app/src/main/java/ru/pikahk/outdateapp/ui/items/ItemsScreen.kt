package ru.pikahk.outdateapp.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: ItemsViewModel, onAddClick: () -> Unit, onItemClick: (String) -> Unit) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.items_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_item)
                )
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.items_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(items = items, key = { it.id }) { item ->
                    ItemRow(item = item, onClick = { onItemClick(item.id) })
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemUi, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.name) },
        trailingContent = {
            Text(
                text = daysLabel(item.daysLeft),
                color = item.urgency.color(),
                style = MaterialTheme.typography.labelLarge
            )
        }
    )
}

@Composable
private fun daysLabel(daysLeft: Long): String = when {
    daysLeft < 0 -> stringResource(R.string.list_expired)
    daysLeft == 0L -> stringResource(R.string.list_today)
    else -> pluralStringResource(R.plurals.days, daysLeft.toInt(), daysLeft.toInt())
}
