@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ir.asteam.namedic.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.asteam.namedic.BuildConfig
import ir.asteam.namedic.data.NameRepository
import ir.asteam.namedic.data.UpdateChecker
import ir.asteam.namedic.model.CultureCategory
import ir.asteam.namedic.model.Gender
import ir.asteam.namedic.model.HeritageItem
import ir.asteam.namedic.model.HeritageType
import ir.asteam.namedic.model.NameEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** رابط اصلی برنامه؛ داده‌ها آفلاین‌اند و فقط بررسی نسخه به اینترنت نیاز دارد. */
@Composable
fun NameDicApp() {
    val context = LocalContext.current
    val repo = remember { NameRepository(context.applicationContext) }
    val prefs = remember { context.getSharedPreferences("namedic_prefs", Context.MODE_PRIVATE) }
    val favorites = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favorite_names", emptySet()).orEmpty()) } }
    var route by remember { mutableStateOf(R.HOME) }
    var parentRoute by remember { mutableStateOf(R.HOME) }
    var culture by remember { mutableStateOf<CultureCategory?>(null) }
    var selected by remember { mutableStateOf<NameEntry?>(null) }
    var profileUri by remember { mutableStateOf(prefs.getString("profile_uri", null)) }
    var update by remember { mutableStateOf<UpdatePrompt?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            profileUri = uri.toString()
            prefs.edit().putString("profile_uri", uri.toString()).apply()
        }
    }

    LaunchedEffect(Unit) {
        val latest = withContext(Dispatchers.IO) { UpdateChecker.fetchLatest() }
        if (latest != null && latest.latestVersionCode > BuildConfig.VERSION_CODE) {
            update = UpdatePrompt(latest.latestVersionName, latest.messageFa, latest.downloadUrl)
        }
    }

    fun saveFavorites() = prefs.edit().putStringSet("favorite_names", favorites.toSet()).apply()
    fun open(routeName: String) {
        selected = null
        culture = null
        route = routeName
        scope.launch { drawer.close() }
    }
    fun openName(item: NameEntry) {
        parentRoute = route
        selected = item
        route = R.DETAIL
    }
    fun goBack() {
        when {
            route == R.DETAIL -> { selected = null; route = parentRoute }
            culture != null -> { culture = null; route = R.CULTURES }
            else -> route = R.HOME
        }
    }

    BackHandler(enabled = route != R.HOME) { goBack() }

    update?.let { item ->
        AlertDialog(
            onDismissRequest = { update = null },
            title = { Text("نسخه ${item.version} آماده است") },
            text = { Text(item.message) },
            dismissButton = { TextButton(onClick = { update = null }) { Text("بعداً") } },
            confirmButton = {
                TextButton(onClick = {
                    if (item.url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                    update = null
                }) { Text("مشاهده نسخه") }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            AppDrawer(
                profileUri = profileUri,
                currentRoute = route,
                onPickPhoto = { picker.launch(arrayOf("image/*")) },
                onNavigate = ::open,
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleFor(route, culture, selected), maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = if (route == R.HOME) ({ scope.launch { drawer.open() } }) else ::goBack) {
                            Icon(if (route == R.HOME) Icons.Rounded.Menu else Icons.Rounded.ArrowBack, null)
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (route) {
                    R.HOME -> HomeScreen(repo, ::openName, { culture = it; route = R.NAMES }, ::open)
                    R.IRANIAN -> NamesScreen(
                        "اسامی اصیل ایرانی",
                        repo.names.filter { "persian" in it.usageCultureIds || "ancient_iranian" in it.usageCultureIds },
                        favorites,
                        { name -> if (name in favorites) favorites.remove(name) else favorites.add(name); saveFavorites() },
                        ::openName,
                    )
                    R.CULTURES -> CulturesScreen(repo) { culture = it; route = R.NAMES }
                    R.NAMES -> NamesScreen(
                        culture?.titleFa ?: "نام‌ها",
                        culture?.let { repo.search("", cultureId = it.id) } ?: repo.names,
                        favorites,
                        { name -> if (name in favorites) favorites.remove(name) else favorites.add(name); saveFavorites() },
                        ::openName,
                    )
                    R.HEROES -> HeritageScreen(repo.heritageItems.filter { it.type == HeritageType.HERO })
                    R.ANIMALS -> HeritageScreen(repo.heritageItems.filter { it.type != HeritageType.HERO })
                    R.FAVORITES -> NamesScreen(
                        "علاقه‌مندی‌ها",
                        repo.names.filter { it.name in favorites },
                        favorites,
                        { name -> favorites.remove(name); saveFavorites() },
                        ::openName,
                    )
                    R.ABOUT -> AboutScreen()
                    R.CONTACT -> ContactScreen()
                    R.DETAIL -> selected?.let { entry ->
                        DetailScreen(entry, repo.cultures, entry.name in favorites) {
                            if (entry.name in favorites) favorites.remove(entry.name) else favorites.add(entry.name)
                            saveFavorites()
                        }
                    }
                }
            }
        }
    }
}

private object R {
    const val HOME = "home"; const val IRANIAN = "iranian"; const val CULTURES = "cultures"
    const val NAMES = "names"; const val HEROES = "heroes"; const val ANIMALS = "animals"
    const val FAVORITES = "favorites"; const val ABOUT = "about"; const val CONTACT = "contact"; const val DETAIL = "detail"
}
private data class UpdatePrompt(val version: String, val message: String, val url: String)
private fun titleFor(route: String, culture: CultureCategory?, name: NameEntry?) = when (route) {
    R.HOME -> "نام‌نامه ایران"; R.IRANIAN -> "اسامی اصیل ایرانی"; R.CULTURES -> "زبان‌ها و فرهنگ‌ها"
    R.NAMES -> culture?.titleFa ?: "نام‌ها"; R.HEROES -> "قهرمانان و اساطیر"; R.ANIMALS -> "جانوران فرهنگی"
    R.FAVORITES -> "علاقه‌مندی‌ها"; R.ABOUT -> "درباره نرم‌افزار"; R.CONTACT -> "ارتباط با ما"
    R.DETAIL -> name?.name ?: "جزئیات"; else -> "نام‌نامه ایران"
}

@Composable
private fun AppDrawer(profileUri: String?, currentRoute: String, onPickPhoto: () -> Unit, onNavigate: (String) -> Unit) {
    ModalDrawerSheet {
        LazyColumn(Modifier.padding(horizontal = 12.dp), contentPadding = PaddingValues(vertical = 18.dp)) {
            item { ProfileHeader(profileUri, onPickPhoto); HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
            item { DrawerRow("خانه", Icons.Rounded.Home, R.HOME, currentRoute, onNavigate) }
            item { DrawerRow("اسامی اصیل ایرانی", Icons.Rounded.Book, R.IRANIAN, currentRoute, onNavigate) }
            item { DrawerRow("زبان‌ها و فرهنگ‌ها", Icons.Rounded.Language, R.CULTURES, currentRoute, onNavigate) }
            item { DrawerRow("قهرمانان و اساطیر", Icons.Rounded.EmojiEvents, R.HEROES, currentRoute, onNavigate) }
            item { DrawerRow("جانوران و موجودات فرهنگی", Icons.Rounded.Pets, R.ANIMALS, currentRoute, onNavigate) }
            item { DrawerRow("علاقه‌مندی‌ها", Icons.Rounded.Favorite, R.FAVORITES, currentRoute, onNavigate) }
            item {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                DrawerRow("درباره نرم‌افزار", Icons.Rounded.Info, R.ABOUT, currentRoute, onNavigate)
                DrawerRow("ارتباط با ما", Icons.Rounded.Email, R.CONTACT, currentRoute, onNavigate)
            }
        }
    }
}

@Composable
private fun ProfileHeader(uri: String?, onPick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(92.dp).clickable(onClick = onPick), contentAlignment = Alignment.BottomEnd) {
            ProfileImage(uri)
            Icon(Icons.Rounded.PhotoCamera, "انتخاب عکس", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Person, null, modifier = Modifier.size(18.dp)); Text("کاربر نام‌نامه", fontWeight = FontWeight.Bold)
        }
        Text("برای تغییر تصویر روی عکس بزنید", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileImage(uri: String?) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = if (uri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), "عکس پروفایل", Modifier.size(84.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        ElevatedCard(Modifier.size(84.dp), shape = CircleShape) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DrawerRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, route: String, current: String, onNavigate: (String) -> Unit) {
    NavigationDrawerItem(label = { Text(label) }, icon = { Icon(icon, null) }, selected = route == current, onClick = { onNavigate(route) })
}

@Composable
private fun HomeScreen(repo: NameRepository, onName: (NameEntry) -> Unit, onCulture: (CultureCategory) -> Unit, onNavigate: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { if (query.isBlank()) emptyList() else repo.search(query).take(40) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("فرهنگ‌نامه نام‌ها و میراث ایران", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("معنی، ریشه، فرهنگ استفاده و نام‌های تاریخی؛ با تفکیک ریشه واژه از فرهنگ محل استفاده.")
                    Text("${repo.names.size} نام • ${repo.cultures.size} دسته", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("جستجوی نام، معنی یا ریشه") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true) }
        if (query.isNotBlank()) {
            if (results.isEmpty()) item { Empty("نتیجه‌ای پیدا نشد.") }
            items(results, key = { it.name }) { NameRow(it, false, null, onName) }
        } else {
            item {
                Text("دسترسی سریع", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AssistChip({ onNavigate(R.IRANIAN) }, { Text("اسامی ایرانی") }, leadingIcon = { Icon(Icons.Rounded.Book, null) }) }
                    item { AssistChip({ onNavigate(R.CULTURES) }, { Text("فرهنگ‌ها") }, leadingIcon = { Icon(Icons.Rounded.Language, null) }) }
                    item { AssistChip({ onNavigate(R.HEROES) }, { Text("قهرمانان") }, leadingIcon = { Icon(Icons.Rounded.EmojiEvents, null) }) }
                }
            }
            item { Text("زبان‌ها و فرهنگ‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(repo.cultures.take(8), key = { it.id }) { CultureRow(it, repo.search("", cultureId = it.id).size, onCulture) }
            item { TextButton({ onNavigate(R.CULTURES) }, Modifier.fillMaxWidth()) { Text("نمایش همه دسته‌ها") } }
        }
    }
}

@Composable
private fun CulturesScreen(repo: NameRepository, onCulture: (CultureCategory) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("دسته‌ها بر اساس کاربرد فرهنگی/زبانی‌اند؛ ریشه واقعی هر نام جداگانه ثبت می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(repo.cultures, key = { it.id }) { CultureRow(it, repo.search("", cultureId = it.id).size, onCulture) }
    }
}

@Composable
private fun CultureRow(culture: CultureCategory, count: Int, onClick: (CultureCategory) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable { onClick(culture) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.People, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(culture.titleFa, fontWeight = FontWeight.Bold)
                Text(culture.subtitleFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(count.toString(), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NamesScreen(title: String, source: List<NameEntry>, favorites: List<String>, onFavorite: (String) -> Unit, onName: (NameEntry) -> Unit) {
    var query by remember(title) { mutableStateOf("") }
    var gender by remember(title) { mutableStateOf<Gender?>(null) }
    val list = remember(source, query, gender) {
        val q = query.trim().lowercase().replace('ي', 'ی').replace('ك', 'ک')
        source.filter { e ->
            (q.isBlank() || listOf(e.name, e.meaning, e.origin, e.tags.joinToString(" ")).any { it.lowercase().replace('ي', 'ی').replace('ك', 'ک').contains(q) }) &&
                (gender == null || e.gender == gender)
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("جستجو در $title") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { FilterChip(gender == null, { gender = null }, { Text("همه") }) }
                item { FilterChip(gender == Gender.FEMALE, { gender = Gender.FEMALE }, { Text("دخترانه") }) }
                item { FilterChip(gender == Gender.MALE, { gender = Gender.MALE }, { Text("پسرانه") }) }
                item { FilterChip(gender == Gender.UNISEX, { gender = Gender.UNISEX }, { Text("مشترک") }) }
            }
        }
        item { Text("${list.size} نتیجه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (list.isEmpty()) item { Empty("در این دسته هنوز رکورد بررسی‌شده کافی نداریم.") }
        items(list, key = { it.name }) { NameRow(it, it.name in favorites, { onFavorite(it.name) }, onName) }
    }
}

@Composable
private fun NameRow(entry: NameEntry, favorite: Boolean, onFavorite: (() -> Unit)?, onClick: (NameEntry) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable { onClick(entry) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${entry.gender.titleFa} • ${entry.meaning}", maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            onFavorite?.let { callback -> IconButton(callback) { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "علاقه‌مندی") } }
        }
    }
}

@Composable
private fun DetailScreen(entry: NameEntry, cultures: List<CultureCategory>, favorite: Boolean, onFavorite: () -> Unit) {
    val cultureNames = cultures.filter { it.id in entry.usageCultureIds }.map { it.titleFa }.ifEmpty { listOf("هنوز دسته‌بندی نشده") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(entry.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); if (entry.latin.isNotBlank()) Text(entry.latin) }
                        IconButton(onFavorite) { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "علاقه‌مندی") }
                    }
                    Info("جنسیت", entry.gender.titleFa); Info("معنی / توضیح", entry.meaning); Info("ریشه", entry.origin)
                    Info("فرهنگ استفاده", cultureNames.joinToString("، ")); Info("وضعیت داده", entry.verificationStatus.titleFa)
                    if (entry.pronunciation.isNotBlank()) Info("تلفظ", entry.pronunciation)
                    if (entry.tags.isNotEmpty()) Info("برچسب‌ها", entry.tags.joinToString("، "))
                    if (entry.sourceTitle.isNotBlank()) Info("منبع", entry.sourceTitle)
                    if (entry.notes.isNotBlank()) Info("یادداشت پژوهش", entry.notes)
                }
            }
        }
        item { Text("رایج بودن یک نام در یک فرهنگ، به‌تنهایی ریشه زبانی آن را اثبات نمی‌کند؛ این دو مورد جدا نگهداری می‌شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun Info(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(value) } }

@Composable
private fun HeritageScreen(source: List<HeritageItem>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("این بخش در نسخه‌های بعدی به دانشنامه تاریخی کامل تبدیل می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(source, key = { it.id }) { item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(item.type.titleFa, color = MaterialTheme.colorScheme.primary); Text(item.summary)
                    if (item.era.isNotBlank()) Text(item.era, style = MaterialTheme.typography.bodySmall)
                    if (item.sourceTitle.isNotBlank()) Text("منبع: ${item.sourceTitle}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("نام‌نامه ایران", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("فرهنگ‌نامه آفلاین نام‌ها، ریشه‌ها، فرهنگ‌های ایران و میراث تاریخی و حماسی؛ داده‌ها به‌تدریج با منبع گسترش می‌یابند."); Text("نسخه ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ContactScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ارتباط با ما", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("برای پیشنهاد نام، گزارش خطای معنی/ریشه یا همکاری پژوهشی با تیم توسعه در ارتباط باشید.")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary); Text("AS.Support.info@Gmail.com", color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.weight(1f)); HorizontalDivider(); Spacer(Modifier.height(10.dp))
        Text("گروه توسعه فناوری و نرم افزاری as Team", Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("AS.Support.info@Gmail.com", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(54.dp))
    }
}

@Composable private fun Empty(text: String) { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
