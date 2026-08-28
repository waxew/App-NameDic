package ir.asteam.namedic.model

/** جنسیت ثبت‌شده؛ UNKNOWN یعنی منبع پایه به بررسی بیشتر نیاز دارد. */
enum class Gender(val titleFa: String) { MALE("پسرانه"), FEMALE("دخترانه"), UNISEX("مشترک"), UNKNOWN("نیازمند بررسی") }

/** سطح اعتبار اطلاعات توصیفی نام. */
enum class VerificationStatus(val titleFa: String) { VERIFIED("بررسی‌شده"), CURATED("گردآوری‌شده"), BASE_ONLY("فهرست پایه"), NEEDS_REVIEW("نیازمند منبع") }

/** دسته فرهنگی/زبانی؛ این دسته با ریشه واژه یک مفهوم نیست. */
data class CultureCategory(val id: String, val titleFa: String, val subtitleFa: String)

/** مدل اصلی هر نام در فرهنگ‌نامه. */
data class NameEntry(
    val name: String,
    val gender: Gender,
    val meaning: String,
    val origin: String,
    val usageCultureIds: List<String>,
    val latin: String = "",
    val pronunciation: String = "",
    val tags: List<String> = emptyList(),
    val sourceTitle: String = "",
    val sourceUrl: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.NEEDS_REVIEW,
    val notes: String = "",
)

data class HeritageItem(val id: String, val title: String, val type: HeritageType, val summary: String, val era: String = "", val sourceTitle: String = "")
enum class HeritageType(val titleFa: String) { HERO("قهرمان و شخصیت"), MYTHICAL_CREATURE("موجود اساطیری"), CULTURAL_ANIMAL("جانور فرهنگی") }
