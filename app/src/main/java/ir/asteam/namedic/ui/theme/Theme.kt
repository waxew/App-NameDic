package ir.asteam.namedic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * پالت روشن و خانوادگی نسل دوم.
 * رنگ‌های اصلی خنثی نگه داشته شده‌اند و دختر/پسر در کارت‌های محتوایی رنگ مستقل دارند.
 */
private val NameDicColorScheme = lightColorScheme(
    primary = Color(0xFF67507A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E7F7),
    onPrimaryContainer = Color(0xFF2D2037),
    secondary = Color(0xFF39756B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF3ED),
    onSecondaryContainer = Color(0xFF143C35),
    tertiary = Color(0xFF8B5C63),
    background = Color(0xFFFFF8EE),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5EFE8),
    onSurface = Color(0xFF242126),
    onSurfaceVariant = Color(0xFF6D676F),
    outline = Color(0xFF9B949D),
)

@Composable
fun NameDicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NameDicColorScheme, content = content)
}
