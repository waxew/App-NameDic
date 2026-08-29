package ir.asteam.namedic.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.asteam.namedic.history.HistoricalFigure
import ir.asteam.namedic.history.HistoricalFiguresRepository

/**
 * ورودی مستقل بخش «بزرگان تاریخ ایران».
 *
 * این صفحه state انتخاب شخصیت را داخل خودش نگه می‌دارد تا برای اضافه شدن این
 * قابلیت مجبور نباشیم مدل ناوبری اصلی نام‌نامه را پیچیده کنیم. فهرست و جزئیات
 * هر دو از دادهٔ آفلاین asset استفاده می‌کنند.
 */
@Composable
fun HistoricalFiguresScreen() {
    val context = LocalContext.current
    val repository = remember { HistoricalFiguresRepository(context) }
    var selected by remember { mutableStateOf<HistoricalFigure?>(null) }

    if (selected != null) {
        BackHandler { selected = null }
        HistoricalFigureDetail(
            figure = selected!!,
            onBack = { selected = null },
            onOpenSource = { sourceUrl ->
                // لینک منبع فقط با اقدام مستقیم کاربر در مرورگر باز می‌شود.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))
                runCatching { context.startActivity(intent) }
            },
        )
    } else {
        HistoricalFiguresList(
            repository = repository,
            onFigure = { selected = it },
        )
    }
}

/**
 * کارت ورودی صفحهٔ اصلی نام‌نامه.
 * با این کامپوننت، فایل اصلی UI فقط یک فراخوانی کوتاه برای ماژول تاریخ دارد.
 */
@Composable
fun HistoricalFiguresHomeCard(onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AccountBalance, null, Modifier.size(31.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("بزرگان تاریخ ایران", fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    "فرمانروایان، دانشمندان، شاعران و اصلاح‌گران را بشناس",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ArrowForward, null)
        }
    }
}

/** فهرست قابل جستجو و فیلتر شخصیت‌های تاریخی. */
@Composable
private fun HistoricalFiguresList(
    repository: HistoricalFiguresRepository,
    onFigure: (HistoricalFigure) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val results = remember(query, category) { repository.search(query, category) }

    Column(Modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(66.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.HistoryEdu, null, Modifier.size(36.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("روایت کوتاه از چهره‌های اثرگذار", fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(
                        "اطلاعات خلاصه، منبع‌دار و قابل استفاده به‌صورت آفلاین است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("${repository.figures.size} شخصیت در نسخه فعلی") },
                        leadingIcon = { Icon(Icons.Rounded.PersonSearch, null) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 5.dp),
            placeholder = { Text("نام، دوره یا نقش را جستجو کن…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, "پاک کردن جستجو")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("همه") },
                )
            }
            items(repository.categories, key = { it }) { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { category = item },
                    label = { Text(item) },
                )
            }
        }

        Text(
            "${results.size} نتیجه",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.PersonSearch, null, Modifier.size(55.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("شخصیتی با این جستجو پیدا نشد", fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "عبارت یا دسته را تغییر بده.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = { query = ""; category = null }) { Text("پاک کردن فیلترها") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.id }) { figure ->
                    HistoricalFigureRow(figure = figure, onClick = { onFigure(figure) })
                }
            }
        }
    }
}

/** کارت خلاصهٔ هر شخصیت در فهرست. */
@Composable
private fun HistoricalFigureRow(figure: HistoricalFigure, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(figure.nameFa.take(1), fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(figure.nameFa, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    figure.roleFa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${figure.periodFa} • ${figure.yearsFa}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ArrowForward, null)
        }
    }
}

/** صفحهٔ جزئیات؛ هیچ متن مقاله‌ای را کپی نمی‌کند و فقط خلاصهٔ بازنویسی‌شده را نشان می‌دهد. */
@Composable
private fun HistoricalFigureDetail(
    figure: HistoricalFigure,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowForward, "بازگشت به فهرست")
                }
                Spacer(Modifier.width(10.dp))
                Text("بازگشت به بزرگان تاریخ", fontWeight = FontWeight.Bold)
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(figure.nameFa.take(1), fontSize = 38.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(figure.nameFa, fontSize = 29.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(figure.nameEn, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(figure.roleFa, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AssistChip(onClick = {}, label = { Text(figure.periodFa) }, leadingIcon = { Icon(Icons.Rounded.AccountBalance, null) })
                    }
                }
            }
        }

        item {
            HistoryInfoCard(
                icon = Icons.Rounded.Schedule,
                title = "دوره و زمان",
                text = figure.yearsFa,
            )
        }

        item {
            HistoryInfoCard(
                icon = Icons.Rounded.MenuBook,
                title = "این شخص که بود؟",
                text = figure.summaryFa,
            )
        }

        item {
            Text("نکات مهم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        items(figure.highlightsFa) { highlight ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Text(highlight, Modifier.weight(1f), lineHeight = 23.sp)
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text("منبع و مطالعهٔ بیشتر", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "متن داخل برنامه خلاصه و بازنویسی شده است. برای جزئیات، منابع و اختلاف‌نظرهای تاریخی می‌توانی مقالهٔ اصلی را باز کنی.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onOpenSource(figure.sourceUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.OpenInNew, null)
                Spacer(Modifier.width(7.dp))
                Text("باز کردن منبع: ${figure.sourceLabel}")
            }
        }
    }
}

/** کارت متنی مشترک برای جلوگیری از تکرار استایل در صفحهٔ جزئیات. */
@Composable
private fun HistoryInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            Text(text, lineHeight = 25.sp)
        }
    }
}
