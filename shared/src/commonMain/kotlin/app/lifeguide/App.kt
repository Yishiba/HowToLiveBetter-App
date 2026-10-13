package app.lifeguide

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.lifeguide.core.data.ThemeMode
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.theme.LifeGuideTheme
import app.lifeguide.feature.checklist.ChecklistRoute
import app.lifeguide.feature.checklist.checklistScreen
import app.lifeguide.feature.entry.entryScreen
import app.lifeguide.feature.entry.navigateToEntry
import app.lifeguide.feature.find.FindRoute
import app.lifeguide.feature.find.findScreen
import app.lifeguide.feature.find.navigateToSection
import app.lifeguide.feature.find.sectionScreen
import app.lifeguide.feature.settings.OnboardingScreen
import app.lifeguide.feature.settings.navigateToSettings
import app.lifeguide.feature.settings.settingsScreen
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KClass

/** 底部的两个 Tab，见 docs/ux.md「结构」 */
private enum class Tab(val label: String, val icon: ImageVector, val route: Any, val routeClass: KClass<*>) {
    Checklist("清单", Icons.Checklist, ChecklistRoute, ChecklistRoute::class),
    Find("查找", Icons.Search, FindRoute, FindRoute::class),
}

/**
 * 全 App 的根：主题、首次启动页、两个 Tab 与它们推进去的页面、全局提示。调用前要先 [app.lifeguide.di.initKoin]。
 * @param onDarkChanged 手动选了和系统相反的配色时，状态栏图标颜色要跟着翻，这件事只有平台层能做
 */
@Composable
fun App(onDarkChanged: (Boolean) -> Unit = {}) {
    val vm = koinViewModel<AppViewModel>()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val onboarded by vm.onboarded.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val dark = when (theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    LaunchedEffect(dark) { onDarkChanged(dark) }

    LifeGuideTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (onboarded) {
                null -> Box(Modifier.fillMaxSize())
                false -> OnboardingScreen()
                true -> Main(snackbar)
            }
        }
    }
}

@Composable
private fun Main(snackbar: SnackbarHostState) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val tab = Tab.entries.firstOrNull { t -> current?.destination?.hasRoute(t.routeClass) == true }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        // 只在两个 Tab 的首页显示底栏，推进去的页面全屏
        bottomBar = { if (tab != null) BottomBar(tab, nav) },
    ) { pad ->
        NavHost(nav, startDestination = ChecklistRoute, modifier = Modifier.padding(pad)) {
            checklistScreen(onOpenEntry = nav::navigateToEntry, onOpenSettings = nav::navigateToSettings)
            findScreen(onOpenSection = nav::navigateToSection, onOpenEntry = nav::navigateToEntry)
            sectionScreen(onOpenEntry = nav::navigateToEntry, onBack = { nav.popBackStack() })
            entryScreen(onOpenSection = nav::navigateToSection, onBack = { nav.popBackStack() })
            settingsScreen(snackbar, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun BottomBar(selected: Tab, nav: NavHostController) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        for (t in Tab.entries) {
            NavigationBarItem(
                selected = t == selected,
                onClick = {
                    // 标准 Tab 切换：回到起点、不叠页面、各 Tab 保留自己的滚动位置
                    nav.navigate(t.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(t.icon, null) },
                label = { Text(t.label) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}
