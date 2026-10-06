package ru.pikahk.outdateapp.ui.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.pikahk.outdateapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemsTopBar(
    query: TextFieldState,
    sort: ItemsSort,
    opened: OpenedFilter,
    onSortSelect: (ItemsSort) -> Unit,
    onOpenedSelect: (OpenedFilter) -> Unit,
    onProfileClick: () -> Unit
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    val closeSearch = {
        query.clearText()
        searching = false
    }
    BackHandler(enabled = searching, onBack = closeSearch)

    TopAppBar(
        title = {
            if (searching) {
                SearchField(query)
            } else {
                Text(
                    text = stringResource(R.string.items_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            if (searching) {
                IconButton(onClick = closeSearch) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            if (!searching) {
                IconButton(onClick = { searching = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search)
                    )
                }
                ListOptionsButton(
                    sort = sort,
                    opened = opened,
                    onSortSelect = onSortSelect,
                    onOpenedSelect = onOpenedSelect
                )
                IconButton(onClick = onProfileClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_person),
                        contentDescription = stringResource(R.string.profile_title)
                    )
                }
            } else if (query.text.isNotEmpty()) {
                IconButton(onClick = { query.clearText() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.search_clear)
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchField(query: TextFieldState) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        if (query.text.isEmpty()) focusRequester.requestFocus()
    }
    val textStyle = MaterialTheme.typography.bodyLarge
    BasicTextField(
        state = query,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { focusManager.clearFocus() },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorator = { innerTextField ->
            Box {
                if (query.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search),
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ListOptionsButton(
    sort: ItemsSort,
    opened: OpenedFilter,
    onSortSelect: (ItemsSort) -> Unit,
    onOpenedSelect: (OpenedFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            BadgedBox(
                badge = {
                    if (opened != OpenedFilter.ALL) Badge(containerColor = MaterialTheme.colorScheme.primary)
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter_list),
                    contentDescription = stringResource(R.string.list_options)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MenuHeader(stringResource(R.string.sort_title))
            ItemsSort.entries.forEach { option ->
                OptionItem(
                    text = stringResource(option.titleRes()),
                    selected = option == sort,
                    onClick = {
                        onSortSelect(option)
                        expanded = false
                    }
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            MenuHeader(stringResource(R.string.show_title))
            OpenedFilter.entries.forEach { option ->
                OptionItem(
                    text = stringResource(option.titleRes()),
                    selected = option == opened,
                    onClick = {
                        onOpenedSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun OptionItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

private fun ItemsSort.titleRes(): Int = when (this) {
    ItemsSort.EXPIRY -> R.string.sort_expiry
    ItemsSort.NAME -> R.string.sort_name
    ItemsSort.ADDED -> R.string.sort_added
}

private fun OpenedFilter.titleRes(): Int = when (this) {
    OpenedFilter.ALL -> R.string.show_all
    OpenedFilter.OPENED -> R.string.show_opened
    OpenedFilter.SEALED -> R.string.show_sealed
}
