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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * ریشه رابط کاربری برنامه.
 * داده‌های فرهنگ‌نامه از assets خوانده می‌شوند و برای استفاده عادی به اینترنت نیاز ندارند.
 */
@Composable
fun NameDicApp() {
    val context = LocalContext.current
    val repository = remember { NameRepository(context.applicationContext) }
    val preferences = remember {
        context.getSharedPreferences("namedic_preferences", Context.MODE_PRIVATE)
    }
    val favoriteNames = remember {
        mutableStateListOf<String>().apply {
            addAll(preferences.getStringSet("favorite_names", emptySet()).orEmpty())
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(Screen.HOME) }
    var previousScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedCulture by remember { mutableStateOf<CultureCategory?>(null) }
    var selectedName by remember { mutableStateOf<NameEntry?>(null) }
    var profileUri by remember { mutableStateOf(preferences.getString("profile_uri", null)) }
    var updatePrompt by remember { mutableStateOf<UpdatePrompt?>(null) }

    // انتخاب عکس پروفایل بدون نیاز به مجوز عمومی حافظه.
    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            profileUri = uri.toString()
            preferences.edit().putString("profile_uri", uri.toString()).apply()
        }
    }

    // بررسی نسخه فقط یک قابلیت کمکی است؛ شکست شبکه روی فرهنگ‌نامه آفلاین اثر ندارد.
    LaunchedEffect(Unit) {
        val latest = withContext(Dispatchers.IO) { UpdateChecker.fetchLatest() }
        if (latest != null && latest.latestVersionCode > BuildConfig.VERSION_CODE) {
            updatePrompt = UpdatePrompt(
                version = latest.latestVersionName,
                message = latest.messageFa,
                url = latest.downloadUrl,
            )
        }
    }

    fun persistFavorites() {
        preferences.edit().putStringSet("favorite_names", favoriteNames.toSet()).apply()
    }

    fun navigate(destination: Screen) {
        selectedName = null
        if (destination != Screen.NAMES) selectedCulture = null
        screen = destination
        scope.launch { drawerState.close() }
    }

    fun openName(entry: NameEntry) {
        previousScreen = screen
        selectedName = entry
        screen = Screen.DETAIL
    }

    fun goBack() {
        when (screen) {
            Screen.DETAIL -> {
                selectedName = null
                screen = previousScreen
            }
            Screen.NAMES -> {
                selectedCulture = null
                screen = Screen.CULTURES
            }
            else -> screen = Screen.HOME
        }
    }

    BackHandler(enabled = screen != Screen.HOME) { goBack() }

    updatePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { updatePrompt = null },
            title = { Text("نسخه ${prompt.version} منتشر شده است") },
            text = { Text(prompt.message) },
            dismissButton = {
                TextButton(onClick = { updatePrompt = null }) { Text("بعداً") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (prompt.url.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(prompt.url)))
                        }
                        updatePrompt = null
                    },
                ) { Text("مشاهده نسخه") }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                profileUri = profileUri,
                currentScreen = screen,
                onPickProfile = { profilePicker.launch(arrayOf("image/*")) },
                onNavigate = ::navigate,
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screenTitle(screen, selectedCulture, selectedName), maxLines = 1) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (screen == Screen.HOME) {
                                    scope.launch { drawerState.open() }
                                } else {
                                    goBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (screen == Screen.HOME) Icons.Rounded.Menu else Icons.Rounded.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        repository = repository,
                        onNameClick = ::openName,
                        onCultureClick = {
                            selectedCulture = it
                            screen = Screen.NAMES
                        },
                        onNavigate = ::navigate,
                    )

                    Screen.ALL_NAMES -> NamesScreen(
                        title = "همه نام‌ها",
                        source = repository.names,
                        favorites = favoriteNames,
                        onFavorite = { name ->
                            if (name in favoriteNames) favoriteNames.remove(name) else favoriteNames.add(name)
                            persistFavorites()
                        },
                        onNameClick = ::openName,
                    )

                    Screen.IRANIAN_NAMES -> NamesScreen(
                        title = "اسامی اصیل ایرانی",
                        source = repository.names.filter {
                            "persian" in it.usageCultureIds || "ancient_iranian" in it.usageCultureIds
                        },
                        favorites = favoriteNames,
                        onFavorite = { name ->
                            if (name in favoriteNames) favoriteNames.remove(name) else favoriteNames.add(name)
                            persistFavorites()
                        },
                        onNameClick = ::openName,
                    )

                    Screen.CULTURES -> CulturesScreen(repository) {
                        selectedCulture = it
                        screen = Screen.NAMES
                    }

                    Screen.NAMES -> NamesScreen(
                        title = selectedCulture?.titleFa ?: "نام‌ها",
                        source = selectedCulture?.let {
                            repository.search(query = "", cultureId = it.id)
                        } ?: repository.names,
                        favorites = favoriteNames,
                        onFavorite = { name ->
                            if (name in favoriteNames) favoriteNames.remove(name) else favoriteNames.add(name)
                            persistFavorites()
                        },
                        onNameClick = ::openName,
                    )

                    Screen.HEROES -> HeritageScreen(
                        repository.heritageItems.filter { it.type == HeritageType.HERO },
                    )

                    Screen.ANIMALS -> HeritageScreen(
                        repository.heritageItems.filter { it.type != HeritageType.HERO },
                    )

                    Screen.FAVORITES -> NamesScreen(
                        title = "علاقه‌مندی‌ها",
                        source = repository.names.filter { it.name in favoriteNames },
                        favorites = favoriteNames,
                        onFavorite = { name ->
                            favoriteNames.remove(name)
                            persistFavorites()
                        },
                        onNameClick = ::openName,
                    )

                    Screen.ABOUT -> AboutScreen()
                    Screen.CONTACT -> ContactScreen()
                    Screen.DETAIL -> selectedName?.let { entry ->
                        DetailScreen(
                            entry = entry,
                            cultures = repository.cultures,
                            isFavorite = entry.name in favoriteNames,
                            onFavorite = {
                                if (entry.name in favoriteNames) favoriteNames.remove(entry.name)
                                else favoriteNames.add(entry.name)
                                persistFavorites()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** مسیرهای ساده داخلی برنامه؛ کتابخانه Navigation برای نسخه پایه ضروری نیست. */
private enum class Screen {
    HOME,
    ALL_NAMES,
    IRANIAN_NAMES,
    CULTURES,
    NAMES,
    HEROES,
    ANIMALS,
    FAVORITES,
    ABOUT,
    CONTACT,
    DETAIL,
}

private data class UpdatePrompt(
    val version: String,
    val message: String,
    val url: String,
)

private fun screenTitle(
    screen: Screen,
    culture: CultureCategory?,
    name: NameEntry?,
): String = when (screen) {
    Screen.HOME -> "نام‌نامه ایران"
    Screen.ALL_NAMES -> "همه نام‌ها"
    Screen.IRANIAN_NAMES -> "اسامی اصیل ایرانی"
    Screen.CULTURES -> "زبان‌ها و فرهنگ‌ها"
    Screen.NAMES -> culture?.titleFa ?: "نام‌ها"
    Screen.HEROES -> "قهرمانان و اساطیر"
    Screen.ANIMALS -> "جانوران و موجودات فرهنگی"
    Screen.FAVORITES -> "علاقه‌مندی‌ها"
    Screen.ABOUT -> "درباره نرم‌افزار"
    Screen.CONTACT -> "ارتباط با ما"
    Screen.DETAIL -> name?.name ?: "جزئیات نام"
}

/** Drawer مشترک پروژه با پروفایل، بخش اختصاصی برنامه و بخش ارتباطی. */
@Composable
private fun AppDrawer(
    profileUri: String?,
    currentScreen: Screen,
    onPickProfile: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    ModalDrawerSheet {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 18.dp),
        ) {
            item {
                ProfileHeader(profileUri = profileUri, onPickProfile = onPickProfile)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item { DrawerItem("خانه", Icons.Rounded.Home, Screen.HOME, currentScreen, onNavigate) }
            item { DrawerItem("همه نام‌ها", Icons.Rounded.People, Screen.ALL_NAMES, currentScreen, onNavigate) }
            item { DrawerItem("اسامی اصیل ایرانی", Icons.Rounded.Book, Screen.IRANIAN_NAMES, currentScreen, onNavigate) }
            item { DrawerItem("زبان‌ها و فرهنگ‌ها", Icons.Rounded.Language, Screen.CULTURES, currentScreen, onNavigate) }
            item { DrawerItem("قهرمانان و اساطیر", Icons.Rounded.EmojiEvents, Screen.HEROES, currentScreen, onNavigate) }
            item { DrawerItem("جانوران فرهنگی", Icons.Rounded.Pets, Screen.ANIMALS, currentScreen, onNavigate) }
            item { DrawerItem("علاقه‌مندی‌ها", Icons.Rounded.Favorite, Screen.FAVORITES, currentScreen, onNavigate) }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                DrawerItem("درباره نرم‌افزار", Icons.Rounded.Info, Screen.ABOUT, currentScreen, onNavigate)
                DrawerItem("ارتباط با ما", Icons.Rounded.Email, Screen.CONTACT, currentScreen, onNavigate)
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    destination: Screen,
    current: Screen,
    onNavigate: (Screen) -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = destination == current,
        onClick = { onNavigate(destination) },
    )
}

/** هدر پروفایل؛ لمس مستقیم تصویر، انتخاب‌گر عکس را باز می‌کند. */
@Composable
private fun ProfileHeader(
    profileUri: String?,
    onPickProfile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clickable(onClick = onPickProfile),
            contentAlignment = Alignment.BottomEnd,
        ) {
            ProfileImage(profileUri)
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = "انتخاب عکس پروفایل",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("کاربر نام‌نامه", fontWeight = FontWeight.Bold)
        }
        Text("برای تغییر تصویر روی عکس بزنید", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileImage(profileUri: String?) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = profileUri) {
        value = if (profileUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(profileUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    if (image != null) {
        Image(
            bitmap = image!!,
            contentDescription = "عکس پروفایل",
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        ElevatedCard(modifier = Modifier.size(84.dp), shape = CircleShape) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** خانه شامل معرفی، جستجوی سریع و دسته‌های اصلی است. */
@Composable
private fun HomeScreen(
    repository: NameRepository,
    onNameClick: (NameEntry) -> Unit,
    onCultureClick: (CultureCategory) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) {
        if (query.isBlank()) emptyList() else repository.search(query).take(50)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        "فرهنگ‌نامه نام‌ها و میراث ایران",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("معنی، ریشه، فرهنگ استفاده و نام‌های تاریخی؛ با تفکیک ریشه واژه از فرهنگ محل استفاده.")
                    Text(
                        "${repository.names.size} نام • ${repository.names.count { it.meaning.isNotBlank() }} رکورد دارای معنی • ${repository.cultures.size} دسته",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("جستجوی نام، معنی یا ریشه") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
            )
        }

        if (query.isNotBlank()) {
            if (results.isEmpty()) item { EmptyState("نتیجه‌ای پیدا نشد.") }
            items(items = results, key = { it.name }) { entry ->
                NameRow(entry = entry, isFavorite = false, onFavorite = null, onClick = onNameClick)
            }
        } else {
            item {
                Text("دسترسی سریع", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssistChip(
                            onClick = { onNavigate(Screen.ALL_NAMES) },
                            label = { Text("همه نام‌ها") },
                            leadingIcon = { Icon(Icons.Rounded.People, contentDescription = null) },
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onNavigate(Screen.IRANIAN_NAMES) },
                            label = { Text("اسامی ایرانی") },
                            leadingIcon = { Icon(Icons.Rounded.Book, contentDescription = null) },
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onNavigate(Screen.CULTURES) },
                            label = { Text("فرهنگ‌ها") },
                            leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onNavigate(Screen.HEROES) },
                            label = { Text("قهرمانان") },
                            leadingIcon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null) },
                        )
                    }
                }
            }

            item {
                Text("زبان‌ها و فرهنگ‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(items = repository.cultures.take(8), key = { it.id }) { culture ->
                CultureRow(
                    culture = culture,
                    count = repository.search(query = "", cultureId = culture.id).size,
                    onClick = onCultureClick,
                )
            }
            item {
                TextButton(
                    onClick = { onNavigate(Screen.CULTURES) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("نمایش همه دسته‌ها") }
            }
        }
    }
}

@Composable
private fun CulturesScreen(
    repository: NameRepository,
    onCultureClick: (CultureCategory) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "دسته‌ها بر اساس کاربرد فرهنگی/زبانی‌اند؛ ریشه واقعی هر نام جداگانه ثبت می‌شود.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(items = repository.cultures, key = { it.id }) { culture ->
            CultureRow(
                culture = culture,
                count = repository.search(query = "", cultureId = culture.id).size,
                onClick = onCultureClick,
            )
        }
    }
}

