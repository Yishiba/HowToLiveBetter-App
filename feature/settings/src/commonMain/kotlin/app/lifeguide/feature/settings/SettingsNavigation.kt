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

/** 术语表 */
@Serializable
data object GlossaryRoute

fun NavController.navigateToGlossary() = navigate(GlossaryRoute)

fun NavGraphBuilder.glossaryScreen(onBack: () -> Unit) {
    composable<GlossaryRoute> { GlossaryScreen(onBack = onBack) }
}

fun NavGraphBuilder.settingsScreen(snackbar: SnackbarHostState, onOpenGlossary: () -> Unit, onBack: () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(snackbar = snackbar, onOpenGlossary = onOpenGlossary, onBack = onBack) }
}

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
    viewModelOf(::OnboardingViewModel)
}
