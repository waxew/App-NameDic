package ir.asteam.namedic.history

import android.content.Context
import org.json.JSONArray

/**
 * مخزن آفلاین خط زمانی تاریخ ایران.
 *
 * داده از asset مستقل خوانده می‌شود تا Timeline بدون اینترنت کار کند و در CI
 * نیز بتوان ساختار JSON و لینک منبع را جداگانه اعتبارسنجی کرد.
 */
class HistoryTimelineRepository(private val context: Context) {

    val items: List<HistoryTimelineItem> by lazy {
        loadItems().sortedBy { it.startSort }
    }

    val eras: List<String> by lazy {
        items.map { it.eraFa }.distinct()
    }

    fun filter(era: String?): List<HistoryTimelineItem> =
        if (era == null) items else items.filter { it.eraFa == era }

    private fun loadItems(): List<HistoryTimelineItem> {
        val raw = context.assets.open("history_timeline.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    HistoryTimelineItem(
                        id = item.getString("id"),
                        titleFa = item.getString("titleFa"),
                        yearsFa = item.getString("yearsFa"),
                        eraFa = item.getString("eraFa"),
                        summaryFa = item.getString("summaryFa"),
                        startSort = item.getInt("startSort"),
                        endSort = item.getInt("endSort"),
                        sourceLabel = item.getString("sourceLabel"),
                        sourceUrl = item.getString("sourceUrl"),
                    ),
                )
            }
        }
    }
}
