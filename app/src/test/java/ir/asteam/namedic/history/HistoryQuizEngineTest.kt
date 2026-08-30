package ir.asteam.namedic.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست می‌کند هر سؤال دقیقاً یک پاسخ درست در میان گزینه‌های یکتا داشته باشد. */
class HistoryQuizEngineTest {

    private val figures = listOf(
        figure("a", "الف", "هخامنشی", "شاه"),
        figure("b", "ب", "ساسانی", "فرمانروا"),
        figure("c", "پ", "صفوی", "بنیان‌گذار"),
        figure("d", "ت", "قاجار", "اصلاح‌گر"),
        figure("e", "ث", "سامانی", "دانشمند"),
    )

    @Test
    fun generatedQuestionsContainOneCorrectUniqueAnswer() {
        val questions = HistoryQuizEngine(figures).buildQuiz(5)

        assertEquals(5, questions.size)
        questions.forEach { question ->
            assertEquals(question.optionsFa.size, question.optionsFa.distinct().size)
            assertEquals(1, question.optionsFa.count { it == question.correctAnswerFa })
            assertTrue(question.optionsFa.size >= 4)
        }
    }

    private fun figure(id: String, name: String, period: String, role: String) = HistoricalFigure(
        id = id,
        nameFa = name,
        nameEn = name,
        periodFa = period,
        categoryFa = "آزمون",
        roleFa = role,
        yearsFa = "",
        summaryFa = "خلاصهٔ $name",
        highlightsFa = listOf("نکته"),
        sourceLabel = "test",
        sourceUrl = "https://example.com/$id",
    )
}
