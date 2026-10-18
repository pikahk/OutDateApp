package ru.pikahk.outdateapp.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import ru.pikahk.outdateapp.R

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistered: () -> Unit,
    onSignIn: (String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(viewModel.outcome) {
        if (viewModel.outcome != null) {
            viewModel.consumeOutcome()
            onRegistered()
        }
    }
    val emailError = viewModel.error.on(AuthField.EMAIL)
    AuthScaffold(
        title = stringResource(R.string.auth_register_title),
        onBack = onBack,
        actions = {
            FormError(viewModel.error)
            PrimaryButton(
                text = stringResource(R.string.auth_register),
                loading = viewModel.loading,
                enabled = viewModel.canSubmit,
                onClick = viewModel::submit
            )
            LinkButton(text = stringResource(R.string.auth_have_account), onClick = onBack)
        }
    ) {
        EmailField(
            value = viewModel.email,
            onValueChange = viewModel::updateEmail,
            error = emailError,
            onErrorAction = if (emailError == AuthError.EMAIL_TAKEN) {
                { onSignIn(viewModel.email.trim()) }
            } else {
                null
            }
        )
        PasswordField(
            value = viewModel.password,
            onValueChange = viewModel::updatePassword,
            label = stringResource(R.string.auth_password),
            error = viewModel.error.on(AuthField.PASSWORD),
            onDone = viewModel::submit,
            hint = stringResource(R.string.auth_password_hint)
        )
    }
}
