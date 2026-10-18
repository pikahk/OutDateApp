package ru.pikahk.outdateapp.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
private data class SignInRoute(val email: String = "")

@Serializable
private object RegisterRoute

@Serializable
private data class VerifyEmailRoute(val codeSent: Boolean = false)

@Serializable
private data class ResetRequestRoute(val email: String = "")

@Serializable
private data class ResetPasswordRoute(val email: String)

@Composable
fun AuthFlow(startWithCode: Boolean, onFinished: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startWithCode) VerifyEmailRoute() else SignInRoute()
    ) {
        composable<SignInRoute> { entry ->
            val route = entry.toRoute<SignInRoute>()
            val signInViewModel: SignInViewModel = viewModel(factory = SignInViewModel.factory(context, route.email))
            SignInScreen(
                viewModel = signInViewModel,
                onSignedIn = { session ->
                    if (session.emailVerified) onFinished() else navController.navigate(VerifyEmailRoute())
                },
                onRegister = dropUnlessResumed { navController.navigate(RegisterRoute) },
                onForgotPassword = { email -> navController.navigate(ResetRequestRoute(email)) },
                onSkip = onFinished
            )
        }
        composable<RegisterRoute> {
            val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.factory(context))
            RegisterScreen(
                viewModel = registerViewModel,
                onRegistered = { navController.navigate(VerifyEmailRoute(codeSent = true)) },
                onSignIn = { email ->
                    navController.navigate(SignInRoute(email)) {
                        popUpTo<SignInRoute> { inclusive = true }
                    }
                },
                onBack = dropUnlessResumed { navController.popBackStack() }
            )
        }
        composable<VerifyEmailRoute> { entry ->
            val route = entry.toRoute<VerifyEmailRoute>()
            val verifyViewModel: VerifyEmailViewModel =
                viewModel(factory = VerifyEmailViewModel.factory(context, route.codeSent))
            VerifyEmailScreen(
                viewModel = verifyViewModel,
                onVerified = onFinished,
                onSignedOut = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(SignInRoute()) {
                            popUpTo<VerifyEmailRoute> { inclusive = true }
                        }
                    }
                }
            )
        }
        composable<ResetRequestRoute> { entry ->
            val route = entry.toRoute<ResetRequestRoute>()
            val resetRequestViewModel: ResetRequestViewModel =
                viewModel(factory = ResetRequestViewModel.factory(context, route.email))
            ResetRequestScreen(
                viewModel = resetRequestViewModel,
                onCodeSent = { email -> navController.navigate(ResetPasswordRoute(email)) },
                onBack = dropUnlessResumed { navController.popBackStack() }
            )
        }
        composable<ResetPasswordRoute> { entry ->
            val route = entry.toRoute<ResetPasswordRoute>()
            val resetPasswordViewModel: ResetPasswordViewModel =
                viewModel(factory = ResetPasswordViewModel.factory(context, route.email))
            ResetPasswordScreen(
                viewModel = resetPasswordViewModel,
                onDone = onFinished,
                onBack = dropUnlessResumed { navController.popBackStack() }
            )
        }
    }
}
