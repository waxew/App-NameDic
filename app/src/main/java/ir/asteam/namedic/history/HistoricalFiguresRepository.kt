package ir.asteam.namedic.history

import android.content.Context
import org.json.JSONArray

/**
 * مخزن آفلاین «بزرگان تاریخ ایران».
 *
 * تمام دادهٔ اصلی از فایل asset خوانده می‌شود تا کاربر برای مشاهدهٔ تاریخ به
 * اینترنت وابسته نباشد. اینترنت فقط در صورت لمس لینک «منبع» استفاده می‌شود.
 */
class HistoricalFiguresRepository(private val context: Context) {

    /** فهرست شخصیت‌ها فقط یک بار در طول عمر Repository از JSON خوانده می‌شود. */
    val figures: List<HistoricalFigure> by lazy { loadFigures() }

    /** دسته‌های واقعی موجود در داده؛ هیچ دستهٔ خالی در UI ساخته نمی‌شود. */
    val categories: List<String> by lazy {
        figures.map { it.categoryFa }.distinct().sorted()
    }

    /**
     * جستجو در نام فارسی/لاتین، دوره، نقش و متن خلاصه.
     * فیلتر دسته اختیاری است و مقدار null یعنی نمایش همهٔ دسته‌ها.
     */
    fun search(query: String, category: String? = null): List<HistoricalFigure> {
        val normalized = query.trim()
        return figures.filter { figure ->
            val categoryMatches = category == null || figure.categoryFa == category
            val queryMatches = normalized.isBlank() || listOf(
                figure.nameFa,
                figure.nameEn,
                figure.periodFa,
                figure.roleFa,
                figure.summaryFa,
                figure.highlightsFa.joinToString(" "),
            ).any { it.contains(normalized, ignoreCase = true) }
            categoryMatches && queryMatches
        }
    }

    /** فایل JSON را با API داخلی Android می‌خوانیم تا وابستگی serialization اضافه نشود. */
    private fun loadFigures(): List<HistoricalFigure> {
        val raw = context.assets.open("historical_figures.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val highlightsJson = item.getJSONArray("highlightsFa")
                val highlights = buildList {
                    for (highlightIndex in 0 until highlightsJson.length()) {
                        add(highlightsJson.getString(highlightIndex))
                    }
                }
                add(
                    HistoricalFigure(
                        id = item.getString("id"),
                        nameFa = item.getString("nameFa"),
                        nameEn = item.getString("nameEn"),
                        periodFa = item.getString("periodFa"),
                        categoryFa = item.getString("categoryFa"),
                        roleFa = item.getString("roleFa"),
                        yearsFa = item.getString("yearsFa"),
                        summaryFa = item.getString("summaryFa"),
                        highlightsFa = highlights,
                        sourceLabel = item.getString("sourceLabel"),
                        sourceUrl = item.getString("sourceUrl"),
                    ),
                )
            }
        }
    }
}
