package ir.asteam.namedic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ir.asteam.namedic.ui.NameDicApp
import ir.asteam.namedic.ui.theme.NameDicTheme

/** Activity واحد؛ تمام UI با Compose رسم می‌شود. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NameDicTheme { NameDicApp() } }
    }
}
