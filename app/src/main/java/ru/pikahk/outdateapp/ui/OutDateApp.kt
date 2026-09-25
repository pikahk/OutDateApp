package ru.pikahk.outdateapp.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.pikahk.outdateapp.ui.add.AddItemScreen
import ru.pikahk.outdateapp.ui.details.ItemDetailsScreen
import ru.pikahk.outdateapp.ui.details.ItemDetailsViewModel
import ru.pikahk.outdateapp.ui.items.ItemsScreen
import ru.pikahk.outdateapp.ui.items.ItemsViewModel

private const val TRANSITION_MILLIS = 250

@Serializable
private object ItemsRoute

@Serializable
private object AddItemRoute

@Serializable
private data class ItemDetailsRoute(val id: String)

@Composable
fun OutDateApp() {
    val context = LocalContext.current
    val itemsViewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.factory(context))
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = ItemsRoute,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(TRANSITION_MILLIS))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(TRANSITION_MILLIS),
                    targetOffset = { it / 4 }
                ) + fadeOut(tween(TRANSITION_MILLIS))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(TRANSITION_MILLIS),
                    initialOffset = { it / 4 }
                ) + fadeIn(tween(TRANSITION_MILLIS))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(TRANSITION_MILLIS))
            }
        ) {
            composable<ItemsRoute> { entry ->
                ItemsScreen(
                    viewModel = itemsViewModel,
                    onAddClick = dropUnlessResumed { navController.navigate(AddItemRoute) },
                    onItemClick = { id ->
                        if (entry.isResumed()) navController.navigate(ItemDetailsRoute(id))
                    }
                )
            }
            composable<AddItemRoute> { entry ->
                AddItemScreen(
                    onSave = { draft ->
                        if (entry.isResumed()) {
                            itemsViewModel.addItem(draft)
                            navController.popBackStack()
                        }
                    },
                    onCancel = dropUnlessResumed { navController.popBackStack() }
                )
            }
            composable<ItemDetailsRoute> { entry ->
                val route = entry.toRoute<ItemDetailsRoute>()
                val detailsViewModel: ItemDetailsViewModel =
                    viewModel(factory = ItemDetailsViewModel.factory(context, route.id))
                ItemDetailsScreen(
                    viewModel = detailsViewModel,
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    onGone = {
                        if (entry.isResumed()) navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun NavBackStackEntry.isResumed(): Boolean = lifecycle.currentState == Lifecycle.State.RESUMED
