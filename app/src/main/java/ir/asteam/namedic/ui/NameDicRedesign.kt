package ir.asteam.namedic.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.asteam.namedic.BuildConfig
import ir.asteam.namedic.data.NameDiscoveryEngine
import ir.asteam.namedic.data.NameRepository
import ir.asteam.namedic.model.CultureCategory
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.NameEntry
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * نسل دوم رابط نام‌نامه ایران.
 *
 * هدف این UI «پیدا کردن اسم» است، نه نمایش ساختار دیتابیس. کاربر از صفحه اول
 * مستقیماً بین اسم دختر و پسر انتخاب می‌کند و در تمام مسیرها، فهرست خالی به
 * صفحهٔ بن‌بست تبدیل نمی‌شود.
 */
private enum class NewScreen { HOME, GIRLS, BOYS, SEARCH, DISCOVER, FAVORITES, CULTURES, CULTURE_NAMES, DETAIL, ABOUT, CONTACT }

@Composable
fun NameDicRedesignApp() {
    val context = LocalContext.current
    val repository = remember { NameRepository(context) }
    val discovery = remember { NameDiscoveryEngine(repository) }
    val prefs = remember { context.getSharedPreferences("app_namedic_preferences", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var screen by remember { mutableStateOf(NewScreen.HOME) }
    var previous by remember { mutableStateOf(NewScreen.HOME) }
    var selectedName by remember { mutableStateOf<NameEntry?>(null) }
    var selectedCulture by remember { mutableStateOf<CultureCategory?>(null) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()) }
    var profileUri by remember { mutableStateOf(prefs.getString("profile_uri", "").orEmpty()) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "کاربر نام‌نامه").orEmpty()) }

    fun go(target: NewScreen) {
        if (target != screen) previous = screen
        screen = target
    }

    fun openName(entry: NameEntry) {
        selectedName = entry
        go(NewScreen.DETAIL)
    }

    fun toggleFavorite(name: String) {
        favorites = if (name in favorites) favorites - name else favorites + name
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            profileUri = uri.toString()
            prefs.edit().putString("profile_uri", profileUri).apply()
        }
    }

    BackHandler(enabled = screen != NewScreen.HOME || drawerState.isOpen) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            screen == NewScreen.DETAIL -> screen = previous
            screen == NewScreen.CULTURE_NAMES -> screen = NewScreen.CULTURES
            else -> screen = NewScreen.HOME
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NewDrawer(
                    profileUri = profileUri,
                    userName = userName,
                    onProfile = { imagePicker.launch(arrayOf("image/*")) },
                    onUserName = {
                        userName = it
                        prefs.edit().putString("user_name", it).apply()
                    },
                    onNavigate = {
                        go(it)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(newTitle(screen, selectedCulture, selectedName), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (screen == NewScreen.HOME) scope.launch { drawerState.open() }
                            else if (screen == NewScreen.DETAIL) screen = previous
                            else if (screen == NewScreen.CULTURE_NAMES) screen = NewScreen.CULTURES
                            else screen = NewScreen.HOME
                        }) {
                            Icon(if (screen == NewScreen.HOME) Icons.Rounded.Menu else Icons.Rounded.ArrowBack, null)
                        }
                    },
                )
            },
            bottomBar = {
                if (screen !in setOf(NewScreen.DETAIL, NewScreen.ABOUT, NewScreen.CONTACT, NewScreen.CULTURE_NAMES)) {
                    NavigationBar {
                        BottomDestination("خانه", Icons.Rounded.Home, screen == NewScreen.HOME) { go(NewScreen.HOME) }
                        BottomDestination("جستجو", Icons.Rounded.Search, screen == NewScreen.SEARCH) { go(NewScreen.SEARCH) }
                        BottomDestination("کشف اسم", Icons.Rounded.AutoAwesome, screen == NewScreen.DISCOVER) { go(NewScreen.DISCOVER) }
                        BottomDestination("پسندها", Icons.Rounded.Favorite, screen == NewScreen.FAVORITES) { go(NewScreen.FAVORITES) }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (screen) {
                    NewScreen.HOME -> NewHome(discovery, favorites.size, ::go, ::openName)
                    NewScreen.GIRLS -> GenderBrowser(Gender.FEMALE, discovery, favorites, ::toggleFavorite, ::openName)
                    NewScreen.BOYS -> GenderBrowser(Gender.MALE, discovery, favorites, ::toggleFavorite, ::openName)
                    NewScreen.SEARCH -> NewSearch(discovery, favorites, ::toggleFavorite, ::openName)
                    NewScreen.DISCOVER -> DiscoverScreen(discovery, favorites, ::toggleFavorite, ::openName)
                    NewScreen.FAVORITES -> FavoritesScreen(discovery, repository.names.filter { it.name in favorites }, favorites, ::toggleFavorite, ::openName) { go(NewScreen.DISCOVER) }
                    NewScreen.CULTURES -> NewCultureScreen(discovery) {
                        selectedCulture = it
                        go(NewScreen.CULTURE_NAMES)
                    }
                    NewScreen.CULTURE_NAMES -> CultureNamesScreen(selectedCulture, discovery, favorites, ::toggleFavorite, ::openName)
                    NewScreen.DETAIL -> selectedName?.let { NewDetail(it, discovery, favorites, ::toggleFavorite, ::openName) }
                    NewScreen.ABOUT -> NewAbout()
                    NewScreen.CONTACT -> NewContact()
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomDestination(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, null) }, label = { Text(label, maxLines = 1) })
}

private fun newTitle(screen: NewScreen, culture: CultureCategory?, name: NameEntry?): String = when (screen) {
    NewScreen.HOME -> "نام‌نامه ایران"
    NewScreen.GIRLS -> "اسم‌های دخترانه"
    NewScreen.BOYS -> "اسم‌های پسرانه"
    NewScreen.SEARCH -> "جستجوی اسم"
    NewScreen.DISCOVER -> "اسم پیدا کن"
    NewScreen.FAVORITES -> "اسم‌های پسندیده"
    NewScreen.CULTURES -> "فرهنگ‌ها و زبان‌ها"
    NewScreen.CULTURE_NAMES -> culture?.titleFa ?: "اسم‌ها"
    NewScreen.DETAIL -> name?.name ?: "جزئیات اسم"
    NewScreen.ABOUT -> "درباره نام‌نامه"
    NewScreen.CONTACT -> "ارتباط با ما"
}

@Composable
private fun NewHome(discovery: NameDiscoveryEngine, favoriteCount: Int, go: (NewScreen) -> Unit, openName: (NameEntry) -> Unit) {
    val today = remember { LocalDate.now().dayOfYear + LocalDate.now().year * 400 }
    val featured = remember(today) { discovery.featuredName(today) }
    val cultureStats = remember { discovery.availableCultures().take(5) }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)),
                ).padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اسمش قراره یک عمر همراهش باشه", fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("بین هزاران اسم ایرانی جستجو کن، پسندیده‌ها را نگه دار و اسم مناسب را راحت‌تر پیدا کن.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = { Text("${discovery.totalNames} اسم") }, leadingIcon = { Icon(Icons.Rounded.AutoStories, null) })
                        if (favoriteCount > 0) AssistChip(onClick = { go(NewScreen.FAVORITES) }, label = { Text("$favoriteCount پسند") }, leadingIcon = { Icon(Icons.Rounded.Favorite, null) })
                    }
                }
            }
        }

        item {
            Text("از کجا شروع کنیم؟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenderHeroCard("اسم دختر", "${discovery.femaleCount} نام", Icons.Rounded.Female, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f)) { go(NewScreen.GIRLS) }
                GenderHeroCard("اسم پسر", "${discovery.maleCount} نام", Icons.Rounded.Male, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f)) { go(NewScreen.BOYS) }
            }
        }

        featured?.let { entry ->
            item {
                SectionHeader("پیشنهاد امروز", "یک اسم برای شروع انتخاب")
                FeaturedNameCard(entry, onClick = { openName(entry) })
            }
        }

        item {
            Text("ابزارهای انتخاب اسم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniTool("جستجوی دقیق", Icons.Rounded.Search, Modifier.weight(1f)) { go(NewScreen.SEARCH) }
                MiniTool("اسم پیدا کن", Icons.Rounded.AutoAwesome, Modifier.weight(1f)) { go(NewScreen.DISCOVER) }
                MiniTool("فرهنگ‌ها", Icons.Rounded.Language, Modifier.weight(1f)) { go(NewScreen.CULTURES) }
            }
        }

        if (cultureStats.isNotEmpty()) {
            item { SectionHeader("فرهنگ‌های دارای اسم", "فقط دسته‌هایی که واقعاً داده دارند") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 2.dp)) {
                    items(cultureStats, key = { it.culture.id }) { stat ->
                        CultureMiniCard(stat.culture.titleFa, stat.total) { go(NewScreen.CULTURES) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderHeroCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), colors = CardDefaults.elevatedCardColors(containerColor = color)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .7f), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(34.dp)) }
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            FilledTonalButton(onClick = onClick) { Text("نمایش اسم‌ها"); Icon(Icons.Rounded.ChevronLeft, null) }
        }
    }
}

