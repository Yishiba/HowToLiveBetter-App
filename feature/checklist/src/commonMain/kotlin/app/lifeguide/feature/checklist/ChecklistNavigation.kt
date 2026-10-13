package app.lifeguide.feature.checklist

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** 首页 Tab：清单 */
@Serializable
data object ChecklistRoute

fun NavGraphBuilder.checklistScreen(onOpenEntry: (id: String) -> Unit, onOpenSettings: () -> Unit) {
    composable<ChecklistRoute> { ChecklistScreen(onOpenEntry = onOpenEntry, onOpenSettings = onOpenSettings) }
}

val checklistModule = module {
    viewModelOf(::ChecklistViewModel)
}
