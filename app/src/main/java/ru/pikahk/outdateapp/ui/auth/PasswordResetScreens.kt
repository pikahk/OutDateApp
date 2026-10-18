package ru.pikahk.outdateapp.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import ru.pikahk.outdateapp.R

@Composable
fun ResetRequestScreen(viewModel: ResetRequestViewModel, onCodeSent: (String) -> Unit, onBack: () -> Unit) {
    val outcome = viewModel.outcome
    LaunchedEffect(outcome) {
        if (outcome != null) {
            viewModel.consumeOutcome()
            onCodeSent(outcome)
        }
    }
    AuthScaffold(
        title = stringResource(R.string.auth_reset_title),
        onBack = onBack,
        actions = {
            FormError(viewModel.error)
            PrimaryButton(
                text = stringResource(R.string.auth_get_code),
                loading = viewModel.loading,
                enabled = viewModel.email.isNotBlank(),
                onClick = viewModel::submit
            )
        }
    ) {
        EmailField(
            value = viewModel.email,
            onValueChange = viewModel::updateEmail,
            error = viewModel.error.on(AuthField.EMAIL)
        )
    }
}

@Composable
fun ResetPasswordScreen(viewModel: ResetPasswordViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    LaunchedEffect(viewModel.outcome) {
        if (viewModel.outcome != null) {
            viewModel.consumeOutcome()
            onDone()
        }
    }
    AuthScaffold(
        title = stringResource(R.string.auth_reset_title),
        onBack = onBack,
        actions = {
            FormError(viewModel.error)
            PrimaryButton(
                text = stringResource(R.string.auth_save_password),
                loading = viewModel.loading,
                enabled = viewModel.canSubmit,
                onClick = viewModel::submit
            )
        }
    ) {
        Hint(stringResource(R.string.auth_code_sent, viewModel.email))
        CodeField(
            value = viewModel.code,
            onValueChange = viewModel::updateCode,
            error = viewModel.error.on(AuthField.CODE)
        )
        PasswordField(
            value = viewModel.password,
            onValueChange = viewModel::updatePassword,
            label = stringResource(R.string.auth_new_password),
            error = viewModel.error.on(AuthField.PASSWORD),
            onDone = viewModel::submit,
            hint = stringResource(R.string.auth_password_hint)
        )
        ResendButton(secondsLeft = viewModel.resendIn, onResend = viewModel::resend)
    }
}
