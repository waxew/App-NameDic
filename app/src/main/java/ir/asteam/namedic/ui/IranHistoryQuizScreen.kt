package ir.asteam.namedic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.asteam.namedic.history.HistoricalFiguresRepository
import ir.asteam.namedic.history.HistoryQuizEngine
import ir.asteam.namedic.history.HistoryQuizQuestion

/** آزمون آفلاین ساخته‌شده از همان دادهٔ شخصیت‌های تاریخی برنامه. */
@Composable
fun IranHistoryQuizScreen() {
    val context = LocalContext.current
    val repository = remember { HistoricalFiguresRepository(context) }
    val questions = remember { HistoryQuizEngine(repository.figures).buildQuiz(10) }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("برای ساخت آزمون، حداقل چهار شخصیت تاریخی لازم است.")
        }
        return
    }

    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

    fun reset() {
        index = 0
        score = 0
        selectedAnswer = null
        finished = false
    }

    if (finished) {
        QuizResult(
            score = score,
            total = questions.size,
            onReset = ::reset,
        )
        return
    }

    val question = questions[index]
    QuizQuestion(
        question = question,
        index = index,
        total = questions.size,
        score = score,
        selectedAnswer = selectedAnswer,
        onSelect = { answer ->
            if (selectedAnswer == null) {
                selectedAnswer = answer
                if (answer == question.correctAnswerFa) score += 1
            }
        },
        onNext = {
            if (index == questions.lastIndex) {
                finished = true
            } else {
                index += 1
                selectedAnswer = null
            }
        },
    )
}

@Composable
private fun QuizQuestion(
    question: HistoryQuizQuestion,
    index: Int,
    total: Int,
    score: Int,
    selectedAnswer: String?,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("سؤال ${index + 1} از $total", fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("امتیاز: $score", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            ElevatedCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Surface(
                        Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Help, null, Modifier.size(34.dp))
                        }
                    }
                    Text(
                        question.promptFa,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                    )
                }
            }
        }

        question.optionsFa.forEach { option ->
            item(key = option) {
                val answered = selectedAnswer != null
                val correct = option == question.correctAnswerFa
                val chosen = option == selectedAnswer
                val label = when {
                    answered && correct -> "✓ $option"
                    answered && chosen && !correct -> "✕ $option"
                    else -> option
                }

                if (answered && correct) {
                    FilledTonalButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                    ) { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !answered,
                    ) { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                }
            }
        }

        if (selectedAnswer != null) {
            item {
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (selectedAnswer == question.correctAnswerFa) "پاسخ درست" else "پاسخ درست: ${question.correctAnswerFa}",
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Text(question.explanationFa, lineHeight = 24.sp)
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(if (index + 1 == total) "دیدن نتیجه" else "سؤال بعدی")
                }
            }
        }
    }
}

@Composable
private fun QuizResult(score: Int, total: Int, onReset: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(50.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("پایان آزمون", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("$score پاسخ درست از $total سؤال", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "آزمون از اطلاعات شخصیت‌های تاریخی همین نسخه ساخته شده است؛ برای مرور جواب‌ها به تب شخصیت‌ها برگرد.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        FilledTonalButton(onClick = onReset) {
            Icon(Icons.Rounded.Refresh, null)
            Spacer(Modifier.size(6.dp))
            Text("شروع دوباره")
        }
    }
}
