package ir.asteam.namedic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ir.asteam.namedic.ui.NameDicRedesignApp
import ir.asteam.namedic.ui.theme.NameDicTheme

/**
 * Activity واحد برنامه.
 *
 * نسخه 1.5.0 رابط انتخاب‌محور نام‌نامه را اجرا می‌کند؛ این رابط تفکیک دختر/پسر،
 * کشف اسم، علاقه‌مندی، جستجو، مقایسه چند اسم، فرهنگ‌های دارای داده، بخش آفلاین
 * «بزرگان تاریخ ایران» و Drawer استاندارد پروژه را روی همان ساختار محلی اجرا می‌کند.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NameDicTheme { NameDicRedesignApp() } }
    }
}
