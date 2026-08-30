package ir.asteam.namedic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.asteam.namedic.data.NameDiscoveryEngine
import ir.asteam.namedic.data.NameRepository
import ir.asteam.namedic.data.NameSuggestionCriteria
import ir.asteam.namedic.data.NameSuggestionEngine
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry

/**
 * صفحهٔ ابزارهای انتخاب نام در نسخه 2.0.
 *
 * فیلترها فقط دادهٔ موجود را محدود می‌کنند. برنامه هیچ «نمرهٔ کیفیت»، طالع،
 * شخصیت‌سنجی یا محبوبیت ساختگی برای نام تولید نمی‌کند.
 */
@Composable
fun AdvancedNameToolsScreen(
    repository: NameRepository,
    discovery: NameDiscoveryEngine,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onName: (NameEntry) -> Unit,
) {
    val engine = remember(repository) { NameSuggestionEngine(repository.names, repository.cultures) }

    var gender by remember { mutableStateOf<Gender?>(null) }
    var firstLetter by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var cultureId by remember { mutableStateOf<String?>(null) }
    var requireInfo by remember { mutableStateOf(true) }
    var familyName by remember { mutableStateOf("") }

    val criteria = NameSuggestionCriteria(
        gender = gender,
        firstLetter = firstLetter,
        keyword = keyword,
        cultureId = cultureId,
        requireReadableInfo = requireInfo,
    )
    val letters = remember(gender) { engine.availableFirstLetters(gender) }
    val cultures = remember { engine.availableCultures() }
    val results = remember(criteria) { engine.suggest(criteria, 80) }
    val totalMatches = remember(criteria) { engine.countMatches(criteria) }

    fun reset() {
        gender = null
        firstLetter = ""
        keyword = ""
        cultureId = null
        requireInfo = true
        familyName = ""
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(62.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Tune, null, Modifier.size(33.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("پیشنهادگر دقیق اسم", fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text(
                            "نتایج بر اساس فیلترهای خودت مرتب می‌شوند؛ نه بر اساس حدس یا امتیاز پنهان.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text("جنسیت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = gender == null,
                    onClick = { gender = null; firstLetter = "" },
                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                ) { Text("همه") }
                SegmentedButton(
                    selected = gender == Gender.FEMALE,
                    onClick = { gender = Gender.FEMALE; firstLetter = "" },
                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                ) { Text("دختر") }
                SegmentedButton(
                    selected = gender == Gender.MALE,
                    onClick = { gender = Gender.MALE; firstLetter = "" },
                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                ) { Text("پسر") }
            }
        }

        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام، معنی یا ریشه") },
                placeholder = { Text("مثلاً زندگی، فارسی، آر…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
            )
        }

        if (letters.isNotEmpty()) {
            item {
                Text("حرف اول", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item {
                        FilterChip(
                            selected = firstLetter.isBlank(),
                            onClick = { firstLetter = "" },
                            label = { Text("همه") },
                        )
                    }
                    items(letters, key = { it }) { letter ->
                        FilterChip(
                            selected = firstLetter == letter,
                            onClick = { firstLetter = letter },
                            label = { Text(letter) },
                        )
                    }
                }
            }
        }

        if (cultures.isNotEmpty()) {
            item {
                Text("فرهنگ / فهرست منبع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item {
                        FilterChip(
                            selected = cultureId == null,
                            onClick = { cultureId = null },
                            label = { Text("همه") },
                        )
                    }
                    items(cultures, key = { it.id }) { culture ->
                        FilterChip(
                            selected = cultureId == culture.id,
                            onClick = { cultureId = culture.id },
                            label = { Text(culture.titleFa) },
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.FilterAlt, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("فقط اسم‌های دارای توضیح", fontWeight = FontWeight.Bold)
                        Text(
                            "حداقل معنی، ریشه یا دادهٔ واژگانی قابل نمایش داشته باشند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = requireInfo, onCheckedChange = { requireInfo = it })
                }
            }
        }

        item {
            OutlinedTextField(
                value = familyName,
                onValueChange = { familyName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام خانوادگی برای پیش‌نمایش (اختیاری)") },
                placeholder = { Text("فقط روی همین صفحه نمایش داده می‌شود") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("$totalMatches نتیجه؛ نمایش ${results.size}") },
                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
                )
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = ::reset) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(5.dp))
                    Text("پاک کردن")
                }
            }
        }

        if (results.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("با این ترکیب فیلتری نتیجه‌ای پیدا نشد", fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(
                        "یکی از فیلترها را حذف کن؛ داده‌ای که وجود ندارد ساخته یا حدس زده نمی‌شود.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(results, key = { it.name }) { entry ->
                SuggestedNameRow(
                    entry = entry,
                    subtitle = suggestionSubtitle(entry, discovery),
                    familyName = familyName,
                    favorite = entry.name in favorites,
                    onFavorite = { onFavorite(entry.name) },
                    onName = { onName(entry) },
                )
            }
        }
    }
}

/** کارت ورودی ابزارهای نسخه 2.0 برای صفحهٔ اصلی. */
@Composable
fun AdvancedToolsHomeCard(onClick: () -> Unit) {
    ElevatedCard(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                Modifier.size(54.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, null, Modifier.size(29.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text("پیشنهادگر پیشرفته", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    "حرف اول، معنی، ریشه، فرهنگ و پیش‌نمایش نام کامل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SuggestedNameRow(
    entry: NameEntry,
    subtitle: String,
    familyName: String,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onName: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onName).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(
                Modifier.size(50.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(entry.name.take(1), fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (familyName.isBlank()) entry.name else "${entry.name} ${familyName.trim()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "پسند",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onName) { Icon(Icons.Rounded.Info, "جزئیات") }
        }
    }
}

private fun suggestionSubtitle(entry: NameEntry, discovery: NameDiscoveryEngine): String {
    val cultures = discovery.cultureTitles(entry)
    return when {
        entry.meaning.isNotBlank() -> entry.meaning
        entry.origin.isNotBlank() -> "ریشه: ${entry.origin}"
        entry.lexicalMeaningFa.isNotBlank() -> entry.lexicalMeaningFa
        cultures.isNotEmpty() -> cultures.joinToString("، ")
        entry.latin.isNotBlank() -> entry.latin
        else -> entry.gender.titleFa
    }
}
