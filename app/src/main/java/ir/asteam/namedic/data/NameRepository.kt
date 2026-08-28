package ir.asteam.namedic.data

import android.content.Context
import ir.asteam.namedic.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * مخزن آفلاین داده‌ها.
 *
 * رکوردهای گردآوری‌شده در curated_names.json روی فیلدهای پژوهشی رکورد پایه
 * اولویت دارند، اما enrichment چندمنبعی پایه (تلفظ، منابع، داده واژگانی و ...)
 * در صورت خالی بودن فیلد curated حفظ می‌شود.
 */
class NameRepository(private val context: Context) {

    private data class SourceInfo(
        val title: String,
        val url: String,
    )

    /** رجیستری منابعی که شناسه آن‌ها داخل names_base.json ذخیره می‌شود. */
    private val sourceRegistry = mapOf(
        "nabidam_persian_names" to SourceInfo(
            "nabidam/persian-names (MIT)",
            "https://github.com/nabidam/persian-names",
        ),
        "farbodbj_pngt" to SourceInfo(
            "Persian Gender by Name / PNGT (Apache-2.0)",
            "https://github.com/farbodbj/persian-gender-by-name",
        ),
        "mehdi_iranian_names_female" to SourceInfo(
            "mehdi-haydari/iranianNames (MIT)",
            "https://github.com/mehdi-haydari/iranianNames",
        ),
        "mehdi_iranian_names_male" to SourceInfo(
            "mehdi-haydari/iranianNames (MIT)",
            "https://github.com/mehdi-haydari/iranianNames",
        ),
        "jadijadi_persianwords_boy" to SourceInfo(
            "jadijadi/persianwords (CC0-1.0)",
            "https://github.com/jadijadi/persianwords",
        ),
        "jadijadi_persianwords_girl" to SourceInfo(
            "jadijadi/persianwords (CC0-1.0)",
            "https://github.com/jadijadi/persianwords",
        ),
        "armanyazdi_persian_names_male" to SourceInfo(
            "armanyazdi/persian-names (MIT)",
            "https://github.com/armanyazdi/persian-names",
        ),
        "armanyazdi_persian_names_female" to SourceInfo(
            "armanyazdi/persian-names (MIT)",
            "https://github.com/armanyazdi/persian-names",
        ),
        "mohammadhejazirad_persian_names" to SourceInfo(
            "mohammadhejazirad/persian-names (MIT)",
            "https://github.com/mohammadhejazirad/persian-names",
        ),
        "wikidata_cc0" to SourceInfo(
            "Wikidata structured data (CC0)",
            "https://www.wikidata.org/",
        ),
        "wiktionary_kaikki" to SourceInfo(
            "Wiktionary via Kaikki/Wiktextract (CC BY-SA / GFDL)",
            "https://kaikki.org/dictionary/Persian/",
        ),
        "maani_dehkhoda_lexicon" to SourceInfo(
            "Maani/Dehkhoda-Lexicon (CC BY-SA 4.0)",
            "https://huggingface.co/datasets/Maani/Dehkhoda-Lexicon",
        ),
        "sahim_official" to SourceInfo(
            "سامانه تعاملی نام ثبت احوال",
            "https://sahim.sabteahval.ir/",
        ),
    )

