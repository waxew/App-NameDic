package ir.asteam.namedic.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val latestVersionCode: Int, val latestVersionName: String, val downloadUrl: String, val messageFa: String)

/** بررسی سبک بروزرسانی؛ شکست شبکه روی عملکرد آفلاین برنامه اثری ندارد. */
object UpdateChecker {
    private const val UPDATE_URL = "https://raw.githubusercontent.com/waxew/App-NameDic/main/update.json"
    suspend fun fetchLatest(): UpdateInfo? = runCatching {
        val connection = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 5_000; readTimeout = 5_000; useCaches = false
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            UpdateInfo(json.getInt("latestVersionCode"), json.getString("latestVersionName"), json.optString("downloadUrl"), json.optString("messageFa", "نسخه جدید منتشر شده است."))
        } finally { connection.disconnect() }
    }.getOrNull()
}