@Composable
private fun MiniTool(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FeaturedNameCard(entry: NameEntry, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = genderContainer(entry.gender), modifier = Modifier.size(66.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(entry.name.take(1), fontSize = 27.sp, fontWeight = FontWeight.Black) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(usefulSubtitle(entry), maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ArrowBackIosNew, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CultureMiniCard(title: String, count: Int, onClick: () -> Unit) {
    Card(Modifier.width(130.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("$count اسم", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GenderBrowser(gender: Gender, discovery: NameDiscoveryEngine, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit) {
    var query by remember { mutableStateOf("") }
    var enrichedOnly by remember { mutableStateOf(false) }
    val names = remember(query, enrichedOnly, gender) {
        if (query.isBlank()) discovery.browse(gender, enrichedOnly = enrichedOnly)
        else discovery.search(query, gender, enrichedOnly)
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(query, { query = it }, if (gender == Gender.FEMALE) "جستجو بین اسم‌های دخترانه" else "جستجو بین اسم‌های پسرانه")
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = enrichedOnly, onClick = { enrichedOnly = !enrichedOnly }, label = { Text("فقط اسم‌های دارای توضیح") }, leadingIcon = { Icon(Icons.Rounded.Verified, null) })
            Spacer(Modifier.weight(1f))
            Text("${names.size} اسم", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        NameFeed(names, favorites, onFavorite, onName, emptyTitle = "اسمی پیدا نشد", emptyAction = { query = "" })
    }
}

@Composable
private fun NewSearch(discovery: NameDiscoveryEngine, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit) {
    var query by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Gender?>(null) }
    var enrichedOnly by remember { mutableStateOf(false) }
    val results = remember(query, gender, enrichedOnly) { discovery.search(query, gender, enrichedOnly) }

    Column(Modifier.fillMaxSize()) {
        SearchField(query, { query = it }, "اسم یا بخشی از معنی را بنویس…")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(gender == null, { gender = null }, { Text("همه") }) }
            item { FilterChip(gender == Gender.FEMALE, { gender = Gender.FEMALE }, { Text("دختر") }, leadingIcon = { Icon(Icons.Rounded.Female, null) }) }
            item { FilterChip(gender == Gender.MALE, { gender = Gender.MALE }, { Text("پسر") }, leadingIcon = { Icon(Icons.Rounded.Male, null) }) }
            item { FilterChip(enrichedOnly, { enrichedOnly = !enrichedOnly }, { Text("اطلاعات‌دار") }, leadingIcon = { Icon(Icons.Rounded.Verified, null) }) }
        }
        Spacer(Modifier.height(8.dp))
        NameFeed(results, favorites, onFavorite, onName, if (query.isBlank()) "برای پیدا کردن اسم جستجو کن" else "نتیجه‌ای پیدا نشد", emptyAction = { query = "" })
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = if (value.isNotBlank()) ({ IconButton(onClick = { onValue("") }) { Icon(Icons.Rounded.Close, null) } }) else null,
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

@Composable
private fun DiscoverScreen(discovery: NameDiscoveryEngine, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit) {
    var gender by remember { mutableStateOf(Gender.FEMALE) }
    var index by remember { mutableIntStateOf(0) }
    val pool = remember(gender) { discovery.discoveryPool(gender) }
    val entry = pool.getOrNull(index % (pool.size.coerceAtLeast(1)))

    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("اسم‌ها را یکی‌یکی ببین", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("هر اسمی را دوست داشتی قلب بزن؛ بعداً همه را کنار هم مقایسه کن.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(selected = gender == Gender.FEMALE, onClick = { gender = Gender.FEMALE; index = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2), icon = { Icon(Icons.Rounded.Female, null) }) { Text("دختر") }
            SegmentedButton(selected = gender == Gender.MALE, onClick = { gender = Gender.MALE; index = 0 }, shape = SegmentedButtonDefaults.itemShape(1, 2), icon = { Icon(Icons.Rounded.Male, null) }) { Text("پسر") }
        }
        Spacer(Modifier.height(4.dp))
        if (entry != null) {
            ElevatedCard(Modifier.fillMaxWidth().weight(1f).clickable { onName(entry) }, shape = RoundedCornerShape(30.dp), colors = CardDefaults.elevatedCardColors(containerColor = genderContainer(entry.gender))) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .75f), modifier = Modifier.size(100.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(entry.name.take(1), fontSize = 45.sp, fontWeight = FontWeight.Black) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(entry.name, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    if (entry.latin.isNotBlank()) Text(entry.latin, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(usefulSubtitle(entry), textAlign = TextAlign.Center, fontSize = 17.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(18.dp))
                    AssistChip(onClick = { onName(entry) }, label = { Text("دیدن جزئیات") }, leadingIcon = { Icon(Icons.Rounded.Info, null) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilledTonalIconButton(onClick = { index = (index + 1) % pool.size.coerceAtLeast(1) }, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.Close, "بعدی", Modifier.size(30.dp)) }
                FilledIconButton(onClick = { onFavorite(entry.name); index = (index + 1) % pool.size.coerceAtLeast(1) }, modifier = Modifier.size(72.dp)) {
                    Icon(if (entry.name in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "پسند", Modifier.size(34.dp))
                }
                FilledTonalIconButton(onClick = { onName(entry) }, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.Info, "جزئیات", Modifier.size(30.dp)) }
            }
        }
    }
}

@Composable
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

@Composable
private fun NewCultureScreen(discovery: NameDiscoveryEngine, onCulture: (CultureCategory) -> Unit) {
    val stats = remember { discovery.availableCultures() }
    if (stats.isEmpty()) {
        EmptyState(Icons.Rounded.Language, "دستهٔ فرهنگی آماده نیست", "فقط دسته‌هایی نمایش داده می‌شوند که دادهٔ معتبر دارند.", null, {})
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("دسته‌های خالی حذف شده‌اند. تعداد واقعی اسم‌های هر فرهنگ را قبل از ورود می‌بینی.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(stats, key = { it.culture.id }) { stat ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { onCulture(stat.culture) }, shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Language, null) } }
                    Column(Modifier.weight(1f)) {
                        Text(stat.culture.titleFa, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("${stat.female} دختر • ${stat.male} پسر${if (stat.unisex > 0) " • ${stat.unisex} مشترک" else ""}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${stat.total}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Icon(Icons.Rounded.ChevronLeft, null)
                }
            }
        }
    }
}

@Composable
private fun CultureNamesScreen(culture: CultureCategory?, discovery: NameDiscoveryEngine, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit) {
    var gender by remember { mutableStateOf<Gender?>(null) }
    val names = remember(culture, gender) { discovery.browse(gender, culture?.id) }
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(gender == null, { gender = null }, { Text("همه") }) }
            item { FilterChip(gender == Gender.FEMALE, { gender = Gender.FEMALE }, { Text("دختر") }) }
            item { FilterChip(gender == Gender.MALE, { gender = Gender.MALE }, { Text("پسر") }) }
            item { FilterChip(gender == Gender.UNISEX, { gender = Gender.UNISEX }, { Text("مشترک") }) }
        }
        NameFeed(names, favorites, onFavorite, onName, "برای این فیلتر اسمی ثبت نشده", { gender = null })
    }
}

@Composable
private fun NameFeed(names: List<NameEntry>, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit, emptyTitle: String, emptyAction: () -> Unit) {
    if (names.isEmpty()) {
        EmptyState(Icons.Rounded.SearchOff, emptyTitle, "فیلتر را عوض کن یا از جستجوی دیگری استفاده کن.", "پاک کردن فیلتر", emptyAction)
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items(names, key = { it.name }) { entry -> NewNameRow(entry, entry.name in favorites, { onFavorite(entry.name) }) { onName(entry) } }
    }
}

@Composable
private fun NewNameRow(entry: NameEntry, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = genderContainer(entry.gender), modifier = Modifier.size(50.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(entry.name.take(1), fontWeight = FontWeight.Black, fontSize = 20.sp) }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(usefulSubtitle(entry), maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "پسند", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun NewDetail(entry: NameEntry, discovery: NameDiscoveryEngine, favorites: Set<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit) {
    val related = remember(entry.name) { discovery.relatedTo(entry, 8) }
    val cultureTitles = remember(entry.name) { discovery.cultureTitles(entry) }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(genderContainer(entry.gender)).padding(22.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .8f), modifier = Modifier.size(86.dp)) { Box(contentAlignment = Alignment.Center) { Text(entry.name.take(1), fontSize = 38.sp, fontWeight = FontWeight.Black) } }
                    Spacer(Modifier.height(12.dp))
                    Text(entry.name, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(entry.gender.titleFa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (entry.latin.isNotBlank()) Text(entry.latin, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = { onFavorite(entry.name) }) { Icon(if (entry.name in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null); Spacer(Modifier.width(6.dp)); Text(if (entry.name in favorites) "در پسندیده‌ها" else "پسندیدن اسم") }
                }
            }
        }

        if (entry.meaning.isNotBlank()) item { DetailCard(Icons.Rounded.MenuBook, "معنی اسم", entry.meaning) }
        if (entry.origin.isNotBlank()) item { DetailCard(Icons.Rounded.HistoryEdu, "ریشه", entry.origin) }
        if (entry.lexicalMeaningFa.isNotBlank()) item { DetailCard(Icons.Rounded.Translate, "معنی واژهٔ هم‌نوشت", entry.lexicalMeaningFa, "این توضیح مربوط به واژهٔ فارسی هم‌نوشت با اسم است و لزوماً ریشهٔ شخص‌نام نیست.") }
        if (cultureTitles.isNotEmpty()) item { DetailCard(Icons.Rounded.Language, "فرهنگ‌های ثبت‌شده", cultureTitles.joinToString("، ")) }
        if (entry.meaning.isBlank() && entry.origin.isBlank() && entry.lexicalMeaningFa.isBlank()) {
            item { DetailCard(Icons.Rounded.Info, "اطلاعات این اسم در حال تکمیل است", "این اسم در چند فهرست معتبر نام ثبت شده، اما هنوز معنی یا ریشهٔ پژوهش‌شده‌ای که بتوانیم با اطمینان نمایش دهیم به آن متصل نشده است.") }
        }

        if (related.isNotEmpty()) {
            item { SectionHeader("اسم‌های مشابه", "بر اساس جنسیت، حرف اول و دسته‌های مشترک") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(related, key = { it.name }) { candidate ->
                        Card(Modifier.width(145.dp).clickable { onName(candidate) }, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(candidate.name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(usefulSubtitle(candidate), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, note: String? = null) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp) }
            Text(text, lineHeight = 25.sp)
            if (!note.isNullOrBlank()) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(42.dp)) } }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!action.isNullOrBlank()) { Spacer(Modifier.height(16.dp)); FilledTonalButton(onClick = onAction) { Text(action) } }
    }
}

@Composable
private fun NewDrawer(profileUri: String, userName: String, onProfile: () -> Unit, onUserName: (String) -> Unit, onNavigate: (NewScreen) -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var draft by remember(userName) { mutableStateOf(userName) }
    Column(Modifier.fillMaxHeight().width(310.dp).padding(14.dp)) {
        Spacer(Modifier.height(14.dp))
        Box(Modifier.size(92.dp).align(Alignment.CenterHorizontally).clip(CircleShape).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
            if (profileUri.isNotBlank()) AsyncImage(Uri.parse(profileUri), "پروفایل", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Surface(Modifier.fillMaxSize(), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, Modifier.size(44.dp)) } }
        }
        if (edit) {
            OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, trailingIcon = { IconButton(onClick = { onUserName(draft.trim().ifBlank { "کاربر نام‌نامه" }); edit = false }) { Icon(Icons.Rounded.Check, null) } })
        } else {
            Row(Modifier.align(Alignment.CenterHorizontally).clickable { edit = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Person, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(userName, fontWeight = FontWeight.Bold); Spacer(Modifier.width(5.dp)); Icon(Icons.Rounded.Edit, null, Modifier.size(14.dp)) }
        }
        HorizontalDivider()
        DrawerItem("خانه", Icons.Rounded.Home) { onNavigate(NewScreen.HOME) }
        DrawerItem("اسم‌های دخترانه", Icons.Rounded.Female) { onNavigate(NewScreen.GIRLS) }
        DrawerItem("اسم‌های پسرانه", Icons.Rounded.Male) { onNavigate(NewScreen.BOYS) }
        DrawerItem("اسم پیدا کن", Icons.Rounded.AutoAwesome) { onNavigate(NewScreen.DISCOVER) }
        DrawerItem("فرهنگ‌ها و زبان‌ها", Icons.Rounded.Language) { onNavigate(NewScreen.CULTURES) }
        DrawerItem("اسم‌های پسندیده", Icons.Rounded.Favorite) { onNavigate(NewScreen.FAVORITES) }
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        DrawerItem("ارتباط با ما", Icons.Rounded.ContactMail) { onNavigate(NewScreen.CONTACT) }
        DrawerItem("درباره نرم‌افزار", Icons.Rounded.Info) { onNavigate(NewScreen.ABOUT) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(label = { Text(title) }, icon = { Icon(icon, null) }, selected = false, onClick = onClick)
}

@Composable
private fun NewAbout() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("نام‌نامه ایران", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item { Text("نام‌نامه ایران برای پیدا کردن و مقایسه اسم‌های دخترانه و پسرانه طراحی شده است. جستجو، فیلتر، پسندیده‌ها، پیشنهاد اسم، فرهنگ‌های دارای داده و اطلاعات معنی/ریشه در محیطی ساده و آفلاین در دسترس هستند.") }
        item { Text("نسخه ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun NewContact() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("برای گزارش ایراد در اسم‌ها، معنی، ریشه یا دسته‌بندی با تیم توسعه در ارتباط باشید.")
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))
        Text("گروه توسعه فناوری و نرم افزاری as Team", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("AS.Support.info@Gmail.com", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(55.dp))
    }
}

private fun usefulSubtitle(entry: NameEntry): String = when {
    entry.meaning.isNotBlank() -> entry.meaning
    entry.lexicalMeaningFa.isNotBlank() -> entry.lexicalMeaningFa
    entry.origin.isNotBlank() -> "ریشه: ${entry.origin}"
    entry.latin.isNotBlank() -> entry.latin
    else -> entry.gender.titleFa
}

@Composable
private fun genderContainer(gender: Gender): Color = when (gender) {
    Gender.FEMALE -> MaterialTheme.colorScheme.tertiaryContainer
    Gender.MALE -> MaterialTheme.colorScheme.primaryContainer
    Gender.UNISEX -> MaterialTheme.colorScheme.secondaryContainer
    Gender.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
}