@Composable
private fun CultureRow(
    culture: CultureCategory,
    count: Int,
    onClick: (CultureCategory) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(culture) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(culture.titleFa, fontWeight = FontWeight.Bold)
                Text(
                    culture.subtitleFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(count.toString(), color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** فهرست نام‌ها با جستجو و فیلتر جنسیت. */
@Composable
private fun NamesScreen(
    title: String,
    source: List<NameEntry>,
    favorites: List<String>,
    onFavorite: (String) -> Unit,
    onNameClick: (NameEntry) -> Unit,
) {
    var query by remember(title) { mutableStateOf("") }
    var gender by remember(title) { mutableStateOf<Gender?>(null) }

    val filtered = remember(source, query, gender) {
        val normalizedQuery = normalizeForSearch(query)
        source.filter { entry ->
            val matchesQuery = normalizedQuery.isBlank() || listOf(
                entry.name,
                entry.meaning,
                entry.origin,
                entry.latin,
                entry.tags.joinToString(" "),
            ).any { normalizeForSearch(it).contains(normalizedQuery) }
            val matchesGender = gender == null || entry.gender == gender
            matchesQuery && matchesGender
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("جستجو در $title") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { FilterChip(selected = gender == null, onClick = { gender = null }, label = { Text("همه") }) }
                item { FilterChip(selected = gender == Gender.FEMALE, onClick = { gender = Gender.FEMALE }, label = { Text("دخترانه") }) }
                item { FilterChip(selected = gender == Gender.MALE, onClick = { gender = Gender.MALE }, label = { Text("پسرانه") }) }
                item { FilterChip(selected = gender == Gender.UNISEX, onClick = { gender = Gender.UNISEX }, label = { Text("مشترک") }) }
            }
        }

        item {
            Text(
                "${filtered.size} نتیجه",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (filtered.isEmpty()) item { EmptyState("در این بخش هنوز رکورد کافی ثبت نشده است.") }

        items(items = filtered, key = { it.name }) { entry ->
            NameRow(
                entry = entry,
                isFavorite = entry.name in favorites,
                onFavorite = { onFavorite(entry.name) },
                onClick = onNameClick,
            )
        }
    }
}

@Composable
private fun NameRow(
    entry: NameEntry,
    isFavorite: Boolean,
    onFavorite: (() -> Unit)?,
    onClick: (NameEntry) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val subtitle = buildList {
                    add(entry.gender.titleFa)
                    if (entry.meaning.isNotBlank()) add(entry.meaning)
                    else if (entry.latin.isNotBlank()) add(entry.latin)
                }.joinToString(" • ")
                Text(
                    subtitle,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onFavorite != null) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "علاقه‌مندی",
                    )
                }
            }
        }
    }
}