    val cultures: List<CultureCategory> = listOf(
        CultureCategory(
            "iran_general",
            "فهرست عمومی ایران",
            "نام‌های ثبت‌شده در منابع عمومی ایران؛ این دسته ادعای ریشه زبانی ندارد",
        ),
        CultureCategory("persian", "فارسی", "نام‌های فارسی و نام‌های ثبت‌شده در منابع زبان فارسی"),
        CultureCategory("azerbaijani", "آذری", "نام‌های مرتبط با زبان و فرهنگ ترکی آذربایجانی ایران"),
        CultureCategory("kurdish", "کردی", "نام‌های مرتبط با زبان‌ها و فرهنگ‌های کردی ایران"),
        CultureCategory("gilaki", "گیلکی", "نام‌های مرتبط با زبان و فرهنگ گیلکی"),
        CultureCategory("mazandarani", "مازندرانی / طبری", "نام‌ها و میراث زبانی مازندران و طبری"),
        CultureCategory("luri", "لری و بختیاری", "نام‌های مرتبط با زبان‌ها و فرهنگ‌های لری و بختیاری"),
        CultureCategory("balochi", "بلوچی", "نام‌های مرتبط با زبان و فرهنگ بلوچی"),
        CultureCategory("talysh", "تالشی", "نام‌ها و میراث تالشی"),
        CultureCategory("tati", "تاتی", "نام‌ها و میراث تاتی"),
        CultureCategory("semnani", "سمنانی و گویش‌های مرکزی", "نام‌ها در حوزه سمنانی و گویش‌های ایرانی مرکزی"),
        CultureCategory("turkmen", "ترکمنی", "نام‌های مرتبط با زبان و فرهنگ ترکمنی"),
        CultureCategory("qashqai", "قشقایی", "نام‌های مرتبط با زبان و فرهنگ قشقایی"),
        CultureCategory(
            "arabic_iran",
            "عربی ایران",
            "نام‌های مستند در جوامع عرب ایرانی؛ ریشه و کاربرد جدا ثبت می‌شوند",
        ),
        CultureCategory("armenian_iran", "ارمنی ایران", "نام‌های مستند در جامعه ارمنی ایران"),
        CultureCategory("assyrian_iran", "آشوری / آرامی نو", "نام‌های مستند در جامعه آشوری ایران"),
        CultureCategory("ancient_iranian", "ایرانی باستان و میانه", "نام‌های تاریخی از دوره‌های ایران باستان و میانه"),
    )

    val names: List<NameEntry> by lazy { loadNames() }

    val heritageItems: List<HeritageItem> = listOf(
        HeritageItem(
            "rostam",
            "رستم",
            HeritageType.HERO,
            "پهلوان برجسته شاهنامه و یکی از شناخته‌شده‌ترین چهره‌های حماسی فرهنگ ایران.",
            "حماسی / شاهنامه",
            "شاهنامه فردوسی",
        ),
        HeritageItem(
            "arash",
            "آرش",
            HeritageType.HERO,
            "کمانگیر نامدار روایت‌های ایرانی که افسانه تعیین مرز ایران با تیر او پیوند خورده است.",
            "اساطیری / حماسی",
        ),
        HeritageItem(
            "kaveh",
            "کاوه آهنگر",
            HeritageType.HERO,
            "چهره حماسی قیام علیه ضحاک و نمادی از دادخواهی در روایت‌های ایرانی.",
            "اساطیری / شاهنامه",
            "شاهنامه فردوسی",
        ),
        HeritageItem(
            "gordafarid",
            "گردآفرید",
            HeritageType.HERO,
            "زن جنگاور و دلاور شاهنامه که در نبرد با سهراب شناخته می‌شود.",
            "حماسی / شاهنامه",
            "شاهنامه فردوسی",
        ),
        HeritageItem(
            "simorgh",
            "سیمرغ",
            HeritageType.MYTHICAL_CREATURE,
            "پرنده اساطیری و دانا در سنت ایرانی و شاهنامه؛ در داستان زال و رستم نقشی مهم دارد.",
            "اساطیری",
            "شاهنامه فردوسی",
        ),
        HeritageItem(
            "rakhsh",
            "رخش",
            HeritageType.CULTURAL_ANIMAL,
            "اسب نامدار رستم در شاهنامه و همراه او در بسیاری از ماجراهای حماسی.",
            "حماسی / شاهنامه",
            "شاهنامه فردوسی",
        ),
        HeritageItem(
            "shabdiz",
            "شبدیز",
            HeritageType.CULTURAL_ANIMAL,
            "اسب مشهور خسرو پرویز که در روایت‌های ادبی و تاریخی ایران شهرت یافته است.",
            "ساسانی / ادبی",
        ),
        HeritageItem(
            "homa",
            "هما",
            HeritageType.MYTHICAL_CREATURE,
            "پرنده‌ای خوش‌یمن در ادبیات و فرهنگ ایرانی که با سعادت و فرخندگی پیوند دارد.",
            "فرهنگی / ادبی",
        ),
    )

