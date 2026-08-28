package ir.asteam.namedic.data

import android.content.Context
import ir.asteam.namedic.model.*
import org.json.JSONArray
import org.json.JSONObject

/** مخزن آفلاین داده‌ها؛ رکورد پژوهش‌شده بر رکورد پایه اولویت دارد. */
class NameRepository(private val context: Context) {
    val cultures: List<CultureCategory> = listOf(
        CultureCategory("persian", "فارسی", "نام‌های فارسی و رایج در فرهنگ فارسی"),
        CultureCategory("azerbaijani", "آذری", "نام‌های رایج در فرهنگ ترکی آذربایجانی ایران"),
        CultureCategory("kurdish", "کردی", "نام‌های رایج در فرهنگ‌ها و گویش‌های کردی ایران"),
        CultureCategory("gilaki", "گیلکی", "نام‌های رایج در فرهنگ گیلان"),
        CultureCategory("mazandarani", "مازندرانی / طبری", "نام‌ها و میراث زبانی مازندران و طبری"),
        CultureCategory("luri", "لری و بختیاری", "نام‌های رایج در فرهنگ‌های لری و بختیاری"),
        CultureCategory("balochi", "بلوچی", "نام‌های رایج در فرهنگ بلوچ ایران"),
        CultureCategory("talysh", "تالشی", "نام‌ها و میراث تالشی"),
        CultureCategory("tati", "تاتی", "نام‌ها و میراث تاتی"),
        CultureCategory("semnani", "سمنانی و گویش‌های مرکزی", "نام‌ها در حوزه سمنانی و گویش‌های ایرانی مرکزی"),
        CultureCategory("turkmen", "ترکمنی", "نام‌های رایج در میان ترکمن‌های ایران"),
        CultureCategory("qashqai", "قشقایی", "نام‌های رایج در فرهنگ قشقایی"),
        CultureCategory("arabic_iran", "عربی ایران", "نام‌های رایج در جوامع عرب ایرانی؛ ریشه و کاربرد جدا ثبت می‌شوند"),
        CultureCategory("armenian_iran", "ارمنی ایران", "نام‌های رایج در جامعه ارمنی ایران"),
        CultureCategory("assyrian_iran", "آشوری / آرامی نو", "نام‌های رایج در جامعه آشوری ایران"),
        CultureCategory("ancient_iranian", "ایرانی باستان و میانه", "نام‌های تاریخی از دوره‌های ایران باستان و میانه"),
    )

    val names: List<NameEntry> by lazy { loadNames() }

    val heritageItems: List<HeritageItem> = listOf(
        HeritageItem("rostam", "رستم", HeritageType.HERO, "پهلوان برجسته شاهنامه و یکی از شناخته‌شده‌ترین چهره‌های حماسی فرهنگ ایران.", "حماسی / شاهنامه", "شاهنامه فردوسی"),
        HeritageItem("arash", "آرش", HeritageType.HERO, "کمانگیر نامدار روایت‌های ایرانی که افسانه تعیین مرز ایران با تیر او پیوند خورده است.", "اساطیری / حماسی"),
        HeritageItem("kaveh", "کاوه آهنگر", HeritageType.HERO, "چهره حماسی قیام علیه ضحاک و نمادی از دادخواهی در روایت‌های ایرانی.", "اساطیری / شاهنامه", "شاهنامه فردوسی"),
        HeritageItem("gordafarid", "گردآفرید", HeritageType.HERO, "زن جنگاور و دلاور شاهنامه که در نبرد با سهراب شناخته می‌شود.", "حماسی / شاهنامه", "شاهنامه فردوسی"),
        HeritageItem("simorgh", "سیمرغ", HeritageType.MYTHICAL_CREATURE, "پرنده اساطیری و دانا در سنت ایرانی و شاهنامه؛ در داستان زال و رستم نقشی مهم دارد.", "اساطیری", "شاهنامه فردوسی"),
        HeritageItem("rakhsh", "رخش", HeritageType.CULTURAL_ANIMAL, "اسب نامدار رستم در شاهنامه و همراه او در بسیاری از ماجراهای حماسی.", "حماسی / شاهنامه", "شاهنامه فردوسی"),
        HeritageItem("shabdiz", "شبدیز", HeritageType.CULTURAL_ANIMAL, "اسب مشهور خسرو پرویز که در روایت‌های ادبی و تاریخی ایران شهرت یافته است.", "ساسانی / ادبی"),
        HeritageItem("homa", "هما", HeritageType.MYTHICAL_CREATURE, "پرنده‌ای خوش‌یمن در ادبیات و فرهنگ ایرانی که با سعادت و فرخندگی پیوند دارد.", "فرهنگی / ادبی"),
    )

