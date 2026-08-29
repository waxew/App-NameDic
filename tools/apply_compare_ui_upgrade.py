#!/usr/bin/env python3
"""Apply the v1.4.0 favorites comparison UI upgrade.

The project keeps this migration idempotent so CI can safely run it more than once.
It only replaces exact, known blocks from the previous UI and fails loudly if the
expected source shape is neither old nor already upgraded.
"""

from pathlib import Path

UI_PATH = Path("app/src/main/java/ir/asteam/namedic/ui/NameDicRedesign.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    """Replace one exact source block or accept an already-applied migration."""
    if new in text:
        print(f"[skip] {label}: already upgraded")
        return text
    if old not in text:
        raise SystemExit(f"[error] {label}: expected source block not found")
    print(f"[apply] {label}")
    return text.replace(old, new, 1)


def main() -> None:
    text = UI_PATH.read_text(encoding="utf-8")

    text = replace_once(
        text,
        'NewScreen.FAVORITES -> FavoritesScreen(repository.names.filter { it.name in favorites }, favorites, ::toggleFavorite, ::openName) { go(NewScreen.DISCOVER) }',
        'NewScreen.FAVORITES -> FavoritesScreen(discovery, repository.names.filter { it.name in favorites }, favorites, ::toggleFavorite, ::openName) { go(NewScreen.DISCOVER) }',
        "wire discovery into favorites",
    )

    old_favorites = '''@Composable
private fun FavoritesScreen(names: List<NameEntry>, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit, onDiscover: () -> Unit) {
    if (names.isEmpty()) {
        EmptyState(Icons.Rounded.FavoriteBorder, "هنوز اسمی نپسندیدی", "در بخش «اسم پیدا کن» یا لیست دختر و پسر، قلب اسم‌هایی را که دوست داری بزن.", "اسم پیدا کن", onDiscover)
    } else {
        Column(Modifier.fillMaxSize()) {
            Text("${names.size} اسم برای مقایسه نگه داشته‌ای", Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            NameFeed(names, favorites, onFavorite, onName, "", {})
        }
    }
}
'''

    new_favorites = '''@Composable
private fun FavoritesScreen(
    discovery: NameDiscoveryEngine,
    names: List<NameEntry>,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onName: (NameEntry) -> Unit,
    onDiscover: () -> Unit,
) {
    if (names.isEmpty()) {
        EmptyState(
            Icons.Rounded.FavoriteBorder,
            "هنوز اسمی نپسندیدی",
            "در بخش «اسم پیدا کن» یا لیست دختر و پسر، قلب اسم‌هایی را که دوست داری بزن.",
            "اسم پیدا کن",
            onDiscover,
        )
        return
    }

    var selectedNames by remember(names) { mutableStateOf(emptySet<String>()) }
    var compareMode by remember { mutableStateOf(false) }

    BackHandler(enabled = compareMode) { compareMode = false }

    if (compareMode) {
        FavoriteComparisonPanel(
            discovery = discovery,
            names = names.filter { it.name in selectedNames },
            onBack = { compareMode = false },
            onName = onName,
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("لیست نهایی انتخاب اسم", fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    "از بین اسم‌های پسندیده، ۲ تا ۴ اسم را انتخاب کن تا معنی، ریشه و فرهنگ آن‌ها را کنار هم ببینی.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${selectedNames.size} انتخاب از ${names.size} پسندیده",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                    )
                    Button(
                        onClick = { compareMode = true },
                        enabled = selectedNames.size in 2..4,
                    ) {
                        Icon(Icons.Rounded.CompareArrows, null)
                        Spacer(Modifier.width(6.dp))
                        Text("مقایسه")
                    }
                }
                if (selectedNames.size < 2) {
                    Text(
                        "برای فعال شدن مقایسه حداقل دو اسم را تیک بزن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (selectedNames.size == 4) {
                    Text(
                        "حداکثر چهار اسم را می‌توانی هم‌زمان مقایسه کنی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(names, key = { it.name }) { entry ->
                val selected = entry.name in selectedNames
                FavoriteSelectionRow(
                    entry = entry,
                    selected = selected,
                    selectionEnabled = selected || selectedNames.size < 4,
                    onSelectedChange = { shouldSelect ->
                        selectedNames = when {
                            shouldSelect && selectedNames.size < 4 -> selectedNames + entry.name
                            !shouldSelect -> selectedNames - entry.name
                            else -> selectedNames
                        }
                    },
                    onFavorite = { onFavorite(entry.name) },
                    onName = { onName(entry) },
                )
            }
        }
    }
}

@Composable
private fun FavoriteSelectionRow(
    entry: NameEntry,
    selected: Boolean,
    selectionEnabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onFavorite: () -> Unit,
    onName: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = selectionEnabled) { onSelectedChange(!selected) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = onSelectedChange,
                enabled = selectionEnabled,
            )
            Surface(
                shape = CircleShape,
                color = genderContainer(entry.gender),
                modifier = Modifier.size(46.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(entry.name.take(1), fontWeight = FontWeight.Black, fontSize = 19.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    usefulSubtitle(entry),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onName) {
                Icon(Icons.Rounded.Info, "جزئیات")
            }
            IconButton(onClick = onFavorite) {
                Icon(Icons.Rounded.Favorite, "حذف از پسندیده‌ها")
            }
        }
    }
}

@Composable
private fun FavoriteComparisonPanel(
    discovery: NameDiscoveryEngine,
    names: List<NameEntry>,
    onBack: () -> Unit,
    onName: (NameEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowForward, "بازگشت به پسندیده‌ها")
            }
            Column(Modifier.weight(1f)) {
                Text("مقایسه اسم‌ها", fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text(
                    "${names.size} اسم انتخاب شده",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(names, key = { it.name }) { entry ->
                        val cultureTitles = discovery.cultureTitles(entry)
                        ElevatedCard(
                            modifier = Modifier.width(270.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = genderContainer(entry.gender)),
                        ) {
                            Column(
                                Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = .78f),
                                        modifier = Modifier.size(54.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(entry.name.take(1), fontSize = 23.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                        Text(entry.gender.titleFa, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (entry.latin.isNotBlank()) {
                                    ComparisonField("نوشتار لاتین", entry.latin)
                                }
                                ComparisonField(
                                    "معنی اسم",
                                    entry.meaning.ifBlank { "معنی مستقیمِ منبع‌دار هنوز ثبت نشده" },
                                )
                                ComparisonField(
                                    "ریشه",
                                    entry.origin.ifBlank { "ریشهٔ منبع‌دار هنوز ثبت نشده" },
                                )
                                ComparisonField(
                                    "فرهنگ / فهرست",
                                    cultureTitles.ifEmpty { listOf("دستهٔ فرهنگی خاص ثبت نشده") }.joinToString("، "),
                                )
                                if (entry.lexicalMeaningFa.isNotBlank()) {
                                    ComparisonField("واژهٔ هم‌نوشت", entry.lexicalMeaningFa)
                                }

                                OutlinedButton(
                                    onClick = { onName(entry) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Rounded.Info, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("جزئیات کامل")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "خالی بودن معنی یا ریشه به معنی نامعتبر بودن اسم نیست؛ فقط یعنی هنوز دادهٔ مستقیم و منبع‌دار کافی برای آن فیلد به رکورد متصل نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, lineHeight = 22.sp)
    }
}
'''

    text = replace_once(text, old_favorites, new_favorites, "favorites comparison UI")

    text = replace_once(
        text,
        '    val related = remember(entry.name) { discovery.relatedTo(entry, 8) }\n',
        '    val related = remember(entry.name) { discovery.relatedTo(entry, 8) }\n    val cultureTitles = remember(entry.name) { discovery.cultureTitles(entry) }\n',
        "resolve culture titles in details",
    )

    text = replace_once(
        text,
        '        if (entry.usageCultureIds.any { it != "iran_general" }) item { DetailCard(Icons.Rounded.Language, "فرهنگ‌های ثبت‌شده", entry.usageCultureIds.filter { it != "iran_general" }.joinToString("، ")) }',
        '        if (cultureTitles.isNotEmpty()) item { DetailCard(Icons.Rounded.Language, "فرهنگ‌های ثبت‌شده", cultureTitles.joinToString("، ")) }',
        "hide internal culture ids",
    )

    UI_PATH.write_text(text, encoding="utf-8")
    print("[done] v1.4.0 comparison UI source is ready")


if __name__ == "__main__":
    main()
