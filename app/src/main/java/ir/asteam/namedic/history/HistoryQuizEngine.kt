package ir.asteam.namedic.history

/** سؤال تولیدشده از دادهٔ منبع‌دار شخصیت‌های تاریخی. */
data class HistoryQuizQuestion(
    val id: String,
    val promptFa: String,
    val optionsFa: List<String>,
    val correctAnswerFa: String,
    val explanationFa: String,
)

/**
 * موتور خالص آزمون ایران‌شناسی.
 *
 * سؤال‌ها از رکوردهای واقعی [HistoricalFigure] ساخته می‌شوند تا متن آزمون و
 * صفحهٔ شخصیت‌ها از یک منبع داده تغذیه شوند. ترتیب گزینه‌ها قطعی ولی متنوع است
 * و نیازی به اینترنت یا Random غیرقابل تست ندارد.
 */
class HistoryQuizEngine(private val figures: List<HistoricalFigure>) {

    fun buildQuiz(limit: Int = 10): List<HistoryQuizQuestion> {
        val usable = figures.filter {
            it.nameFa.isNotBlank() && it.roleFa.isNotBlank() && it.periodFa.isNotBlank()
        }
        if (usable.size < 4) return emptyList()

        return usable
            .take(limit.coerceIn(1, usable.size))
            .mapIndexed { index, figure ->
                val distractors = buildList {
                    var offset = 1
                    while (size < 3 && offset < usable.size + 1) {
                        val candidate = usable[(index + offset) % usable.size].nameFa
                        if (candidate != figure.nameFa && candidate !in this) add(candidate)
                        offset += 1
                    }
                }
                val baseOptions = (distractors + figure.nameFa).distinct()
                val correctPosition = index % baseOptions.size
                val withoutCorrect = baseOptions.filterNot { it == figure.nameFa }.toMutableList()
                withoutCorrect.add(correctPosition.coerceAtMost(withoutCorrect.size), figure.nameFa)

                HistoryQuizQuestion(
                    id = "figure-${figure.id}",
                    promptFa = "کدام شخصیت در دورهٔ «${figure.periodFa}» با نقش «${figure.roleFa}» شناخته می‌شود؟",
                    optionsFa = withoutCorrect,
                    correctAnswerFa = figure.nameFa,
                    explanationFa = figure.summaryFa,
                )
            }
    }
}
