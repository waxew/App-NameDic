package ir.asteam.namedic.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.asteam.namedic.model.NameEntry

/**
 * اکشن‌های اشتراک‌گذاری و گزارش ایراد برای صفحهٔ جزئیات یک نام.
 * هیچ ارسال شبکه‌ای در پس‌زمینه انجام نمی‌شود؛ هر دو عمل با Intent و اقدام صریح
 * کاربر ادامه پیدا می‌کنند.
 */
@Composable
fun NameShareReportActions(entry: NameEntry, cultureTitles: List<String>) {
    val context = LocalContext.current

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ابزارهای این نام", fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(
                "اطلاعات را با دیگران به اشتراک بگذار یا اگر داده‌ای نیاز به اصلاح دارد گزارش آماده کن.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        val shareText = buildShareText(entry, cultureTitles)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "نام ${entry.name} — نام‌نامه ایران")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری نام"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Share, null)
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک")
                }

                OutlinedButton(
                    onClick = {
                        val subject = Uri.encode("گزارش داده نام ${entry.name} — نام‌نامه ایران")
                        val body = Uri.encode(buildReportBody(entry, cultureTitles))
                        val mailUri = Uri.parse("mailto:AS.Support.info@Gmail.com?subject=$subject&body=$body")
                        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, mailUri)) }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.BugReport, null)
                    Spacer(Modifier.width(6.dp))
                    Text("گزارش ایراد")
                }
            }
        }
    }
}

/** بخش شفافیت داده برای صفحهٔ About. */
@Composable
fun DataTransparencySection() {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.DataObject, null, tint = MaterialTheme.colorScheme.primary)
                Text("شفافیت داده‌ها", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Text(
                "نام‌نامه بین «معنی مستقیم نام»، «ریشه»، «فرهنگ/فهرست استفاده» و «معنی واژهٔ هم‌نوشت» تفاوت می‌گذارد و هیچ‌کدام را از روی دیگری حدس نمی‌زند.",
                lineHeight = 24.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Verified, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "اگر معنی یا ریشه خالی باشد، یعنی دادهٔ مستقیم و منبع‌دار کافی هنوز به آن رکورد وصل نشده؛ نه اینکه نام نامعتبر باشد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "اطلاعات تاریخی نیز در فایل مستقل نگهداری می‌شود و به معنی یا ریشهٔ نام‌ها تبدیل نمی‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildShareText(entry: NameEntry, cultureTitles: List<String>): String = buildString {
    appendLine("${entry.name} — نام‌نامه ایران")
    appendLine("جنسیت: ${entry.gender.titleFa}")
    if (entry.latin.isNotBlank()) appendLine("لاتین: ${entry.latin}")
    if (entry.meaning.isNotBlank()) appendLine("معنی: ${entry.meaning}")
    if (entry.origin.isNotBlank()) appendLine("ریشه: ${entry.origin}")
    if (cultureTitles.isNotEmpty()) appendLine("فرهنگ/فهرست: ${cultureTitles.joinToString("، ")}")
    if (entry.lexicalMeaningFa.isNotBlank()) {
        appendLine("واژهٔ هم‌نوشت: ${entry.lexicalMeaningFa}")
    }
    append("ارسال‌شده از برنامه نام‌نامه ایران")
}

private fun buildReportBody(entry: NameEntry, cultureTitles: List<String>): String = buildString {
    appendLine("نام: ${entry.name}")
    appendLine("جنسیت فعلی: ${entry.gender.titleFa}")
    appendLine("معنی فعلی: ${entry.meaning.ifBlank { "ثبت نشده" }}")
    appendLine("ریشه فعلی: ${entry.origin.ifBlank { "ثبت نشده" }}")
    appendLine("فرهنگ/فهرست: ${cultureTitles.ifEmpty { listOf("ثبت نشده") }.joinToString("، ")}")
    if (entry.sourceUrl.isNotBlank()) appendLine("منبع رکورد: ${entry.sourceUrl}")
    appendLine()
    appendLine("نوع ایراد یا پیشنهاد اصلاح:")
    appendLine()
    appendLine("منبع پیشنهادی برای اصلاح (در صورت وجود):")
}
