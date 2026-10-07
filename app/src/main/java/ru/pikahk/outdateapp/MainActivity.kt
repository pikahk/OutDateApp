package ru.pikahk.outdateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.repository.ThemeMode
import ru.pikahk.outdateapp.notifications.ExpiryWorker
import ru.pikahk.outdateapp.ui.OutDateApp
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { ExpiryWorker.schedule(applicationContext) }
        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(this))
            val theme by mainViewModel.theme.collectAsStateWithLifecycle()
            val mode = theme ?: return@setContent
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            OutDateAppTheme(darkTheme = darkTheme) {
                OutDateApp()
            }
        }
    }
}
