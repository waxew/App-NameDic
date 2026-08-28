package ir.asteam.namedic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// پالت گرم و فرهنگی با فیروزه‌ای، کرم و طلایی.
private val NameDicColorScheme = lightColorScheme(
    primary = Color(0xFF0F5E6E), onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E9EF), onPrimaryContainer = Color(0xFF08343D),
    secondary = Color(0xFF8A5A18), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1D8A8), onSecondaryContainer = Color(0xFF3F2909),
    tertiary = Color(0xFF6B5B45), background = Color(0xFFFFFBF5), surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFF0E9DF), onSurface = Color(0xFF211F1B), outline = Color(0xFF7A746B),
)

@Composable fun NameDicTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = NameDicColorScheme, content = content) }
