package app.lifeguide.feature.entry

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** [id] 和网页锚点 #e-{sec}-{n} 是同一个键 */
@Serializable
data class EntryRoute(val id: String)

fun NavController.navigateToEntry(id: String) = navigate(EntryRoute(id))

fun NavGraphBuilder.entryScreen(onOpenSection: (n: String) -> Unit, onBack: () -> Unit) {
    composable<EntryRoute> { EntryScreen(id = it.toRoute<EntryRoute>().id, onOpenSection = onOpenSection, onBack = onBack) }
}

val entryModule = module {
    viewModelOf(::EntryViewModel)
}
