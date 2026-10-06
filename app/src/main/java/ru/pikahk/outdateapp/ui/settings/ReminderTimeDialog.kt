package ru.pikahk.outdateapp.ui.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import java.time.LocalTime
import ru.pikahk.outdateapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimeDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    val clockFits = LocalWindowInfo.current.containerDpSize.height > TimePickerDialogDefaults.MinHeightForTimePicker
    var keyboard by rememberSaveable { mutableStateOf(false) }
    val displayMode = if (keyboard || !clockFits) TimePickerDisplayMode.Input else TimePickerDisplayMode.Picker

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { TimePickerDialogDefaults.Title(displayMode = displayMode) },
        modeToggleButton = {
            if (clockFits) {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = { keyboard = !keyboard },
                    displayMode = displayMode
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPick(LocalTime.of(state.hour, state.minute))
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) {
            TimePicker(state = state)
        } else {
            TimeInput(state = state)
        }
    }
}
