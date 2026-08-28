package ir.asteam.namedic.data

import ir.asteam.namedic.model.CultureCategory
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry
import kotlin.math.absoluteValue

/**
 * لایهٔ کاربردی انتخاب نام.
 *
 * این کلاس عمداً منطق «کشف نام» را از UI جدا می‌کند تا صفحه‌ها مجبور نباشند
 * روی فهرست خام ۲۶هزارنامی کار کنند. خروجی‌ها فقط از دادهٔ موجود در
 * [NameRepository] ساخته می‌شوند و هیچ ریشه/معنی تازه‌ای حدس زده نمی‌شود.
 */
class NameDiscoveryEngine(private val repository: NameRepository) {

    data class CultureStats(
        val culture: CultureCategory,
        val total: Int,
        val female: Int,
        val male: Int,
        val unisex: Int,
    )

    val totalNames: Int get() = repository.names.size
    val femaleCount: Int get() = repository.names.count { it.gender == Gender.FEMALE }
    val maleCount: Int get() = repository.names.count { it.gender == Gender.MALE }
    val unisexCount: Int get() = repository.names.count { it.gender == Gender.UNISEX }

    /**
     * مرور نام‌ها با فیلتر جنسیت/فرهنگ. UNKNOWN فقط در حالت «همه» دیده می‌شود.
     */
    fun browse(
        gender: Gender? = null,
        cultureId: String? = null,
        enrichedOnly: Boolean = false,
    ): List<NameEntry> = repository.names.filter { entry ->
        val genderOk = gender == null || entry.gender == gender
        val cultureOk = cultureId == null || cultureId in entry.usageCultureIds
        val enrichmentOk = !enrichedOnly || entry.hasReadableInfo()
        genderOk && cultureOk && enrichmentOk
    }

    /**
     * فقط فرهنگ‌هایی را برمی‌گرداند که واقعاً رکورد دارند؛ بنابراین UI دیگر
     * کاربر را وارد صفحه‌های خالی نمی‌کند.
     */
    fun availableCultures(): List<CultureStats> = repository.cultures
        .asSequence()
        .filterNot { it.id == "iran_general" }
        .map { culture ->
            val names = repository.names.filter { culture.id in it.usageCultureIds }
            CultureStats(
                culture = culture,
                total = names.size,
                female = names.count { it.gender == Gender.FEMALE },
                male = names.count { it.gender == Gender.MALE },
                unisex = names.count { it.gender == Gender.UNISEX },
            )
        }
        .filter { it.total > 0 }
        .sortedByDescending { it.total }
        .toList()

    /**
     * استخر پیشنهاد سریع؛ نام‌های دارای توضیح/ریشه/دادهٔ واژگانی جلوتر هستند.
     */
    fun discoveryPool(gender: Gender): List<NameEntry> = repository.names
        .asSequence()
        .filter { it.gender == gender }
        .filter { it.hasReadableInfo() || it.latin.isNotBlank() }
        .sortedWith(
            compareByDescending<NameEntry> { it.meaning.isNotBlank() }
                .thenByDescending { it.origin.isNotBlank() }
                .thenByDescending { it.lexicalMeaningFa.isNotBlank() }
                .thenByDescending { it.sourceIds.size }
                .thenBy { it.name },
        )
        .toList()

    /**
     * نام پیشنهادی روز را به‌صورت قطعی از روی seed می‌سازد تا در یک روز مدام
     * تغییر نکند. این تابع هیچ ادعای «محبوبیت» ایجاد نمی‌کند.
     */
    fun featuredName(seed: Int): NameEntry? {
        val pool = repository.names.filter {
            it.gender in setOf(Gender.FEMALE, Gender.MALE) && it.hasReadableInfo()
        }
        if (pool.isEmpty()) return repository.names.firstOrNull()
        return pool[seed.absoluteValue % pool.size]
    }

    /**
     * نام‌های مشابه را با امتیاز شباهت ساده و قابل توضیح رتبه‌بندی می‌کند:
     * جنسیت، حرف آغازین، اشتراک فرهنگ و وجود اطلاعات توصیفی.
     */
    fun relatedTo(entry: NameEntry, limit: Int = 8): List<NameEntry> = repository.names
        .asSequence()
        .filter { it.name != entry.name }
        .map { candidate ->
            var score = 0
            if (candidate.gender == entry.gender) score += 5
            if (candidate.name.firstOrNull() == entry.name.firstOrNull()) score += 4
            score += candidate.usageCultureIds.intersect(entry.usageCultureIds.toSet()).size * 3
            if (candidate.meaning.isNotBlank()) score += 2
            if (candidate.origin.isNotBlank()) score += 1
            if (candidate.lexicalMeaningFa.isNotBlank()) score += 1
            candidate to score
        }
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Pair<NameEntry, Int>> { it.second }.thenBy { it.first.name })
        .take(limit)
        .map { it.first }
        .toList()

    fun search(
        query: String,
        gender: Gender? = null,
        enrichedOnly: Boolean = false,
    ): List<NameEntry> = repository.search(query, gender).filter {
        !enrichedOnly || it.hasReadableInfo()
    }

    private fun NameEntry.hasReadableInfo(): Boolean =
        meaning.isNotBlank() || origin.isNotBlank() || lexicalMeaningFa.isNotBlank()
}
