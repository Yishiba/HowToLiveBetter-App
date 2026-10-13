package app.lifeguide.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 配色取自上游检索页（VitePress 默认主题），App 和网页看起来是同一本书
@Immutable
data class Tones(
    val brand: Color, val brandSoft: Color,
    val green: Color, val greenSoft: Color,
    val yellow: Color, val yellowSoft: Color,
    val red: Color, val redSoft: Color,
    val gray: Color, val graySoft: Color,
    val mark: Color,
)

private val LightTones = Tones(
    brand = Color(0xFF3451B2), brandSoft = Color(0x24646CFF),
    green = Color(0xFF18794E), greenSoft = Color(0x2410B981),
    yellow = Color(0xFF915930), yellowSoft = Color(0x24EAB308),
    red = Color(0xFFB8272C), redSoft = Color(0x24F43F5E),
    gray = Color(0xFF565A5F), graySoft = Color(0x248E96AA),
    mark = Color(0x55FACC15),
)
private val DarkTones = Tones(
    brand = Color(0xFFA8B1FF), brandSoft = Color(0x29646CFF),
    green = Color(0xFF3DD68C), greenSoft = Color(0x2910B981),
    yellow = Color(0xFFF9B44E), yellowSoft = Color(0x29EAB308),
    red = Color(0xFFF66F81), redSoft = Color(0x29F43F5E),
    gray = Color(0xFFA4A8AE), graySoft = Color(0x298E96AA),
    mark = Color(0x66CA8A04),
)

val LocalTones = staticCompositionLocalOf { LightTones }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3451B2), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E6FB), onPrimaryContainer = Color(0xFF1F2E73),
    secondaryContainer = Color(0xFFE3E6FB), onSecondaryContainer = Color(0xFF1F2E73),
    background = Color.White, onBackground = Color(0xFF3C3C43),
    surface = Color.White, onSurface = Color(0xFF3C3C43),
    surfaceVariant = Color(0xFFF6F6F7), onSurfaceVariant = Color(0xC73C3C43),
    surfaceContainer = Color(0xFFF6F6F7), surfaceContainerLow = Color(0xFFF9F9FA),
    surfaceContainerHigh = Color(0xFFF1F1F2), surfaceContainerHighest = Color(0xFFEBEBEC),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFFC2C2C4), outlineVariant = Color(0xFFE2E2E3),
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA8B1FF), onPrimary = Color(0xFF1B1B1F),
    primaryContainer = Color(0xFF2F3770), onPrimaryContainer = Color(0xFFDDE1FF),
    secondaryContainer = Color(0xFF2F3770), onSecondaryContainer = Color(0xFFDDE1FF),
    background = Color(0xFF1B1B1F), onBackground = Color(0xDBFFFFF5),
    surface = Color(0xFF1B1B1F), onSurface = Color(0xDBFFFFF5),
    surfaceVariant = Color(0xFF202127), onSurfaceVariant = Color(0x99EBEBF5),
    surfaceContainer = Color(0xFF202127), surfaceContainerLow = Color(0xFF1E1E22),
    surfaceContainerHigh = Color(0xFF2B2B2F), surfaceContainerHighest = Color(0xFF323238),
    surfaceContainerLowest = Color(0xFF161618),
    outline = Color(0xFF4A4A50), outlineVariant = Color(0xFF2E2E32),
)

@Composable
fun LifeGuideTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTones provides if (darkTheme) DarkTones else LightTones) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
    }
}
