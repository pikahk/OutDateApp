package ru.pikahk.outdateapp.ui.add

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.repository.ProductLookup
import ru.pikahk.outdateapp.domain.DEFAULT_REMINDER_DAYS
import ru.pikahk.outdateapp.domain.DateReading
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
import ru.pikahk.outdateapp.ui.scan.BarcodeScanner
import ru.pikahk.outdateapp.ui.scan.DateScanner
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private enum class InputMode { EXPIRY_DATE, PRODUCTION }

private enum class Scanner { BARCODE, DATE }

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
    defaultNotifyDaysBefore: Int,
    onCreateCategory: ((String) -> String)?,
    onSave: (ItemDraft) -> Unit,
    onCancel: () -> Unit,
    onLookup: suspend (String) -> ProductLookup,
    dateNoticeShown: Boolean,
    onDateNoticeShown: () -> Unit,
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
    var notifyDaysBefore by rememberSaveable { mutableIntStateOf(defaultNotifyDaysBefore) }
    var showMissing by rememberSaveable { mutableStateOf(false) }
    var barcode by rememberSaveable { mutableStateOf<String?>(null) }
    var scanner by rememberSaveable { mutableStateOf<Scanner?>(null) }
    var requestedScanner by rememberSaveable { mutableStateOf<Scanner?>(null) }
    var showDateNotice by rememberSaveable { mutableStateOf(false) }
    var scanNote by rememberSaveable { mutableStateOf<ScanNote?>(null) }
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (scanNote == ScanNote.NO_CAMERA) scanNote = null
            scanner = requestedScanner
        } else {
            scanNote = ScanNote.NO_CAMERA
        }
    }

    fun openScanner(kind: Scanner) {
        if (context.hasCameraPermission()) {
            scanner = kind
        } else {
            requestedScanner = kind
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun applyLookup(code: String, lookup: ProductLookup) {
        barcode = code
        scanNote = lookup.toNote()
        when (lookup) {
            is ProductLookup.Remembered -> {
                name = lookup.product.name
                categoryId = lookup.product.categoryId
                openedAmountText = lookup.product.defaultDaysAfterOpening?.toString().orEmpty()
                openedUnit = ShelfLifeUnit.DAYS
                lookup.product.defaultNotifyDaysBefore?.let { notifyDaysBefore = it }
            }
            is ProductLookup.Found -> {
                name = lookup.name
                categoryId = lookup.categoryId
            }
            else -> Unit
        }
    }

    fun applyDates(reading: DateReading) {
        when (reading) {
            is DateReading.Expiry -> {
                mode = InputMode.EXPIRY_DATE
                expiryText = formatDate(reading.expiresAt)
                if (scanNote == ScanNote.PRODUCTION_DATE) scanNote = null
            }
            is DateReading.Production -> {
                mode = InputMode.PRODUCTION
                producedText = formatDate(reading.producedAt)
                scanNote = ScanNote.PRODUCTION_DATE
            }
            is DateReading.Unclear -> Unit
        }
    }

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
                                            notifyDaysBefore = notifyDaysBefore,
                                            barcode = barcode
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
            scanNote?.let { note ->
                ScanNoteCard(note = note, onOpenSettings = context::openAppSettings)
            }
            if (hasCamera) {
                OutlinedButton(
                    onClick = { openScanner(Scanner.BARCODE) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barcode),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_barcode))
                }
            }

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
            if (hasCamera) {
                TextButton(
                    onClick = { if (dateNoticeShown) openScanner(Scanner.DATE) else showDateNotice = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.date_recognize))
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

    if (showDateNotice) {
        AlertDialog(
            onDismissRequest = { showDateNotice = false },
            title = { Text(stringResource(R.string.date_notice_title)) },
            text = { Text(stringResource(R.string.date_notice_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDateNotice = false
                        onDateNoticeShown()
                        openScanner(Scanner.DATE)
                    }
                ) {
                    Text(stringResource(R.string.away_ok))
                }
            }
        )
    }

    val activeScanner = scanner
    if (activeScanner != null) {
        Dialog(
            onDismissRequest = { scanner = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            when (activeScanner) {
                Scanner.BARCODE -> BarcodeScanner(
                    onLookup = onLookup,
                    onResult = { code, lookup ->
                        applyLookup(code, lookup)
                        scanner = null
                    },
                    onClose = { scanner = null }
                )

                Scanner.DATE -> DateScanner(
                    onConfirm = { reading ->
                        applyDates(reading)
                        scanner = null
                    },
                    onClose = { scanner = null }
                )
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

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    OutDateAppTheme {
        AddItemScreen(
            categories = emptyList(),
            initialCategoryId = null,
            defaultNotifyDaysBefore = DEFAULT_REMINDER_DAYS,
            onCreateCategory = { "" },
            onSave = {},
            onCancel = {},
            onLookup = { ProductLookup.NotFound },
            dateNoticeShown = true,
            onDateNoticeShown = {}
        )
    }
}
