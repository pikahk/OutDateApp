package ru.pikahk.outdateapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ru.pikahk.outdateapp.ui.add.AddItemScreen
import ru.pikahk.outdateapp.ui.items.ItemsScreen
import ru.pikahk.outdateapp.ui.items.ItemsViewModel

@Serializable
private object ItemsRoute

@Serializable
private object AddItemRoute

@Composable
fun OutDateApp() {
    val context = LocalContext.current
    val itemsViewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.factory(context))
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ItemsRoute) {
        composable<ItemsRoute> {
            ItemsScreen(
                viewModel = itemsViewModel,
                onAddClick = dropUnlessResumed { navController.navigate(AddItemRoute) }
            )
        }
        composable<AddItemRoute> { entry ->
            AddItemScreen(
                onSave = { name, expiresAt ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        itemsViewModel.addItem(name, expiresAt)
                        navController.popBackStack()
                    }
                },
                onCancel = dropUnlessResumed { navController.popBackStack() }
            )
        }
    }
}
