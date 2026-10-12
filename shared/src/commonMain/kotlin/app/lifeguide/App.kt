package app.lifeguide

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.lifeguide.ui.AboutScreen
import app.lifeguide.ui.DetailScreen
import app.lifeguide.ui.DisclaimerDialog
import app.lifeguide.ui.HomeScreen
import app.lifeguide.ui.MainViewModel
import app.lifeguide.ui.theme.LifeGuideTheme
import kotlinx.serialization.Serializable

@Serializable private data object HomeRoute
@Serializable private data class DetailRoute(val id: String)
@Serializable private data object AboutRoute

@Composable
fun App(onDarkChanged: (Boolean) -> Unit = {}, env: () -> AppEnv) {
    val graph = remember { AppGraph.get(env) }
    val vm = viewModel { MainViewModel(graph) }
    val theme by vm.theme.collectAsStateWithLifecycle()
    val accepted by vm.disclaimerAccepted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    LifeGuideTheme(theme, onDarkChanged) {
        val nav = rememberNavController()
        NavHost(nav, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(vm, snackbar, onOpen = { nav.navigate(DetailRoute(it)) }, onAbout = { nav.navigate(AboutRoute) })
            }
            composable<DetailRoute> {
                DetailScreen(vm, it.toRoute<DetailRoute>().id, onShare = graph.env::share, onBack = { nav.popBackStack() })
            }
            composable<AboutRoute> {
                AboutScreen(vm, snackbar, onBack = { nav.popBackStack() })
            }
        }
        if (!accepted) DisclaimerDialog(onAccept = vm::acceptDisclaimer)
    }
}