/** جزئیات نام؛ ریشه و فرهنگ استفاده عمداً در دو سطر مستقل نمایش داده می‌شوند. */
@Composable
private fun DetailScreen(
    entry: NameEntry,
    cultures: List<CultureCategory>,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
) {
    val cultureNames = cultures
        .filter { it.id in entry.usageCultureIds }
        .map { it.titleFa }
        .ifEmpty { listOf("هنوز دسته‌بندی نشده") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            if (entry.latin.isNotBlank()) Text(entry.latin)
                        }
                        IconButton(onClick = onFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "علاقه‌مندی",
                            )
                        }
                    }

                    InfoRow("جنسیت", entry.gender.titleFa)
                    if (entry.meaning.isNotBlank()) InfoRow("معنی / توضیح", entry.meaning)
                    if (entry.origin.isNotBlank()) InfoRow("ریشه", entry.origin)
                    if (cultureNames.isNotEmpty()) InfoRow("فرهنگ / فهرست", cultureNames.joinToString("، "))
                    if (entry.latin.isNotBlank()) InfoRow("نوشتار لاتین", entry.latin)
                    if (entry.pronunciation.isNotBlank()) InfoRow("تلفظ", entry.pronunciation)
                    if (entry.tags.isNotEmpty()) InfoRow("برچسب‌ها", entry.tags.joinToString("، "))
                    if (entry.sourceTitle.isNotBlank()) InfoRow("منبع فهرست", entry.sourceTitle)
                    if (entry.notes.isNotBlank()) InfoRow("یادداشت پژوهش", entry.notes)
                    if (entry.meaning.isBlank() && entry.origin.isBlank()) {
                        Text(
                            "برای معنی و ریشه این نام هنوز منبع معتبر به رکورد متصل نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                "رایج بودن یک نام در یک فرهنگ، به‌تنهایی ریشه زبانی آن را اثبات نمی‌کند؛ این دو ویژگی جدا ثبت می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value)
    }
}

