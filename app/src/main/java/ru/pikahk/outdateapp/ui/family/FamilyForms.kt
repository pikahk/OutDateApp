package ru.pikahk.outdateapp.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.add.FieldShape
import ru.pikahk.outdateapp.ui.add.LabeledField
import ru.pikahk.outdateapp.ui.add.fieldColors
import ru.pikahk.outdateapp.ui.auth.AuthScaffold
import ru.pikahk.outdateapp.ui.auth.CodeCells
import ru.pikahk.outdateapp.ui.auth.LinkButton
import ru.pikahk.outdateapp.ui.auth.PrimaryButton

@Composable
fun CreateFamilyScreen(
    busy: Boolean,
    error: FamilyError?,
    onCreate: (name: String, memberName: String) -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var memberName by rememberSaveable { mutableStateOf("") }
    AuthScaffold(
        title = stringResource(R.string.family_new_title),
        onBack = onBack,
        actions = {
            FamilyErrorText(error)
            PrimaryButton(
                text = stringResource(R.string.family_create_button),
                loading = busy,
                enabled = name.isNotBlank() && memberName.isNotBlank(),
                onClick = { onCreate(name, memberName) }
            )
        }
    ) {
        NameField(
            label = stringResource(R.string.family_name),
            value = name,
            onValueChange = { name = it.take(FAMILY_NAME_LIMIT) }
        )
        NameField(
            label = stringResource(R.string.family_member_name),
            value = memberName,
            onValueChange = { memberName = it.take(MEMBER_NAME_LIMIT) },
            hint = stringResource(R.string.family_member_name_hint)
        )
    }
}

@Composable
fun JoinFamilyScreen(
    busy: Boolean,
    error: FamilyError?,
    onJoin: (code: String, memberName: String) -> Unit,
    onBack: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var memberName by rememberSaveable { mutableStateOf("") }
    val codeError = error == FamilyError.INVALID_CODE
    AuthScaffold(
        title = stringResource(R.string.family_join_title),
        onBack = onBack,
        actions = {
            FamilyErrorText(error.takeUnless { codeError })
            PrimaryButton(
                text = stringResource(R.string.family_join_button),
                loading = busy,
                enabled = code.length == INVITE_CODE_LENGTH && memberName.isNotBlank(),
                onClick = { onJoin(code, memberName) }
            )
        }
    ) {
        LabeledField(label = stringResource(R.string.family_code)) {
            Column {
                InviteCodeField(value = code, onValueChange = { code = it }, isError = codeError)
                if (codeError) {
                    Text(
                        text = stringResource(FamilyError.INVALID_CODE.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        NameField(
            label = stringResource(R.string.family_member_name),
            value = memberName,
            onValueChange = { memberName = it.take(MEMBER_NAME_LIMIT) },
            hint = stringResource(R.string.family_member_name_hint)
        )
    }
}

@Composable
fun InviteScreen(
    familyName: String,
    invite: InviteUi?,
    busy: Boolean,
    error: FamilyError?,
    onRetry: () -> Unit,
    onShare: (String) -> Unit,
    onDone: () -> Unit
) {
    val shareText = invite?.let { stringResource(R.string.family_share_text, familyName, formatInviteCode(it.code)) }
    AuthScaffold(
        title = stringResource(R.string.family_invite_title),
        onBack = onDone,
        actions = {
            if (shareText != null) {
                PrimaryButton(
                    text = stringResource(R.string.family_share),
                    loading = false,
                    enabled = true,
                    onClick = { onShare(shareText) }
                )
                LinkButton(text = stringResource(R.string.family_done), onClick = onDone)
            } else if (error != null && !busy) {
                FamilyErrorText(error)
                PrimaryButton(
                    text = stringResource(R.string.family_retry),
                    loading = false,
                    enabled = true,
                    onClick = onRetry
                )
            }
        }
    ) {
        Text(
            text = stringResource(R.string.family_invite_text, familyName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (invite != null) {
            Text(
                text = formatInviteCode(invite.code),
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp)
            )
            Text(
                text = stringResource(R.string.family_invite_until, rememberExpiry(invite)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (busy) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun rememberExpiry(invite: InviteUi): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, invite) {
        invite.expiresAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMMM, HH:mm", locale))
    }
}

@Composable
private fun NameField(label: String, value: String, onValueChange: (String) -> Unit, hint: String? = null) {
    LabeledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            supportingText = hint?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InviteCodeField(value: String, onValueChange: (String) -> Unit, isError: Boolean) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(normalizeInviteCode(it)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box {
                Box(modifier = Modifier.size(1.dp)) { innerTextField() }
                CodeCells(value = value, isError = isError)
            }
        }
    )
}

@Composable
internal fun FamilyErrorText(error: FamilyError?) {
    if (error == null) return
    Text(
        text = stringResource(error.message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}
