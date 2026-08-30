package ir.asteam.namedic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class HistoryHubTab(val titleFa: String) {
    FIGURES("شخصیت‌ها"),
    TIMELINE("خط زمانی"),
    QUIZ("آزمون"),
}

/**
 * مرکز تاریخ ایران در نام‌نامه.
 *
 * این Hub سه تجربهٔ جدا را بدون افزودن پیچیدگی به ناوبری سراسری برنامه کنار هم
 * قرار می‌دهد: شخصیت‌ها، خط زمانی و آزمون آفلاین.
 */
@Composable
fun IranHistoryHubScreen() {
    var tab by remember { mutableStateOf(HistoryHubTab.FIGURES) }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(HistoryHubTab.entries, key = { it.name }) { item ->
                FilterChip(
                    selected = tab == item,
                    onClick = { tab = item },
                    label = { Text(item.titleFa) },
                    leadingIcon = {
                        Icon(
                            when (item) {
                                HistoryHubTab.FIGURES -> Icons.Rounded.AccountBalance
                                HistoryHubTab.TIMELINE -> Icons.Rounded.HistoryEdu
                                HistoryHubTab.QUIZ -> Icons.Rounded.Quiz
                            },
                            null,
                        )
                    },
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (tab) {
                HistoryHubTab.FIGURES -> HistoricalFiguresScreen()
                HistoryHubTab.TIMELINE -> HistoryTimelineScreen()
                HistoryHubTab.QUIZ -> IranHistoryQuizScreen()
            }
        }
    }
}
