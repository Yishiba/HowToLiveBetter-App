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

/** [onOpenEntry]、[onOpenSection] 也给正文里的交叉引用用：点「见第 X 节第 Y 条」就推一个新页面，返回键回来 */
fun NavGraphBuilder.entryScreen(onOpenEntry: (id: String) -> Unit, onOpenSection: (n: String) -> Unit, onBack: () -> Unit) {
    composable<EntryRoute> {
        EntryScreen(id = it.toRoute<EntryRoute>().id, onOpenEntry = onOpenEntry, onOpenSection = onOpenSection, onBack = onBack)
    }
}

val entryModule = module {
    viewModelOf(::EntryViewModel)
}
