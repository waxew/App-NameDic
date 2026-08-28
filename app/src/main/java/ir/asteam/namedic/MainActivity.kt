package ir.asteam.namedic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ir.asteam.namedic.ui.NameDicRedesignApp
import ir.asteam.namedic.ui.theme.NameDicTheme

/**
 * Activity واحد برنامه.
 *
 * نسخه 1.3.0 رابط انتخاب‌محور جدید را اجرا می‌کند؛ این رابط تفکیک دختر/پسر،
 * کشف اسم، علاقه‌مندی، جستجو، فرهنگ‌های دارای داده و Drawer استاندارد پروژه
 * را روی همان دیتابیس آفلاین قرار می‌دهد.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NameDicTheme { NameDicRedesignApp() } }
    }
}
