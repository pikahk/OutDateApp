package ru.pikahk.outdateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.pikahk.outdateapp.ui.OutDateApp
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OutDateAppTheme {
                OutDateApp()
            }
        }
    }
}
