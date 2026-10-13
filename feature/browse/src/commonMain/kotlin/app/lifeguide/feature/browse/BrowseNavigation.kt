package app.lifeguide.feature.browse

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

@Serializable
data object BrowseRoute

fun NavGraphBuilder.browseScreen(
    snackbar: SnackbarHostState,
    onOpenEntry: (id: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<BrowseRoute> {
        BrowseScreen(snackbar = snackbar, onOpen = onOpenEntry, onSettings = onOpenSettings)
    }
}

val browseModule = module {
    viewModelOf(::BrowseViewModel)
}
