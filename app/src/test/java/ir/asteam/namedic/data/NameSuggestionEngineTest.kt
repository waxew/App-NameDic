package ir.asteam.namedic.data

import ir.asteam.namedic.model.CultureCategory
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست‌های خالص موتور پیشنهادگر؛ بدون Android و بدون فایل asset اجرا می‌شوند. */
class NameSuggestionEngineTest {

    private val cultures = listOf(
        CultureCategory("persian", "فارسی", ""),
        CultureCategory("kurdish", "کردی", ""),
    )

    private val names = listOf(
        NameEntry(
            name = "آرمان",
            gender = Gender.MALE,
            meaning = "آرزو و هدف",
            origin = "فارسی",
            usageCultureIds = listOf("persian"),
            sourceIds = listOf("curated"),
        ),
        NameEntry(
            name = "آرین",
            gender = Gender.MALE,
            meaning = "",
            origin = "",
            usageCultureIds = listOf("persian"),
        ),
        NameEntry(
            name = "ژینا",
            gender = Gender.FEMALE,
            meaning = "زندگی",
            origin = "کردی",
            usageCultureIds = listOf("kurdish"),
            sourceIds = listOf("curated"),
        ),
    )

    @Test
    fun filtersByGenderLetterAndReadableInfo() {
        val engine = NameSuggestionEngine(names, cultures)
        val result = engine.suggest(
            NameSuggestionCriteria(
                gender = Gender.MALE,
                firstLetter = "آ",
                requireReadableInfo = true,
            ),
        )

        assertEquals(listOf("آرمان"), result.map { it.name })
    }

    @Test
    fun searchesMeaningAndNormalizesPersianCharacters() {
        val engine = NameSuggestionEngine(names, cultures)
        val result = engine.suggest(NameSuggestionCriteria(keyword = "زندگي"))

        assertEquals("ژینا", result.first().name)
    }

    @Test
    fun cultureListOnlyContainsUsedSpecificCultures() {
        val engine = NameSuggestionEngine(names, cultures)
        val titles = engine.availableCultures().map { it.titleFa }

        assertTrue("فارسی" in titles)
        assertTrue("کردی" in titles)
    }
}
