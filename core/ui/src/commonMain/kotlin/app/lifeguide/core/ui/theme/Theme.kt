package app.lifeguide.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 配色只有三种角色：一个品牌绿；争议用的红；其余全是灰阶。见 docs/ux.md「视觉」。
 * 页面底色是浅灰，卡片是白色，靠底色差和细边分层，不用阴影。
 */
@Immutable
data class ExtraColors(
    /** 搜索关键词高亮 */
    val mark: Color,
    /** 有争议 */
    val danger: Color,
    /** 卡片细边 */
    val hairline: Color,
)

private val LightExtra = ExtraColors(mark = Color(0x55FACC15), danger = Color(0xFFC2413A), hairline = Color(0xFFE6E8E5))
private val DarkExtra = ExtraColors(mark = Color(0x66CA8A04), danger = Color(0xFFF2857D), hairline = Color(0xFF2C302D))

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F7D57), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F2EA), onPrimaryContainer = Color(0xFF1B5238),
    secondaryContainer = Color(0xFFE4F2EA), onSecondaryContainer = Color(0xFF1B5238),
    background = Color(0xFFF4F5F3), onBackground = Color(0xFF1D211F),
    surface = Color(0xFFF4F5F3), onSurface = Color(0xFF1D211F),
    surfaceVariant = Color(0xFFECEEEB), onSurfaceVariant = Color(0xFF656C67),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF4F5F3), surfaceContainerHigh = Color(0xFFECEEEB),
    surfaceContainerHighest = Color(0xFFE4E7E3),
    outline = Color(0xFFC9CDC9), outlineVariant = Color(0xFFE6E8E5),
    error = Color(0xFFC2413A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7CC59A), onPrimary = Color(0xFF0D2A1B),
    primaryContainer = Color(0xFF1E3A2B), onPrimaryContainer = Color(0xFFBFE5CC),
    secondaryContainer = Color(0xFF1E3A2B), onSecondaryContainer = Color(0xFFBFE5CC),
    background = Color(0xFF121413), onBackground = Color(0xFFE4E7E4),
    surface = Color(0xFF121413), onSurface = Color(0xFFE4E7E4),
    surfaceVariant = Color(0xFF1F2321), onSurfaceVariant = Color(0xFFA2A9A4),
    surfaceContainerLowest = Color(0xFF1A1D1B), surfaceContainerLow = Color(0xFF1A1D1B),
    surfaceContainer = Color(0xFF121413), surfaceContainerHigh = Color(0xFF242826),
    surfaceContainerHighest = Color(0xFF2C302E),
    outline = Color(0xFF454A47), outlineVariant = Color(0xFF2C302D),
    error = Color(0xFFF2857D),
)

// 中文正文行高放宽一些，标题收紧
private val Base = Typography()
private val AppTypography = Typography(
    headlineSmall = Base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
)

@Composable
fun LifeGuideTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtraColors provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, typography = AppTypography, content = content)
    }
}

