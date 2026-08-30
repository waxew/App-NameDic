package ir.asteam.namedic.data

import ir.asteam.namedic.model.CultureCategory
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry

/**
 * معیارهای پیشنهاد نام در نسخه 2.0.
 *
 * همهٔ معیارها اختیاری‌اند و فقط روی داده‌ای که واقعاً در corpus وجود دارد
 * اعمال می‌شوند. این مدل هیچ ویژگی فرهنگی، ریشه یا معنی را از روی املای نام
 * حدس نمی‌زند.
 */
data class NameSuggestionCriteria(
    val gender: Gender? = null,
    val firstLetter: String = "",
    val keyword: String = "",
    val cultureId: String? = null,
    val requireReadableInfo: Boolean = true,
)

/**
 * موتور خالص و آفلاین پیشنهاد نام.
 *
 * کلاس Android dependency ندارد تا بتوان منطق مرتب‌سازی و فیلتر را با تست واحد
 * بررسی کرد. امتیاز فقط برای مرتب‌سازی نتایج منطبق استفاده می‌شود و به کاربر
 * به عنوان «کیفیت نام» یا «محبوبیت» نمایش داده نمی‌شود.
 */
class NameSuggestionEngine(
    private val names: List<NameEntry>,
    private val cultures: List<CultureCategory>,
) {

    /** فهرست فرهنگ‌هایی که واقعاً در رکوردهای نام استفاده شده‌اند. */
    fun availableCultures(): List<CultureCategory> {
        val usedIds = names.flatMap { it.usageCultureIds }.toSet()
        return cultures
            .filter { it.id != "iran_general" && it.id in usedIds }
            .sortedBy { it.titleFa }
    }

    /** حروف اول موجود در داده برای جنسیت انتخاب‌شده؛ هیچ حرف ساختگی نمایش داده نمی‌شود. */
    fun availableFirstLetters(gender: Gender? = null): List<String> = names
        .asSequence()
        .filter { gender == null || it.gender == gender }
        .mapNotNull { entry -> entry.name.trim().firstOrNull()?.toString() }
        .map(::normalizePersian)
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .toList()

    /**
     * پیشنهادها را ابتدا سخت‌گیرانه فیلتر و سپس با معیارهای قابل توضیح مرتب می‌کند.
     * limit برای جلوگیری از ساخت هزاران کارت Compose در یک صفحه اعمال می‌شود.
     */
    fun suggest(criteria: NameSuggestionCriteria, limit: Int = 80): List<NameEntry> {
        val normalizedLetter = normalizePersian(criteria.firstLetter.trim())
        val normalizedKeyword = normalizePersian(criteria.keyword.trim())

        return names
            .asSequence()
            .filter { criteria.gender == null || it.gender == criteria.gender }
            .filter {
                normalizedLetter.isBlank() || normalizePersian(it.name).startsWith(normalizedLetter)
            }
            .filter {
                criteria.cultureId == null || criteria.cultureId in it.usageCultureIds
            }
            .filter {
                !criteria.requireReadableInfo || it.hasReadableInfo()
            }
            .filter { entry ->
                normalizedKeyword.isBlank() || entry.searchableText().contains(normalizedKeyword)
            }
            .map { it to score(it, criteria, normalizedKeyword) }
            .sortedWith(
                compareByDescending<Pair<NameEntry, Int>> { it.second }
                    .thenBy { normalizePersian(it.first.name) },
            )
            .take(limit.coerceIn(1, 250))
            .map { it.first }
            .toList()
    }

    /** تعداد کل رکوردهایی که با فیلترها منطبق‌اند؛ برای متن UI بدون ساخت لیست بزرگ. */
    fun countMatches(criteria: NameSuggestionCriteria): Int {
        val expanded = suggest(criteria, limit = 250)
        if (expanded.size < 250) return expanded.size

        val normalizedLetter = normalizePersian(criteria.firstLetter.trim())
        val normalizedKeyword = normalizePersian(criteria.keyword.trim())
        return names.count { entry ->
            (criteria.gender == null || entry.gender == criteria.gender) &&
                (normalizedLetter.isBlank() || normalizePersian(entry.name).startsWith(normalizedLetter)) &&
                (criteria.cultureId == null || criteria.cultureId in entry.usageCultureIds) &&
                (!criteria.requireReadableInfo || entry.hasReadableInfo()) &&
                (normalizedKeyword.isBlank() || entry.searchableText().contains(normalizedKeyword))
        }
    }

    private fun score(
        entry: NameEntry,
        criteria: NameSuggestionCriteria,
        normalizedKeyword: String,
    ): Int {
        var score = 0

        // غنای داده فقط ترتیب نمایش را بهتر می‌کند و ادعای «بهتر بودن» نام نیست.
        if (entry.meaning.isNotBlank()) score += 8
        if (entry.origin.isNotBlank()) score += 5
        if (entry.lexicalMeaningFa.isNotBlank()) score += 3
        if (entry.latin.isNotBlank()) score += 2
        score += entry.sourceIds.size.coerceAtMost(5)

        if (criteria.cultureId != null && criteria.cultureId in entry.usageCultureIds) score += 4
        if (criteria.gender != null && entry.gender == criteria.gender) score += 3

        if (normalizedKeyword.isNotBlank()) {
            val normalizedName = normalizePersian(entry.name)
            val normalizedMeaning = normalizePersian(entry.meaning)
            val normalizedOrigin = normalizePersian(entry.origin)
            when {
                normalizedName == normalizedKeyword -> score += 30
                normalizedName.startsWith(normalizedKeyword) -> score += 18
                normalizedName.contains(normalizedKeyword) -> score += 12
            }
            if (normalizedMeaning.contains(normalizedKeyword)) score += 10
            if (normalizedOrigin.contains(normalizedKeyword)) score += 7
        }

        return score
    }

    private fun NameEntry.hasReadableInfo(): Boolean =
        meaning.isNotBlank() || origin.isNotBlank() || lexicalMeaningFa.isNotBlank()

    private fun NameEntry.searchableText(): String = normalizePersian(
        listOf(
            name,
            latin,
            latinVariants.joinToString(" "),
            meaning,
            origin,
            lexicalMeaningFa,
            tags.joinToString(" "),
        ).joinToString(" "),
    )
}

/**
 * یک نرمال‌سازی کوچک برای تفاوت ک/ك و ی/ي و فاصلهٔ مجازی.
 * هدف موتور جستجو است، نه تغییر متن اصلی که به کاربر نمایش داده می‌شود.
 */
internal fun normalizePersian(value: String): String = value
    .replace('ي', 'ی')
    .replace('ى', 'ی')
    .replace('ك', 'ک')
    .replace("\u200c", "")
    .trim()
    .lowercase()
