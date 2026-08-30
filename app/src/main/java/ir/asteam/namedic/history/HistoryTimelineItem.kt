package ir.asteam.namedic.history

/**
 * یک بازهٔ مهم در خط زمانی تاریخ ایران.
 *
 * startSort/endSort فقط برای ترتیب زمانی داخلی‌اند؛ متن قابل نمایش از yearsFa
 * می‌آید تا دربارهٔ تاریخ‌های تقریبی یا مورد اختلاف، دقت کاذب ایجاد نشود.
 */
data class HistoryTimelineItem(
    val id: String,
    val titleFa: String,
    val yearsFa: String,
    val eraFa: String,
    val summaryFa: String,
    val startSort: Int,
    val endSort: Int,
    val sourceLabel: String,
    val sourceUrl: String,
)
