package ru.pikahk.outdateapp.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.notifications.NotificationPermissionRequest
import ru.pikahk.outdateapp.ui.add.AddItemScreen
import ru.pikahk.outdateapp.ui.details.ItemDetailsScreen
import ru.pikahk.outdateapp.ui.details.ItemDetailsViewModel
import ru.pikahk.outdateapp.ui.items.ItemsScreen
import ru.pikahk.outdateapp.ui.items.ItemsViewModel
import ru.pikahk.outdateapp.ui.profile.ProfileScreen
import ru.pikahk.outdateapp.ui.profile.ProfileViewModel
import ru.pikahk.outdateapp.ui.settings.SettingsScreen
import ru.pikahk.outdateapp.ui.settings.SettingsViewModel
import ru.pikahk.outdateapp.ui.subscriptions.SubscriptionEditScreen
import ru.pikahk.outdateapp.ui.subscriptions.SubscriptionEditViewModel
import ru.pikahk.outdateapp.ui.subscriptions.SubscriptionsScreen
import ru.pikahk.outdateapp.ui.subscriptions.SubscriptionsViewModel

private const val TRANSITION_MILLIS = 250

const val ITEM_DEEP_LINK = "outdate://item"

const val SUBSCRIPTIONS_DEEP_LINK = "outdate://subscriptions"

@Serializable
private object ItemsRoute

@Serializable
private object AddItemRoute

@Serializable
private data class ItemDetailsRoute(val id: String)

@Serializable
private object SubscriptionsRoute

@Serializable
private data class SubscriptionEditRoute(val id: String? = null)

@Serializable
private object ProfileRoute

@Serializable
private object SettingsRoute

private enum class Tab(val route: Any, @StringRes val label: Int, @DrawableRes val icon: Int) {
    ITEMS(ItemsRoute, R.string.tab_items, R.drawable.ic_notification),
    SUBSCRIPTIONS(SubscriptionsRoute, R.string.subscriptions_title, R.drawable.ic_credit_card),
    PROFILE(ProfileRoute, R.string.profile_title, R.drawable.ic_person)
}

@Composable
fun OutDateApp() {
    val context = LocalContext.current
    val itemsViewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.factory(context))
    val itemsState by itemsViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentTab = currentEntry?.tab()

    NotificationPermissionRequest(shouldAsk = itemsState.hasItems)

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                AppNavigationBar(selected = currentTab, onSelect = navController::navigateToTab)
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ItemsRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = {
                if (isTabSwitch()) {
                    fadeIn(tween(TRANSITION_MILLIS))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(TRANSITION_MILLIS))
                }
            },
            exitTransition = {
                if (isTabSwitch()) {
                    fadeOut(tween(TRANSITION_MILLIS))
                } else {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(TRANSITION_MILLIS),
                        targetOffset = { it / 4 }
                    ) + fadeOut(tween(TRANSITION_MILLIS))
                }
            },
            popEnterTransition = {
                if (isTabSwitch()) {
                    fadeIn(tween(TRANSITION_MILLIS))
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(TRANSITION_MILLIS),
                        initialOffset = { it / 4 }
                    ) + fadeIn(tween(TRANSITION_MILLIS))
                }
            },
            popExitTransition = {
                if (isTabSwitch()) {
                    fadeOut(tween(TRANSITION_MILLIS))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(TRANSITION_MILLIS))
                }
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
                    categories = itemsState.categories,
                    initialCategoryId = itemsState.selectedCategoryId,
                    defaultNotifyDaysBefore = itemsState.defaultNotifyDaysBefore,
                    onCreateCategory = itemsViewModel::createCategory,
                    onSave = { draft ->
                        if (entry.isResumed()) {
                            itemsViewModel.addItem(draft)
                            navController.popBackStack()
                        }
                    },
                    onCancel = dropUnlessResumed { navController.popBackStack() }
                )
            }
            composable<ItemDetailsRoute>(
                deepLinks = listOf(navDeepLink<ItemDetailsRoute>(basePath = ITEM_DEEP_LINK))
            ) { entry ->
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
            composable<SubscriptionsRoute>(
                deepLinks = listOf(navDeepLink<SubscriptionsRoute>(basePath = SUBSCRIPTIONS_DEEP_LINK))
            ) { entry ->
                val subscriptionsViewModel: SubscriptionsViewModel =
                    viewModel(factory = SubscriptionsViewModel.factory(context))
                SubscriptionsScreen(
                    viewModel = subscriptionsViewModel,
                    onAddClick = dropUnlessResumed { navController.navigate(SubscriptionEditRoute()) },
                    onSubscriptionClick = { id ->
                        if (entry.isResumed()) navController.navigate(SubscriptionEditRoute(id))
                    }
                )
            }
            composable<SubscriptionEditRoute> { entry ->
                val route = entry.toRoute<SubscriptionEditRoute>()
                val editViewModel: SubscriptionEditViewModel =
                    viewModel(factory = SubscriptionEditViewModel.factory(context, route.id))
                SubscriptionEditScreen(
                    viewModel = editViewModel,
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    onDone = {
                        if (entry.isResumed()) navController.popBackStack()
                    }
                )
            }
            composable<ProfileRoute> {
                val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
                ProfileScreen(
                    viewModel = profileViewModel,
                    onSettingsClick = dropUnlessResumed { navController.navigate(SettingsRoute) }
                )
            }
            composable<SettingsRoute> {
                val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = dropUnlessResumed { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AppNavigationBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onSelect(tab) },
                icon = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) }
            )
        }
    }
}

private fun NavController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavBackStackEntry.tab(): Tab? = Tab.entries.firstOrNull { destination.hasRoute(it.route::class) }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.tab() != null && targetState.tab() != null

private fun NavBackStackEntry.isResumed(): Boolean = lifecycle.currentState == Lifecycle.State.RESUMED
