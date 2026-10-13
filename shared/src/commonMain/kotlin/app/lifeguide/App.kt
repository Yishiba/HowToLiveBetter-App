package app.lifeguide

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.lifeguide.core.data.ThemeMode
import app.lifeguide.core.ui.theme.LifeGuideTheme
import app.lifeguide.feature.browse.BrowseRoute
import app.lifeguide.feature.browse.browseScreen
import app.lifeguide.feature.entry.entryScreen
import app.lifeguide.feature.entry.navigateToEntry
import app.lifeguide.feature.settings.DisclaimerDialog
import app.lifeguide.feature.settings.navigateToSettings
import app.lifeguide.feature.settings.settingsScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * 全 App 的根：主题、导航、全局提示、首次启动的免责声明。调用前要先 [app.lifeguide.di.initKoin]。
 * @param onDarkChanged 手动选了和系统相反的配色时，状态栏图标颜色要跟着翻，这件事只有平台层能做
 */
@Composable
fun App(onDarkChanged: (Boolean) -> Unit = {}) {
    val vm = koinViewModel<AppViewModel>()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val accepted by vm.disclaimerAccepted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val dark = when (theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    LaunchedEffect(dark) { onDarkChanged(dark) }

    LifeGuideTheme(darkTheme = dark) {
        val nav = rememberNavController()
        NavHost(nav, startDestination = BrowseRoute) {
            browseScreen(snackbar, onOpenEntry = nav::navigateToEntry, onOpenSettings = nav::navigateToSettings)
            entryScreen(onBack = { nav.popBackStack() })
            settingsScreen(snackbar, onBack = { nav.popBackStack() })
        }
        if (!accepted) DisclaimerDialog(onAccept = vm::acceptDisclaimer)
    }
}
