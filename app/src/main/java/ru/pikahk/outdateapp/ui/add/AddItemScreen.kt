package ru.pikahk.outdateapp.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.domain.ShelfLifeUnit
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.domain.expiryFromProduction
import ru.pikahk.outdateapp.domain.formatDate
import ru.pikahk.outdateapp.domain.parseDate
import ru.pikahk.outdateapp.domain.parseExpiryDate
import ru.pikahk.outdateapp.domain.shelfLifeInDays
import ru.pikahk.outdateapp.domain.urgency
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.color
import ru.pikahk.outdateapp.ui.containerColor
import ru.pikahk.outdateapp.ui.reminderOptions
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private enum class InputMode { EXPIRY_DATE, PRODUCTION }

private val modeOptions = listOf(
    InputMode.EXPIRY_DATE to R.string.mode_expiry,
    InputMode.PRODUCTION to R.string.mode_production
)

internal val unitOptions = listOf(
    ShelfLifeUnit.HOURS to R.string.unit_hours,
    ShelfLifeUnit.DAYS to R.string.unit_days,
    ShelfLifeUnit.MONTHS to R.string.unit_months,
    ShelfLifeUnit.YEARS to R.string.unit_years
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    categories: List<CategoryUi>,
    initialCategoryId: String?,
    onCreateCategory: (String) -> String,
    onSave: (ItemDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    var name by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(initialCategoryId) }
    var mode by rememberSaveable { mutableStateOf(InputMode.EXPIRY_DATE) }
    var expiryText by rememberSaveable { mutableStateOf("") }
    var producedText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }
    var openedAmountText by rememberSaveable { mutableStateOf("") }
    var openedUnit by rememberSaveable { mutableStateOf(ShelfLifeUnit.DAYS) }
    var alreadyOpened by rememberSaveable { mutableStateOf(false) }
    var openedText by rememberSaveable { mutableStateOf("") }
    var notifyDaysBefore by rememberSaveable { mutableIntStateOf(3) }
    var showMissing by rememberSaveable { mutableStateOf(false) }

    val expiresAt = when (mode) {
        InputMode.EXPIRY_DATE -> parseExpiryDate(expiryText)
        InputMode.PRODUCTION -> calculateExpiry(producedText, amountText, unit)
    }
    val daysAfterOpening = parseAmount(openedAmountText)?.let { shelfLifeInDays(it, openedUnit) }
    val openedAt = if (alreadyOpened) parseDate(openedText) else null
    val canSave = name.isNotBlank() && expiresAt != null && (!alreadyOpened || openedAt != null)
    val nameMissing = showMissing && name.isBlank()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.add_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (expiresAt != null) {
                        ExpirySummary(
                            expiresAt = expiresAt,
                            effective = effectiveExpiryDate(expiresAt, openedAt, daysAfterOpening),
                            today = today
                        )
                    }
                    Box {
                        Button(
                            onClick = {
                                if (expiresAt != null) {
                                    onSave(
                                        ItemDraft(
                                            name = name.trim(),
                                            categoryId = categoryId,
                                            expiresAt = expiresAt,
                                            daysAfterOpening = daysAfterOpening,
                                            openedAt = openedAt,
                                            notifyDaysBefore = notifyDaysBefore
                                        )
                                    )
                                }
                            },
                            enabled = canSave,
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
                        }
                        if (!canSave) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(interactionSource = null, indication = null) { showMissing = true }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabeledField(label = stringResource(R.string.field_name)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    isError = nameMissing,
                    supportingText = if (nameMissing) {
                        { Text(stringResource(R.string.field_required)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            CategoryField(
                categories = categories,
                selectedId = categoryId,
                onSelect = { categoryId = it },
                onCreate = onCreateCategory
            )

            Segmented(options = modeOptions, selected = mode, onSelect = { mode = it })

            when (mode) {
                InputMode.EXPIRY_DATE -> DateField(
                    value = expiryText,
                    onValueChange = { expiryText = it },
                    label = stringResource(R.string.field_expires),
                    hint = stringResource(R.string.hint_expiry_format),
                    parse = ::parseExpiryDate,
                    missing = showMissing && expiresAt == null
                )

                InputMode.PRODUCTION -> {
                    DateField(
                        value = producedText,
                        onValueChange = { producedText = it },
                        label = stringResource(R.string.field_produced),
                        hint = stringResource(R.string.hint_date_format),
                        parse = ::parseDate,
                        missing = showMissing && parseDate(producedText) == null
                    )
                    AmountField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = stringResource(R.string.field_shelf_life),
                        missing = showMissing && parseAmount(amountText) == null
                    )
                    Segmented(options = unitOptions, selected = unit, onSelect = { unit = it })
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AmountField(
                value = openedAmountText,
                onValueChange = { openedAmountText = it },
                label = stringResource(R.string.field_after_opening),
                hint = stringResource(R.string.hint_after_opening)
            )
            Segmented(options = unitOptions, selected = openedUnit, onSelect = { openedUnit = it })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.already_opened),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = alreadyOpened,
                    onCheckedChange = { checked ->
                        alreadyOpened = checked
                        if (checked && openedText.isEmpty()) openedText = formatDate(today)
                    }
                )
            }
            if (alreadyOpened) {
                DateField(
                    value = openedText,
                    onValueChange = { openedText = it },
                    label = stringResource(R.string.field_opened),
                    hint = stringResource(R.string.hint_date_format),
                    parse = ::parseDate,
                    missing = showMissing && openedAt == null
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LabeledField(label = stringResource(R.string.field_reminder)) {
                Segmented(options = reminderOptions, selected = notifyDaysBefore, onSelect = { notifyDaysBefore = it })
            }
        }
    }
}

@Composable
private fun ExpirySummary(expiresAt: LocalDate, effective: LocalDate, today: LocalDate) {
    val left = ChronoUnit.DAYS.between(today, effective)
    val level = urgency(left)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val date = effective.format(formatter)
    val text = when {
        left < 0 -> stringResource(R.string.summary_expired, date)
        effective < expiresAt -> stringResource(R.string.summary_expires_opening, date)
        else -> stringResource(R.string.summary_expires, date)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = level.color(),
        modifier = Modifier
            .fillMaxWidth()
            .background(color = level.containerColor(), shape = RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp)
    )
}

private fun calculateExpiry(producedText: String, amountText: String, unit: ShelfLifeUnit): LocalDate? {
    val producedAt = parseDate(producedText) ?: return null
    val amount = parseAmount(amountText) ?: return null
    return expiryFromProduction(producedAt, amount, unit)
}

private fun parseAmount(text: String): Int? = text.toIntOrNull()?.takeIf { it > 0 }

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    OutDateAppTheme {
        AddItemScreen(
            categories = emptyList(),
            initialCategoryId = null,
            onCreateCategory = { "" },
            onSave = {},
            onCancel = {}
        )
    }
}
