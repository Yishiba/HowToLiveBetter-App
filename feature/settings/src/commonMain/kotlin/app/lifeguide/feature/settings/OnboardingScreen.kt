package app.lifeguide.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class OnboardingViewModel(private val prefs: UserPreferences) : ViewModel() {
    val profile = prefs.profile.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggle(v: String) = viewModelScope.launch { prefs.setProfile(profile.value.let { if (v in it) it - v else it + v }) }

    fun finish() = viewModelScope.launch { prefs.finishOnboarding() }
}

/**
 * 首次启动只有这一页：一句话说清这是什么、免责说明、一个可以跳过的问题。
 * 不做问卷：书里唯一的个人维度就是人群标签，见 docs/ux.md「依据」。
 */
@Composable
fun OnboardingScreen(vm: OnboardingViewModel = koinViewModel()) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(40.dp))
            Text("高性价比人生指南", style = MaterialTheme.typography.headlineSmall)
            Text(
                "每一条都写明花掉什么、换回什么、证据多硬。不用全做，先挑最划算的几条。",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text("以下哪些符合你？", style = MaterialTheme.typography.titleMedium)
            Text(
                "只用来决定孕期、带娃、照顾老人这类建议要不要放进你的清单。可以不选，以后在设置里改。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProfileChips(profile, { vm.toggle(it) })
            Spacer(Modifier.height(12.dp))
            Text(DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { vm.finish() }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(48.dp)) {
            Text(if (profile.isEmpty()) "跳过，开始看" else "开始看")
        }
    }
}
