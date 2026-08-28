package ir.asteam.namedic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.asteam.namedic.data.NameDiscoveryEngine
import ir.asteam.namedic.data.NameRepository
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry
import java.time.LocalDate

private enum class V2Screen { HOME, GIRLS, BOYS, SEARCH, FAVORITES, CULTURES, DETAIL, DISCOVER }

private val GirlA = Color(0xFFFDE5EF)
private val GirlB = Color(0xFFF8BBD0)
private val BoyA = Color(0xFFE1F2FF)
private val BoyB = Color(0xFFB9DFFF)
private val MintA = Color(0xFFE5F7F0)
private val Cream = Color(0xFFFFF8EE)

/**
 * رابط نسل دوم نام‌نامه ایران.
 * محور طراحی: انتخاب نام، نه نمایش دیتابیس خام.
 */
@Composable
fun NameDicAppV2() {
    val context = LocalContext.current
    val repository = remember { NameRepository(context) }
    val discovery = remember { NameDiscoveryEngine(repository) }
    val prefs = remember { context.getSharedPreferences("app_namedic_preferences", 0) }

    var screen by remember { mutableStateOf(V2Screen.HOME) }
    var previous by remember { mutableStateOf(V2Screen.HOME) }
    var selected by remember { mutableStateOf<NameEntry?>(null) }
    var discoverGender by remember { mutableStateOf(Gender.FEMALE) }
    var favorites by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    }

    fun go(target: V2Screen) {
        if (screen != target) previous = screen
        screen = target
    }

    fun openDetail(entry: NameEntry) {
        selected = entry
        previous = screen
        screen = V2Screen.DETAIL
    }

    fun toggleFavorite(name: String) {
        favorites = if (name in favorites) favorites - name else favorites + name
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    BackHandler(enabled = screen != V2Screen.HOME) {
        screen = if (screen == V2Screen.DETAIL) previous else V2Screen.HOME
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (screen != V2Screen.DETAIL) {
                NavigationBar(containerColor = Color.White) {
                    V2NavItem(screen == V2Screen.HOME, Icons.Rounded.Home, "خانه") { go(V2Screen.HOME) }
                    V2NavItem(screen == V2Screen.SEARCH, Icons.Rounded.Search, "جستجو") { go(V2Screen.SEARCH) }
                    V2NavItem(screen == V2Screen.DISCOVER, Icons.Rounded.AutoAwesome, "کشف اسم") { go(V2Screen.DISCOVER) }
                    V2NavItem(screen == V2Screen.FAVORITES, Icons.Rounded.Favorite, "پسندها") { go(V2Screen.FAVORITES) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                V2Screen.HOME -> V2Home(
                    discovery = discovery,
                    favoriteCount = favorites.size,
                    onGirls = { go(V2Screen.GIRLS) },
                    onBoys = { go(V2Screen.BOYS) },
                    onSearch = { go(V2Screen.SEARCH) },
                    onCultures = { go(V2Screen.CULTURES) },
                    onDiscover = { gender -> discoverGender = gender; go(V2Screen.DISCOVER) },
                    onOpen = ::openDetail,
                )

                V2Screen.GIRLS -> GenderBrowseScreen(
                    title = "اسم‌های دخترانه",
                    subtitle = "نام‌های دخترانه را ساده، سریع و بدون شلوغی مرور کن",
                    gender = Gender.FEMALE,
                    names = discovery.browse(Gender.FEMALE),
                    favorites = favorites,
                    onBack = { go(V2Screen.HOME) },
                    onOpen = ::openDetail,
                    onFavorite = ::toggleFavorite,
                )

                V2Screen.BOYS -> GenderBrowseScreen(
                    title = "اسم‌های پسرانه",
                    subtitle = "نام‌های پسرانه با جستجو و فیلتر کاربردی",
                    gender = Gender.MALE,
                    names = discovery.browse(Gender.MALE),
                    favorites = favorites,
                    onBack = { go(V2Screen.HOME) },
                    onOpen = ::openDetail,
                    onFavorite = ::toggleFavorite,
                )

                V2Screen.SEARCH -> V2Search(discovery, favorites, ::toggleFavorite, ::openDetail)
                V2Screen.FAVORITES -> V2Favorites(repository.names.filter { it.name in favorites }, favorites, ::toggleFavorite, ::openDetail)
                V2Screen.CULTURES -> V2Cultures(discovery, ::openDetail)
                V2Screen.DISCOVER -> V2Discover(discovery, discoverGender, favorites, ::toggleFavorite, ::openDetail)
                V2Screen.DETAIL -> selected?.let { entry ->
                    V2Detail(
                        entry = entry,
                        related = discovery.relatedTo(entry),
                        isFavorite = entry.name in favorites,
                        onBack = { screen = previous },
                        onFavorite = { toggleFavorite(entry.name) },
                        onOpenRelated = ::openDetail,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.V2NavItem(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        label = { Text(label, fontSize = 11.sp) },
    )
}

@Composable
private fun V2Home(
    discovery: NameDiscoveryEngine,
    favoriteCount: Int,
    onGirls: () -> Unit,
    onBoys: () -> Unit,
    onSearch: () -> Unit,
    onCultures: () -> Unit,
    onDiscover: (Gender) -> Unit,
    onOpen: (NameEntry) -> Unit,
) {
    val featured = remember { discovery.featuredName(LocalDate.now().dayOfYear) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("نام‌نامه ایران", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("اسم مناسب را پیدا کن، مقایسه کن و نگه دار", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SearchHero(onSearch)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                GenderHeroCard(
                    title = "اسم دختر",
                    count = discovery.femaleCount,
                    icon = Icons.Rounded.Female,
                    colors = listOf(GirlA, GirlB),
                    modifier = Modifier.weight(1f),
                    onClick = onGirls,
                )
                GenderHeroCard(
                    title = "اسم پسر",
                    count = discovery.maleCount,
                    icon = Icons.Rounded.Male,
                    colors = listOf(BoyA, BoyB),
                    modifier = Modifier.weight(1f),
                    onClick = onBoys,
                )
            }
        }

        item {
            Text("کشف سریع", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SmallFeatureCard("دخترانه", "اسم‌های پیشنهادی", Icons.Rounded.AutoAwesome, GirlA) { onDiscover(Gender.FEMALE) } }
                item { SmallFeatureCard("پسرانه", "اسم‌های پیشنهادی", Icons.Rounded.Explore, BoyA) { onDiscover(Gender.MALE) } }
                item { SmallFeatureCard("فرهنگ‌ها", "فقط دسته‌های پُر", Icons.Rounded.Public, MintA, onCultures) }
                item { SmallFeatureCard("پسندها", "$favoriteCount اسم ذخیره‌شده", Icons.Rounded.Favorite, Color(0xFFFFE7E7)) { } }
            }
        }

        featured?.let { name ->
            item {
                Text("پیشنهاد امروز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            item {
                FeaturedNameCard(name = name, onClick = { onOpen(name) })
            }
        }

        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("چرا این نسخه ساده‌تر است؟", fontWeight = FontWeight.Bold)
                    Text("• دختر و پسر از همان ابتدا جدا هستند")
                    Text("• دسته‌های خالی نمایش داده نمی‌شوند")
                    Text("• اطلاعات فنی کم‌ارزش از صفحه نام حذف شده")
                    Text("• مسیر اصلی برنامه «پیدا کردن اسم» است، نه مرور دیتابیس خام")
                }
            }
        }
    }
}

@Composable
private fun SearchHero(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null)
            Spacer(Modifier.width(10.dp))
            Text("اسم یا معنی مورد نظرت را جستجو کن", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GenderHeroCard(
    title: String,
    count: Int,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(178.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(54.dp).align(Alignment.TopEnd))
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("${count.toPersianDigits()} نام", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SmallFeatureCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(154.dp).height(112.dp).clickable(onClick = onClick),
        color = color,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null)
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun FeaturedNameCard(name: NameEntry, onClick: () -> Unit) {
    val colors = if (name.gender == Gender.FEMALE) listOf(GirlA, Color.White) else listOf(BoyA, Color.White)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.horizontalGradient(colors)).clickable(onClick = onClick).padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name.name, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text(name.gender.titleFa, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            val desc = name.meaning.ifBlank { name.lexicalMeaningFa.ifBlank { name.origin.ifBlank { "برای دیدن جزئیات لمس کن" } } }
            Text(desc, maxLines = 2)
        }
        Icon(Icons.Rounded.ArrowBackIosNew, null, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun GenderBrowseScreen(
    title: String,
    subtitle: String,
    gender: Gender,
    names: List<NameEntry>,
    favorites: Set<String>,
    onBack: () -> Unit,
    onOpen: (NameEntry) -> Unit,
    onFavorite: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var enrichedOnly by remember { mutableStateOf(false) }
    val filtered = remember(query, enrichedOnly, names) {
        names.filter {
            (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.meaning.contains(query.trim(), ignoreCase = true)) &&
                (!enrichedOnly || it.meaning.isNotBlank() || it.origin.isNotBlank() || it.lexicalMeaningFa.isNotBlank())
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, subtitle, onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("جستجو در ${if (gender == Gender.FEMALE) "اسم‌های دختر" else "اسم‌های پسر"}") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        )
        Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = enrichedOnly,
                onClick = { enrichedOnly = !enrichedOnly },
                label = { Text("فقط اسم‌های دارای اطلاعات") },
                leadingIcon = { Icon(Icons.Rounded.Verified, null, Modifier.size(16.dp)) },
            )
            Spacer(Modifier.weight(1f))
            Text("${filtered.size.toPersianDigits()} نام", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NameCardsList(filtered, favorites, onFavorite, onOpen)
    }
}

@Composable
private fun V2Search(
    discovery: NameDiscoveryEngine,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onOpen: (NameEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Gender?>(null) }
    var enrichedOnly by remember { mutableStateOf(false) }
    val results = remember(query, gender, enrichedOnly) { discovery.search(query, gender, enrichedOnly) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("جستجوی نام", fontSize = 28.sp, fontWeight = FontWeight.Black)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("مثلاً آرش، بهار، امید...") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(gender == null, { gender = null }, { Text("همه") }) }
                item { FilterChip(gender == Gender.FEMALE, { gender = Gender.FEMALE }, { Text("دختر") }) }
                item { FilterChip(gender == Gender.MALE, { gender = Gender.MALE }, { Text("پسر") }) }
                item { FilterChip(enrichedOnly, { enrichedOnly = !enrichedOnly }, { Text("دارای اطلاعات") }) }
            }
            Text("${results.size.toPersianDigits()} نتیجه", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NameCardsList(results, favorites, onFavorite, onOpen)
    }
}

@Composable
private fun V2Favorites(
    names: List<NameEntry>,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onOpen: (NameEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("اسم‌های پسندیده", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("لیست شخصی برای مقایسه نهایی", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (names.isEmpty()) {
            FriendlyEmptyState(Icons.Rounded.FavoriteBorder, "هنوز اسمی ذخیره نکردی", "روی قلب کنار هر اسم بزن تا اینجا نگهش داری.")
        } else NameCardsList(names, favorites, onFavorite, onOpen)
    }
}

@Composable
private fun V2Cultures(discovery: NameDiscoveryEngine, onOpen: (NameEntry) -> Unit) {
    val cultures = remember { discovery.availableCultures() }
    var selectedCultureId by remember { mutableStateOf<String?>(null) }
    val selectedStats = cultures.firstOrNull { it.culture.id == selectedCultureId }

    if (selectedStats == null) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("فرهنگ‌ها و زبان‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("فقط دسته‌هایی نمایش داده می‌شوند که واقعاً داده دارند.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            items(cultures, key = { it.culture.id }) { stat ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { selectedCultureId = stat.culture.id },
                    color = Color.White,
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(MintA), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Language, null)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stat.culture.titleFa, fontWeight = FontWeight.Bold)
                            Text("${stat.total.toPersianDigits()} نام • دختر ${stat.female.toPersianDigits()} • پسر ${stat.male.toPersianDigits()}", fontSize = 12.sp)
                        }
                        Icon(Icons.Rounded.ChevronLeft, null)
                    }
                }
            }
        }
    } else {
        val names = remember(selectedStats.culture.id) { discovery.browse(cultureId = selectedStats.culture.id) }
        Column {
            ScreenHeader(selectedStats.culture.titleFa, selectedStats.culture.subtitleFa) { selectedCultureId = null }
            NameCardsList(names, emptySet(), {}, onOpen)
        }
    }
}

@Composable
private fun V2Discover(
    discovery: NameDiscoveryEngine,
    initialGender: Gender,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onOpen: (NameEntry) -> Unit,
) {
    var gender by remember { mutableStateOf(initialGender) }
    var index by remember(gender) { mutableStateOf(0) }
    val pool = remember(gender) { discovery.discoveryPool(gender) }
    val current = pool.getOrNull(index % pool.size.coerceAtLeast(1))

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("کشف اسم", fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("یکی‌یکی اسم‌ها را ببین؛ پسند کن یا برو بعدی", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(gender == Gender.FEMALE, { gender = Gender.FEMALE; index = 0 }, { Text("دختر") })
            FilterChip(gender == Gender.MALE, { gender = Gender.MALE; index = 0 }, { Text("پسر") })
        }
        Spacer(Modifier.height(4.dp))
        current?.let { name ->
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 310.dp).clickable { onOpen(name) },
                color = if (gender == Gender.FEMALE) GirlA else BoyA,
                shape = RoundedCornerShape(34.dp),
                shadowElevation = 4.dp,
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(if (gender == Gender.FEMALE) Icons.Rounded.Female else Icons.Rounded.Male, null, Modifier.size(52.dp))
                    Spacer(Modifier.height(18.dp))
                    Text(name.name, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    if (name.latin.isNotBlank()) Text(name.latin, color = Color.DarkGray)
                    Spacer(Modifier.height(16.dp))
                    val desc = name.meaning.ifBlank { name.lexicalMeaningFa.ifBlank { name.origin.ifBlank { "برای جزئیات لمس کن" } } }
                    Text(desc, textAlign = TextAlign.Center, maxLines = 4)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                FilledTonalIconButton(onClick = { index++ }, modifier = Modifier.size(60.dp)) { Icon(Icons.Rounded.Close, "بعدی") }
                FilledIconButton(onClick = { onFavorite(name.name); index++ }, modifier = Modifier.size(66.dp)) {
                    Icon(if (name.name in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "پسند")
                }
                FilledTonalIconButton(onClick = { onOpen(name) }, modifier = Modifier.size(60.dp)) { Icon(Icons.Rounded.Info, "جزئیات") }
            }
        } ?: FriendlyEmptyState(Icons.Rounded.SearchOff, "نامی پیدا نشد", "فیلتر را تغییر بده.")
    }
}

@Composable
private fun V2Detail(
    entry: NameEntry,
    related: List<NameEntry>,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onOpenRelated: (NameEntry) -> Unit,
) {
    val heroColors = if (entry.gender == Gender.FEMALE) listOf(GirlA, GirlB) else listOf(BoyA, BoyB)
    LazyColumn(contentPadding = PaddingValues(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(250.dp).background(Brush.verticalGradient(heroColors)).padding(18.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) { Icon(Icons.Rounded.ArrowBack, "بازگشت") }
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "علاقه‌مندی")
                }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(entry.name, fontSize = 43.sp, fontWeight = FontWeight.Black)
                    if (entry.latin.isNotBlank()) Text(entry.latin, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    SuggestionChip(onClick = {}, label = { Text(entry.gender.titleFa) })
                }
            }
        }

        if (entry.meaning.isNotBlank()) item { InfoCard("معنی نام", entry.meaning, Icons.Rounded.MenuBook) }
        if (entry.origin.isNotBlank()) item { InfoCard("ریشه", entry.origin, Icons.Rounded.AccountBalance) }
        if (entry.lexicalMeaningFa.isNotBlank()) item {
            InfoCard("معنی واژهٔ هم‌نام", entry.lexicalMeaningFa, Icons.Rounded.Translate)
        }
        if (entry.lexicalAntonymsFa.isNotBlank()) item {
            InfoCard("متضاد واژهٔ هم‌نام", entry.lexicalAntonymsFa, Icons.Rounded.CompareArrows)
        }
        if (entry.meaning.isBlank() && entry.origin.isBlank() && entry.lexicalMeaningFa.isBlank()) item {
            Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("اطلاعات این اسم هنوز کامل نشده", fontWeight = FontWeight.Bold)
                    Text("اسم در فهرست معتبر وجود دارد، اما برای معنی یا ریشه هنوز منبع قابل اتکای کافی متصل نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (related.isNotEmpty()) {
            item { Text("اسم‌های مشابه", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(related, key = { it.name }) { item ->
                        Surface(
                            Modifier.width(132.dp).clickable { onOpenRelated(item) },
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(item.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(item.gender.titleFa, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String, icon: ImageVector) {
    Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MintA), contentAlignment = Alignment.Center) { Icon(icon, null) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text)
            }
        }
    }
}

@Composable
private fun NameCardsList(
    names: List<NameEntry>,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onOpen: (NameEntry) -> Unit,
) {
    if (names.isEmpty()) {
        FriendlyEmptyState(Icons.Rounded.SearchOff, "چیزی پیدا نشد", "جستجو یا فیلتر را تغییر بده.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(names, key = { it.name }) { entry ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(entry) },
                color = Color.White,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 1.dp,
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(if (entry.gender == Gender.FEMALE) GirlA else if (entry.gender == Gender.MALE) BoyA else MintA),
                        contentAlignment = Alignment.Center,
                    ) { Text(entry.name.take(1), fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        val info = when {
                            entry.meaning.isNotBlank() -> entry.meaning
                            entry.origin.isNotBlank() -> "ریشه: ${entry.origin}"
                            entry.lexicalMeaningFa.isNotBlank() -> entry.lexicalMeaningFa
                            entry.latin.isNotBlank() -> entry.latin
                            else -> entry.gender.titleFa
                        }
                        Text(info, maxLines = 1, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (onFavorite != {}) {
                        IconButton(onClick = { onFavorite(entry.name) }) {
                            Icon(if (entry.name in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "علاقه‌مندی")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendlyEmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(82.dp).clip(CircleShape).background(MintA), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(38.dp)) }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "بازگشت") }
        Column {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Int.toPersianDigits(): String = toString().map {
    when (it) {
        '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
        '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
        else -> it
    }
}.joinToString("")
