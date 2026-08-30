package ir.asteam.namedic.history

import android.content.Context
import org.json.JSONArray

/**
 * مخزن آفلاین «بزرگان تاریخ ایران».
 *
 * داده‌ها عمداً در چند asset مستقل نگهداری می‌شوند تا مجموعهٔ پایه و توسعه‌های
 * محتوایی بعدی بدون بازنویسی فایل اصلی قابل نگهداری باشند. تمام فایل‌ها در
 * زمان ساخت توسط validator بررسی می‌شوند و در زمان اجرا به یک فهرست واحد تبدیل
 * می‌شوند. اینترنت فقط هنگام لمس لینک منبع استفاده می‌شود.
 */
class HistoricalFiguresRepository(private val context: Context) {

    /**
     * ترتیب فایل‌ها پایدار است تا نمایش اولیه بین اجراهای برنامه بی‌دلیل تغییر
     * نکند. اضافه شدن بسته‌های محتوایی بعدی فقط به این فهرست نیاز دارد.
     */
    private val assetFiles = listOf(
        "historical_figures.json",
        "historical_figures_extra.json",
    )

    /** فهرست شخصیت‌ها فقط یک بار در طول عمر Repository از assetها خوانده می‌شود. */
    val figures: List<HistoricalFigure> by lazy {
        assetFiles.flatMap(::loadFile)
    }

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

    /** یک فایل JSON را بدون افزودن dependency جدید serialization تبدیل می‌کند. */
    private fun loadFile(fileName: String): List<HistoricalFigure> {
        val raw = context.assets.open(fileName)
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