    fun search(
        query: String,
        gender: Gender? = null,
        cultureId: String? = null,
    ): List<NameEntry> {
        val q = normalize(query)
        return names.filter { entry ->
            val searchable = listOf(
                entry.name,
                entry.meaning,
                entry.lexicalMeaningFa,
                entry.lexicalAntonymsFa,
                entry.origin,
                entry.latin,
                entry.latinVariants.joinToString(" "),
                entry.tags.joinToString(" "),
            )
            val queryOk = q.isBlank() || searchable.any { normalize(it).contains(q) }
            val genderOk = gender == null || entry.gender == gender
            val cultureOk = cultureId == null || cultureId in entry.usageCultureIds
            queryOk && genderOk && cultureOk
        }
    }

    private fun loadNames(): List<NameEntry> {
        val merged = linkedMapOf<String, NameEntry>()
        loadBaseNames().forEach { merged[normalize(it.name)] = it }
        loadCuratedNames().forEach { curated ->
            val key = normalize(curated.name)
            val base = merged[key]
            merged[key] = if (base == null) curated else mergeBaseWithCurated(base, curated)
        }
        return merged.values.sortedBy { it.name }
    }

    /**
     * Curated data wins only where it actually has a value. This prevents a short
     * hand-curated record from deleting pronunciation/provenance/lexical enrichment
     * already collected for the same name from open structured sources.
     */
    private fun mergeBaseWithCurated(base: NameEntry, curated: NameEntry): NameEntry {
        val combinedSources = (base.sourceIds + curated.sourceIds).distinct()
        return curated.copy(
            gender = if (curated.gender == Gender.UNKNOWN) base.gender else curated.gender,
            meaning = curated.meaning.ifBlank { base.meaning },
            origin = curated.origin.ifBlank { base.origin },
            usageCultureIds = (base.usageCultureIds + curated.usageCultureIds).distinct(),
            latin = curated.latin.ifBlank { base.latin },
            latinVariants = (base.latinVariants + curated.latinVariants).filter { it.isNotBlank() }.distinct(),
            pronunciation = curated.pronunciation.ifBlank { base.pronunciation },
            lexicalMeaningFa = curated.lexicalMeaningFa.ifBlank { base.lexicalMeaningFa },
            lexicalAntonymsFa = curated.lexicalAntonymsFa.ifBlank { base.lexicalAntonymsFa },
            tags = (base.tags + curated.tags).distinct(),
            sourceIds = combinedSources,
            sourceTitle = sourceTitleFor(combinedSources),
            sourceUrl = curated.sourceUrl.ifBlank { base.sourceUrl.ifBlank { sourceUrlFor(combinedSources) } },
            notes = curated.notes.ifBlank { base.notes },
        )
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
                    val sourceIds = item.optJSONArray("sourceIds").toStringList()
                    val latin = item.optString("latin")
                    val latinVariants = (
                        listOf(latin) + item.optJSONArray("latinVariants").toStringList()
                    ).filter { it.isNotBlank() }.distinct()
                    val usageCultures = (
                        listOf("iran_general") + item.optJSONArray("usageCultureIds").toStringList()
                    ).distinct()
                    val meaning = item.optString("meaning")
                    val origin = item.optString("origin")
                    val pronunciation = item.optString("pronunciation")
                    val lexicalMeaningFa = item.optString("lexicalMeaningFa")
                    val lexicalAntonymsFa = item.optString("lexicalAntonymsFa")
                    val status = if (
                        meaning.isNotBlank() ||
                        origin.isNotBlank() ||
                        pronunciation.isNotBlank() ||
                        lexicalMeaningFa.isNotBlank() ||
                        usageCultures.any { it != "iran_general" }
                    ) {
                        VerificationStatus.CURATED
                    } else {
                        VerificationStatus.BASE_ONLY
                    }

                    add(
                        NameEntry(
                            name = name,
                            gender = parseGender(item.optString("gender")),
                            meaning = meaning,
                            origin = origin,
                            usageCultureIds = usageCultures,
                            latin = latin,
                            latinVariants = latinVariants,
                            pronunciation = pronunciation,
                            lexicalMeaningFa = lexicalMeaningFa,
                            lexicalAntonymsFa = lexicalAntonymsFa,
                            tags = item.optJSONArray("tags").toStringList(),
                            sourceIds = sourceIds,
                            sourceTitle = sourceTitleFor(sourceIds),
                            sourceUrl = sourceUrlFor(sourceIds),
                            verificationStatus = status,
                            notes = item.optString("notes"),
                        )
                    )
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
                    val sourceIds = item.optJSONArray("sourceIds").toStringList()
                    val explicitSourceTitle = item.optString("sourceTitle")
                    val explicitSourceUrl = item.optString("sourceUrl")
                    val latin = item.optString("latin")
                    val variants = (
                        listOf(latin) + item.optJSONArray("latinVariants").toStringList()
                    ).filter { it.isNotBlank() }.distinct()

                    add(
                        NameEntry(
                            name = item.getString("name"),
                            gender = parseGender(item.optString("gender")),
                            meaning = item.optString("meaning"),
                            origin = item.optString("origin"),
                            usageCultureIds = item.optJSONArray("usageCultureIds").toStringList(),
                            latin = latin,
                            latinVariants = variants,
                            pronunciation = item.optString("pronunciation"),
                            lexicalMeaningFa = item.optString("lexicalMeaningFa"),
                            lexicalAntonymsFa = item.optString("lexicalAntonymsFa"),
                            tags = item.optJSONArray("tags").toStringList(),
                            sourceIds = sourceIds,
                            sourceTitle = explicitSourceTitle.ifBlank { sourceTitleFor(sourceIds) },
                            sourceUrl = explicitSourceUrl.ifBlank { sourceUrlFor(sourceIds) },
                            verificationStatus = parseVerification(item.optString("verificationStatus")),
                            notes = item.optString("notes"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun sourceTitleFor(sourceIds: List<String>): String {
        val titles = sourceIds.mapNotNull { sourceRegistry[it]?.title }.distinct()
        return when {
            titles.isNotEmpty() -> titles.joinToString("، ")
            sourceIds.isNotEmpty() -> sourceIds.distinct().joinToString("، ")
            else -> "فهرست عمومی نام‌های ایران"
        }
    }

    private fun sourceUrlFor(sourceIds: List<String>): String =
        sourceIds.firstNotNullOfOrNull { sourceRegistry[it]?.url }.orEmpty()

    private fun readAssetOrNull(fileName: String): String? =
        runCatching {
            context.assets.open(fileName)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }.getOrNull()

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) add(optString(i))
        }.filter { it.isNotBlank() }
    }

    private fun parseGender(value: String): Gender = when (value.uppercase()) {
        "MALE", "BOY" -> Gender.MALE
        "FEMALE", "GIRL" -> Gender.FEMALE
        "UNISEX" -> Gender.UNISEX
        else -> Gender.UNKNOWN
    }

    private fun parseVerification(value: String): VerificationStatus =
        runCatching { VerificationStatus.valueOf(value.uppercase()) }
            .getOrDefault(VerificationStatus.NEEDS_REVIEW)

    private fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')
        .replace("‌", "")
        .replace(" ", "")
}
