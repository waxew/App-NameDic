package ir.asteam.namedic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ir.asteam.namedic.ui.NameDicRedesignApp
import ir.asteam.namedic.ui.theme.NameDicTheme

/**
 * Activity واحد برنامه.
 *
 * نسخه 2.0.0 رابط یکپارچهٔ نام‌نامه را اجرا می‌کند: انتخاب و مقایسهٔ نام،
 * پیشنهادگر پیشرفتهٔ آفلاین، فرهنگ‌های دارای داده، مرکز تاریخ ایران با شخصیت‌ها
 * و خط زمانی و آزمون، تنظیمات و بررسی اختیاری نسخهٔ جدید. نگه داشتن یک Activity
 * اصلی باعث می‌شود state و Back navigation در Compose قابل کنترل و ساده بماند.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NameDicTheme { NameDicRedesignApp() } }
    }
}
