package ru.pikahk.outdateapp.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.Session

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onSignedIn: (Session) -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSkip: (() -> Unit)?,
    onBack: (() -> Unit)?
) {
    val outcome = viewModel.outcome
    LaunchedEffect(outcome) {
        if (outcome != null) {
            viewModel.consumeOutcome()
            onSignedIn(outcome)
        }
    }
    AuthScaffold(
        title = null,
        onBack = onBack,
        actions = {
            FormError(viewModel.error)
            PrimaryButton(
                text = stringResource(R.string.auth_sign_in),
                loading = viewModel.loading,
                enabled = viewModel.canSubmit,
                onClick = viewModel::submit
            )
            LinkButton(text = stringResource(R.string.auth_create_account), onClick = onRegister)
            if (onSkip != null) {
                LinkButton(text = stringResource(R.string.auth_skip), onClick = onSkip, quiet = true)
            }
        }
    ) {
        ScreenTitle(stringResource(R.string.auth_sign_in_title))
        EmailField(
            value = viewModel.email,
            onValueChange = viewModel::updateEmail,
            error = viewModel.error.on(AuthField.EMAIL)
        )
        PasswordField(
            value = viewModel.password,
            onValueChange = viewModel::updatePassword,
            label = stringResource(R.string.auth_password),
            error = viewModel.error.on(AuthField.PASSWORD),
            onDone = viewModel::submit
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { onForgotPassword(viewModel.email.trim()) }) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