    fun search(query: String, gender: Gender? = null, cultureId: String? = null): List<NameEntry> {
        val q = normalize(query)
        return names.filter { entry ->
            val queryOk = q.isBlank() || listOf(entry.name, entry.meaning, entry.origin, entry.latin, entry.tags.joinToString(" ")).any { normalize(it).contains(q) }
            val genderOk = gender == null || entry.gender == gender
            val cultureOk = cultureId == null || cultureId in entry.usageCultureIds
            queryOk && genderOk && cultureOk
        }
    }

    private fun loadNames(): List<NameEntry> {
        val merged = linkedMapOf<String, NameEntry>()
        loadBaseNames().forEach { merged[normalize(it.name)] = it }
        loadCuratedNames().forEach { merged[normalize(it.name)] = it }
        return merged.values.sortedBy { it.name }
    }

    private fun loadBaseNames(): List<NameEntry> {
        val text = readAssetOrNull("names_base.json") ?: return emptyList()
        return runCatching {
            val root = JSONObject(text)
            buildList {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = root.optJSONObject(key) ?: JSONObject()
                    val name = item.optString("name", key).ifBlank { key }
                    add(NameEntry(name, parseGender(item.optString("gender")), "معنی و ریشه این نام هنوز در بخش پژوهش در حال تکمیل است.", "نیازمند بررسی", emptyList(), sourceTitle = "Persian Names dataset (MIT)", verificationStatus = VerificationStatus.BASE_ONLY))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadCuratedNames(): List<NameEntry> {
        val text = readAssetOrNull("curated_names.json") ?: return emptyList()
        return runCatching {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(NameEntry(
                        name = item.getString("name"),
                        gender = parseGender(item.optString("gender")),
                        meaning = item.optString("meaning", "در حال تکمیل"),
                        origin = item.optString("origin", "نیازمند بررسی"),
                        usageCultureIds = item.optJSONArray("usageCultureIds").toStringList(),
                        latin = item.optString("latin"), pronunciation = item.optString("pronunciation"),
                        tags = item.optJSONArray("tags").toStringList(), sourceTitle = item.optString("sourceTitle"),
                        sourceUrl = item.optString("sourceUrl"), verificationStatus = parseVerification(item.optString("verificationStatus")),
                        notes = item.optString("notes"),
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readAssetOrNull(fileName: String): String? = runCatching { context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrNull()
    private fun JSONArray?.toStringList(): List<String> { if (this == null) return emptyList(); return buildList { for (i in 0 until length()) add(optString(i)) }.filter { it.isNotBlank() } }
    private fun parseGender(value: String): Gender = when (value.uppercase()) { "MALE", "BOY" -> Gender.MALE; "FEMALE", "GIRL" -> Gender.FEMALE; "UNISEX" -> Gender.UNISEX; else -> Gender.UNKNOWN }
    private fun parseVerification(value: String): VerificationStatus = runCatching { VerificationStatus.valueOf(value.uppercase()) }.getOrDefault(VerificationStatus.NEEDS_REVIEW)
    private fun normalize(value: String): String = value.trim().lowercase().replace('ي','ی').replace('ك','ک').replace("‌","").replace(" ","")
}
