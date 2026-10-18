package ru.pikahk.outdateapp.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import ru.pikahk.outdateapp.R

@Composable
fun VerifyEmailScreen(viewModel: VerifyEmailViewModel, onVerified: () -> Unit, onSignedOut: () -> Unit) {
    LaunchedEffect(viewModel.outcome) {
        if (viewModel.outcome != null) {
            viewModel.consumeOutcome()
            onVerified()
        }
    }
    LaunchedEffect(viewModel.signedOut) {
        if (viewModel.signedOut) onSignedOut()
    }
    BackHandler(onBack = viewModel::changeEmail)
    AuthScaffold(
        title = stringResource(R.string.auth_verify_title),
        onBack = null,
        actions = {
            FormError(viewModel.error)
            PrimaryButton(
                text = stringResource(R.string.auth_confirm),
                loading = viewModel.loading,
                enabled = viewModel.code.length == CODE_LENGTH,
                onClick = viewModel::submit
            )
            LinkButton(
                text = stringResource(R.string.auth_change_email),
                onClick = viewModel::changeEmail,
                quiet = true
            )
        }
    ) {
        Hint(stringResource(R.string.auth_code_sent, viewModel.email))
        CodeField(
            value = viewModel.code,
            onValueChange = viewModel::updateCode,
            error = viewModel.error.on(AuthField.CODE)
        )
        ResendButton(secondsLeft = viewModel.resendIn, onResend = viewModel::resend)
    }
}
