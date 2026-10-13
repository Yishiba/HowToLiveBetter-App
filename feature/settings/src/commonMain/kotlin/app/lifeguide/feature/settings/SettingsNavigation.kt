package app.lifeguide.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

@Serializable
data object SettingsRoute

fun NavController.navigateToSettings() = navigate(SettingsRoute)

fun NavGraphBuilder.settingsScreen(snackbar: SnackbarHostState, onBack: () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(snackbar = snackbar, onBack = onBack) }
}

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
