package app.lifeguide.feature.find

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Tab：查找 */
@Serializable
data object FindRoute

/** 一节的条目列表，[n] 是节号 */
@Serializable
data class SectionRoute(val n: String)

fun NavController.navigateToSection(n: String) = navigate(SectionRoute(n))

fun NavGraphBuilder.findScreen(onOpenSection: (n: String) -> Unit, onOpenEntry: (id: String) -> Unit) {
    composable<FindRoute> { FindScreen(onOpenSection = onOpenSection, onOpenEntry = onOpenEntry) }
}

fun NavGraphBuilder.sectionScreen(onOpenEntry: (id: String) -> Unit, onBack: () -> Unit) {
    composable<SectionRoute> { SectionScreen(n = it.toRoute<SectionRoute>().n, onOpenEntry = onOpenEntry, onBack = onBack) }
}

val findModule = module {
    viewModelOf(::FindViewModel)
    viewModelOf(::SectionViewModel)
}
