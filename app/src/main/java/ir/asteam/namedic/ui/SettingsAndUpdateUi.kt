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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.asteam.namedic.BuildConfig
import ir.asteam.namedic.data.UpdateChecker
import ir.asteam.namedic.data.UpdateInfo
import kotlinx.coroutines.launch

private const val RELEASE_PAGE = "https://github.com/waxew/App-NameDic/releases/latest"

/** تنظیمات واقعی و مرتبط با رفتار برنامه. */
@Composable
fun SettingsScreen(
    autoUpdateCheck: Boolean,
    onAutoUpdateCheckChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("اطلاع‌رسانی نسخه جدید", fontWeight = FontWeight.Black)
                    Text(
                        "هنگام باز شدن برنامه، یک بررسی سبک انجام شود و فقط اگر نسخه جدیدتر وجود داشت پیام داخل برنامه نمایش داده شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoUpdateCheck, onCheckedChange = onAutoUpdateCheckChange)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                    Text("به‌روزرسانی برنامه", fontWeight = FontWeight.Black)
                }
                Text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                resultText?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                FilledTonalButton(
                    onClick = {
                        if (checking) return@FilledTonalButton
                        checking = true
                        resultText = "در حال بررسی…"
                        scope.launch {
                            val info = UpdateChecker.check()
                            checking = false
                            when {
                                info == null -> resultText = "بررسی نسخه انجام نشد؛ اتصال اینترنت را بررسی کن. استفاده آفلاین برنامه بدون مشکل ادامه دارد."
                                info.latestVersionCode > BuildConfig.VERSION_CODE -> {
                                    availableUpdate = info
                                    resultText = "نسخه ${info.latestVersionName} در دسترس است."
                                }
                                else -> {
                                    availableUpdate = null
                                    resultText = "همین حالا آخرین نسخه را داری."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !checking,
                ) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (checking) "در حال بررسی" else "بررسی نسخه جدید")
                }

                if (availableUpdate != null) {
                    OutlinedButton(
                        onClick = {
                            val url = availableUpdate?.downloadUrl.orEmpty().ifBlank { RELEASE_PAGE }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null)
                        Spacer(Modifier.width(6.dp))
                        Text("باز کردن صفحه دریافت")
                    }
                }
            }
        }
    }
}

/**
 * بررسی خودکار نسخه جدید. شکست شبکه یا خاموش بودن تنظیم، هیچ اثری روی مسیر
 * آفلاین برنامه ندارد.
 */
@Composable
fun UpdateNotificationHost(enabled: Boolean) {
    val context = LocalContext.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled || dismissed) return@LaunchedEffect
        val info = UpdateChecker.check()
        if (info != null && info.latestVersionCode > BuildConfig.VERSION_CODE) {
            update = info
        }
    }

    val info = update
    if (info != null && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("نسخه جدید ${info.latestVersionName}") },
            text = { Text(info.messageFa.ifBlank { "نسخه جدید نام‌نامه ایران منتشر شده است." }) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = info.downloadUrl.ifBlank { RELEASE_PAGE }
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                ) { Text("دریافت نسخه") }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text("بعداً") }
            },
        )
    }
}

/** آیتم مستقل «اشتراک با دوستان» مطابق Drawer مشترک پروژه. */
@Composable
fun ShareAppDrawerItem() {
    val context = LocalContext.current
    NavigationDrawerItem(
        label = { Text("اشتراک با دوستان") },
        icon = { Icon(Icons.Rounded.Share, null) },
        selected = false,
        onClick = {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "نام‌نامه ایران")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "نام‌نامه ایران؛ جستجو، مقایسه و شناخت نام‌های ایرانی و بزرگان تاریخ ایران.\n$RELEASE_PAGE",
                )
            }
            runCatching { context.startActivity(Intent.createChooser(share, "اشتراک برنامه")) }
        },
    )
}
