package ru.pikahk.outdateapp.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.Locale
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.ui.add.FieldShape
import ru.pikahk.outdateapp.ui.add.LabeledField
import ru.pikahk.outdateapp.ui.add.fieldColors

private const val SECONDS_IN_MINUTE = 60

@StringRes
private fun AuthError.message(): Int = when (this) {
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.EMAIL_TAKEN -> R.string.auth_error_email_taken
    AuthError.INVALID_PASSWORD -> R.string.auth_password_hint
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.INVALID_CODE -> R.string.auth_error_invalid_code
    AuthError.CODE_EXPIRED -> R.string.auth_error_code_expired
    AuthError.TOO_SOON -> R.string.auth_error_too_soon
    AuthError.MAIL_UNAVAILABLE -> R.string.auth_error_mail
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.SERVER -> R.string.auth_error_server
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthScaffold(
    title: String?,
    onBack: (() -> Unit)?,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (title != null) Text(text = title, fontWeight = FontWeight.Bold) else Brandmark()
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun Brandmark() {
    Text(
        text = buildAnnotatedString {
            append("out")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Date") }
        },
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun ScreenTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
internal fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    error: AuthError?,
    onErrorAction: (() -> Unit)? = null
) {
    LabeledField(label = stringResource(R.string.auth_email)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = error != null,
            supportingText = if (error != null) {
                { ErrorWithAction(message = stringResource(error.message()), onAction = onErrorAction) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorWithAction(message: String, onAction: (() -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = message, modifier = Modifier.weight(1f))
        if (onAction != null) {
            Text(
                text = stringResource(R.string.auth_sign_in),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: AuthError?,
    onDone: () -> Unit,
    hint: String? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val supporting = error?.let { stringResource(it.message()) } ?: hint
    LabeledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        painter = painterResource(
                            if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                        ),
                        contentDescription = stringResource(
                            if (visible) R.string.auth_hide_password else R.string.auth_show_password
                        )
                    )
                }
            },
            isError = error != null,
            supportingText = if (supporting != null) {
                { Text(supporting) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun CodeField(value: String, onValueChange: (String) -> Unit, error: AuthError?) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    Box(modifier = Modifier.size(1.dp)) { innerTextField() }
                    CodeCells(value = value, isError = error != null)
                }
            }
        )
        if (error != null) {
            Text(
                text = stringResource(error.message()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
internal fun CodeCells(value: String, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(CODE_LENGTH) { index ->
            val borderColor = when {
                isError -> MaterialTheme.colorScheme.error
                index == value.length -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, FieldShape)
                    .border(width = if (index == value.length) 2.dp else 1.dp, color = borderColor, shape = FieldShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = value.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
internal fun ResendButton(secondsLeft: Int, onResend: () -> Unit) {
    if (secondsLeft > 0) {
        val time = String.format(
            Locale.ROOT,
            "%d:%02d",
            secondsLeft / SECONDS_IN_MINUTE,
            secondsLeft % SECONDS_IN_MINUTE
        )
        Text(
            text = stringResource(R.string.auth_resend_in, time),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )
    } else {
        LinkButton(text = stringResource(R.string.auth_resend), onClick = onResend)
    }
}

@Composable
internal fun FormError(error: AuthError?) {
    val formError = error.on(AuthField.FORM) ?: return
    Text(
        text = stringResource(formError.message()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

@Composable
internal fun PrimaryButton(text: String, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = LocalContentColor.current
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun LinkButton(text: String, onClick: () -> Unit, quiet: Boolean = false, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
    }
}
