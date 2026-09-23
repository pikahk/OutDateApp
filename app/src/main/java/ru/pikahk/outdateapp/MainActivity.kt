package ru.pikahk.outdateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pikahk.outdateapp.ui.items.ItemsScreen
import ru.pikahk.outdateapp.ui.items.ItemsViewModel
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OutDateAppTheme {
                val viewModel: ItemsViewModel = viewModel(
                    factory = ItemsViewModel.factory(this@MainActivity)
                )
                ItemsScreen(viewModel)
            }
        }
    }
}
