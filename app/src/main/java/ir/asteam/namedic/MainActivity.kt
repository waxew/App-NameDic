package ir.asteam.namedic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ir.asteam.namedic.ui.NameDicAppV2
import ir.asteam.namedic.ui.theme.NameDicTheme

/** Activity واحد؛ رابط نسل دوم روی همان دیتابیس آفلاین اجرا می‌شود. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NameDicTheme { NameDicAppV2() } }
    }
}