@Composable
private fun HeritageScreen(source: List<HeritageItem>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "این بخش در نسخه‌های بعدی به دانشنامه تاریخی کامل با سلسله‌ها و خط زمانی تبدیل می‌شود.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(items = source, key = { it.id }) { item ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(item.type.titleFa, color = MaterialTheme.colorScheme.primary)
                    Text(item.summary)
                    if (item.era.isNotBlank()) Text(item.era, style = MaterialTheme.typography.bodySmall)
                    if (item.sourceTitle.isNotBlank()) {
                        Text("منبع: ${item.sourceTitle}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("نام‌نامه ایران", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("فرهنگ‌نامه آفلاین نام‌ها، ریشه‌ها، فرهنگ‌های ایران و میراث تاریخی و حماسی؛ داده‌ها به‌تدریج با منبع گسترش می‌یابند.")
            Text("نسخه ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ContactScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ارتباط با ما", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("برای پیشنهاد نام، گزارش خطای معنی یا ریشه و همکاری پژوهشی با تیم توسعه در ارتباط باشید.")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("AS.Support.info@Gmail.com", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "گروه توسعه فناوری و نرم افزاری as Team",
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "AS.Support.info@Gmail.com",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(54.dp))
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun normalizeForSearch(value: String): String = value
    .trim()
    .lowercase()
    .replace('ي', 'ی')
    .replace('ك', 'ک')
    .replace("‌", "")
    .replace(" ", "")
