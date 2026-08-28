package ir.asteam.namedic.data

import ir.asteam.namedic.model.CultureCategory
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry
import kotlin.math.absoluteValue

/**
 * لایهٔ کاربردی انتخاب نام.
 *
 * این کلاس منطق «کشف نام» را از UI جدا می‌کند تا صفحه‌ها مجبور نباشند روی
 * فهرست خام ۲۶هزارنامی کار کنند. هیچ معنی یا ریشه‌ای در این لایه حدس زده نمی‌شود.
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

    /** فقط دسته‌هایی که حداقل یک نام واقعی دارند وارد رابط کاربری می‌شوند. */
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

    /** شناسه‌های داخلی دیتابیس هرگز مستقیماً به کاربر نمایش داده نمی‌شوند. */
    fun cultureTitles(entry: NameEntry): List<String> = entry.usageCultureIds
        .filterNot { it == "iran_general" }
        .mapNotNull { id -> repository.cultures.firstOrNull { it.id == id }?.titleFa }
        .distinct()

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

    /** پیشنهاد روز قطعی است و در طول یک روز بی‌دلیل عوض نمی‌شود. */
    fun featuredName(seed: Int): NameEntry? {
        val pool = repository.names.filter {
            it.gender in setOf(Gender.FEMALE, Gender.MALE) && it.hasReadableInfo()
        }
        if (pool.isEmpty()) return repository.names.firstOrNull()
        return pool[seed.absoluteValue % pool.size]
    }

    /**
     * مشابه‌ها با امتیاز قابل توضیح مرتب می‌شوند: جنسیت، حرف اول، فرهنگ مشترک
     * و میزان اطلاعات قابل نمایش.
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

    /**
     * جستجوی خالی عمداً نتیجه‌ای برنمی‌گرداند تا صفحه جستجو با ورود اولیه
     * مجبور به ساخت و نمایش ده‌ها هزار ردیف نشود.
     */
    fun search(
        query: String,
        gender: Gender? = null,
        enrichedOnly: Boolean = false,
    ): List<NameEntry> {
        if (query.isBlank()) return emptyList()
        return repository.search(query, gender).filter {
            !enrichedOnly || it.hasReadableInfo()
        }
    }

    private fun NameEntry.hasReadableInfo(): Boolean =
        meaning.isNotBlank() || origin.isNotBlank() || lexicalMeaningFa.isNotBlank()
}
