package ir.asteam.namedic.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.asteam.namedic.BuildConfig
import ir.asteam.namedic.data.NameRepository
import ir.asteam.namedic.data.UpdateChecker
import ir.asteam.namedic.model.*
import kotlinx.coroutines.launch

private enum class Screen {
    HOME,
    ALL_NAMES,
    SEARCH,
    CULTURES,
    CULTURE_NAMES,
    IRANIAN_NAMES,
    FAVORITES,
    HERITAGE,
    ABOUT,
    CONTACT,
    DETAIL,
}

/**
 * ریشه رابط کاربری برنامه.
 *
 * ناوبری در این نسخه سبک و محلی نگه داشته شده تا برنامه بدون وابستگی اضافه
 * همچنان کوچک، آفلاین و قابل نگهداری باشد.
 */
@Composable
fun NameDicApp() {
    val context = LocalContext.current
    val repository = remember { NameRepository(context) }
    val preferences = remember {
        context.getSharedPreferences("app_namedic_preferences", android.content.Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var previousScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedCulture by remember { mutableStateOf<CultureCategory?>(null) }
    var selectedName by remember { mutableStateOf<NameEntry?>(null) }
    var favorites by remember {
        mutableStateOf(preferences.getStringSet("favorites", emptySet()).orEmpty().toSet())
    }
    var profileUri by remember { mutableStateOf(preferences.getString("profile_uri", "").orEmpty()) }
    var userName by remember { mutableStateOf(preferences.getString("user_name", "کاربر نام‌نامه").orEmpty()) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    fun navigate(screen: Screen) {
        if (screen != currentScreen) previousScreen = currentScreen
        currentScreen = screen
    }

    fun toggleFavorite(name: String) {
        favorites = if (name in favorites) favorites - name else favorites + name
        preferences.edit().putStringSet("favorites", favorites).apply()
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            profileUri = uri.toString()
            preferences.edit().putString("profile_uri", profileUri).apply()
        }
    }

    BackHandler(enabled = currentScreen != Screen.HOME || drawerState.isOpen) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            currentScreen == Screen.DETAIL -> currentScreen = previousScreen
            currentScreen == Screen.CULTURE_NAMES -> currentScreen = Screen.CULTURES
            else -> currentScreen = Screen.HOME
        }
    }

    LaunchedEffect(Unit) {
        val info = UpdateChecker.check()
        if (info != null && info.latestVersionCode > BuildConfig.VERSION_CODE) {
            updateMessage = info.messageFa.ifBlank {
                "نسخه جدید ${info.latestVersionName} آماده است."
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    profileUri = profileUri,
                    userName = userName,
                    onProfileImageClick = { imagePicker.launch(arrayOf("image/*")) },
                    onUserNameChanged = {
                        userName = it
                        preferences.edit().putString("user_name", it).apply()
                    },
                    onNavigate = { screen ->
                        navigate(screen)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screenTitle(currentScreen, selectedCulture, selectedName)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentScreen == Screen.HOME) {
                                    scope.launch { drawerState.open() }
                                } else {
                                    when (currentScreen) {
                                        Screen.DETAIL -> currentScreen = previousScreen
                                        Screen.CULTURE_NAMES -> currentScreen = Screen.CULTURES
                                        else -> currentScreen = Screen.HOME
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (currentScreen == Screen.HOME) Icons.Rounded.Menu else Icons.Rounded.ArrowBack,
                                contentDescription = if (currentScreen == Screen.HOME) "منو" else "بازگشت",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.HOME -> HomeScreen(
                        repository = repository,
                        updateMessage = updateMessage,
                        onAllNames = { navigate(Screen.ALL_NAMES) },
                        onSearch = { navigate(Screen.SEARCH) },
                        onCultures = { navigate(Screen.CULTURES) },
                        onIranianNames = { navigate(Screen.IRANIAN_NAMES) },
                        onFavorites = { navigate(Screen.FAVORITES) },
                        onHeritage = { navigate(Screen.HERITAGE) },
                    )

                    Screen.ALL_NAMES -> NameListScreen(
                        names = repository.names,
                        favorites = favorites,
                        onFavorite = ::toggleFavorite,
                        onNameClick = {
                            selectedName = it
                            navigate(Screen.DETAIL)
                        },
                    )

                    Screen.SEARCH -> SearchScreen(
                        repository = repository,
                        favorites = favorites,
                        onFavorite = ::toggleFavorite,
                        onNameClick = {
                            selectedName = it
                            navigate(Screen.DETAIL)
                        },
                    )

                    Screen.CULTURES -> CultureScreen(
                        cultures = repository.cultures,
                        repository = repository,
                        onCultureClick = {
                            selectedCulture = it
                            navigate(Screen.CULTURE_NAMES)
                        },
                    )

                    Screen.CULTURE_NAMES -> NameListScreen(
                        names = repository.search("", cultureId = selectedCulture?.id),
                        favorites = favorites,
                        onFavorite = ::toggleFavorite,
                        onNameClick = {
                            selectedName = it
                            navigate(Screen.DETAIL)
                        },
                    )

                    Screen.IRANIAN_NAMES -> NameListScreen(
                        names = repository.names.filter {
                            "persian" in it.usageCultureIds || "ancient_iranian" in it.usageCultureIds
                        },
                        favorites = favorites,
                        onFavorite = ::toggleFavorite,
                        onNameClick = {
                            selectedName = it
                            navigate(Screen.DETAIL)
                        },
                    )

                    Screen.FAVORITES -> NameListScreen(
                        names = repository.names.filter { it.name in favorites },
                        favorites = favorites,
                        onFavorite = ::toggleFavorite,
                        onNameClick = {
                            selectedName = it
                            navigate(Screen.DETAIL)
                        },
                    )

                    Screen.HERITAGE -> HeritageScreen(repository.heritageItems)
                    Screen.ABOUT -> AboutScreen()
                    Screen.CONTACT -> ContactScreen()

                    Screen.DETAIL -> selectedName?.let { entry ->
                        DetailScreen(
                            entry = entry,
                            cultures = repository.cultures,
                            isFavorite = entry.name in favorites,
                            onFavorite = { toggleFavorite(entry.name) },
                        )
                    }
                }
            }
        }
    }
}

private fun screenTitle(
    screen: Screen,
    culture: CultureCategory?,
    selectedName: NameEntry?,
): String = when (screen) {
    Screen.HOME -> "نام‌نامه ایران"
    Screen.ALL_NAMES -> "همه نام‌ها"
    Screen.SEARCH -> "جستجوی نام"
    Screen.CULTURES -> "فرهنگ‌ها و زبان‌ها"
    Screen.CULTURE_NAMES -> culture?.titleFa ?: "نام‌های فرهنگ"
    Screen.IRANIAN_NAMES -> "اسامی اصیل ایرانی"
    Screen.FAVORITES -> "علاقه‌مندی‌ها"
    Screen.HERITAGE -> "میراث و شخصیت‌ها"
    Screen.ABOUT -> "درباره نرم‌افزار"
    Screen.CONTACT -> "ارتباط با ما"
    Screen.DETAIL -> selectedName?.name ?: "جزئیات نام"
}

@Composable
private fun DrawerContent(
    profileUri: String,
    userName: String,
    onProfileImageClick: () -> Unit,
    onUserNameChanged: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    var editingName by remember { mutableStateOf(false) }
    var draftName by remember(userName) { mutableStateOf(userName) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp)
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(94.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .clickable(onClick = onProfileImageClick),
            contentAlignment = Alignment.Center,
        ) {
            if (profileUri.isNotBlank()) {
                AsyncImage(
                    model = Uri.parse(profileUri),
                    contentDescription = "تصویر پروفایل",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (editingName) {
            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val clean = draftName.trim().ifBlank { "کاربر نام‌نامه" }
                            onUserNameChanged(clean)
                            editingName = false
                        },
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "ذخیره نام")
                    }
                },
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { editingName = true }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(userName, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.Edit, contentDescription = "ویرایش نام", modifier = Modifier.size(15.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("خانه") },
            icon = { Icon(Icons.Rounded.Home, null) },
            selected = false,
            onClick = { onNavigate(Screen.HOME) },
        )
        NavigationDrawerItem(
            label = { Text("همه نام‌ها") },
            icon = { Icon(Icons.Rounded.List, null) },
            selected = false,
            onClick = { onNavigate(Screen.ALL_NAMES) },
        )
        NavigationDrawerItem(
            label = { Text("جستجو") },
            icon = { Icon(Icons.Rounded.Search, null) },
            selected = false,
            onClick = { onNavigate(Screen.SEARCH) },
        )
        NavigationDrawerItem(
            label = { Text("فرهنگ‌ها و زبان‌ها") },
            icon = { Icon(Icons.Rounded.Language, null) },
            selected = false,
            onClick = { onNavigate(Screen.CULTURES) },
        )
        NavigationDrawerItem(
            label = { Text("علاقه‌مندی‌ها") },
            icon = { Icon(Icons.Rounded.Favorite, null) },
            selected = false,
            onClick = { onNavigate(Screen.FAVORITES) },
        )
        NavigationDrawerItem(
            label = { Text("میراث و شخصیت‌ها") },
            icon = { Icon(Icons.Rounded.AutoStories, null) },
            selected = false,
            onClick = { onNavigate(Screen.HERITAGE) },
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text("ارتباط با ما") },
            icon = { Icon(Icons.Rounded.ContactMail, null) },
            selected = false,
            onClick = { onNavigate(Screen.CONTACT) },
        )
        NavigationDrawerItem(
            label = { Text("درباره نرم‌افزار") },
            icon = { Icon(Icons.Rounded.Info, null) },
            selected = false,
            onClick = { onNavigate(Screen.ABOUT) },
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HomeScreen(
    repository: NameRepository,
    updateMessage: String?,
    onAllNames: () -> Unit,
    onSearch: () -> Unit,
    onCultures: () -> Unit,
    onIranianNames: () -> Unit,
    onFavorites: () -> Unit,
    onHeritage: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "فرهنگ‌نامه نام‌ها و میراث ایران",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("معنی، ریشه، فرهنگ استفاده و نام‌های تاریخی؛ با تفکیک ریشه واژه از فرهنگ محل استفاده.")
                    Text(
                        "${repository.names.size} نام • ${repository.names.count { it.meaning.isNotBlank() }} رکورد دارای معنی مستقیم • ${repository.names.count { it.lexicalMeaningFa.isNotBlank() }} رکورد دارای داده واژگانی • ${repository.cultures.size} دسته",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (!updateMessage.isNullOrBlank()) {
            item {
                AssistChip(
                    onClick = {},
                    label = { Text(updateMessage) },
                    leadingIcon = { Icon(Icons.Rounded.SystemUpdate, null) },
                )
            }
        }

        item {
            Text("دسترسی سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAction("همه نام‌ها", Icons.Rounded.List, Modifier.weight(1f), onAllNames)
                QuickAction("جستجو", Icons.Rounded.Search, Modifier.weight(1f), onSearch)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAction("فرهنگ‌ها", Icons.Rounded.Language, Modifier.weight(1f), onCultures)
                QuickAction("اصیل ایرانی", Icons.Rounded.AccountBalance, Modifier.weight(1f), onIranianNames)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAction("علاقه‌مندی", Icons.Rounded.Favorite, Modifier.weight(1f), onFavorites)
                QuickAction("میراث", Icons.Rounded.AutoStories, Modifier.weight(1f), onHeritage)
            }
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Text(title, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SearchScreen(
    repository: NameRepository,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onNameClick: (NameEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Gender?>(null) }

    val results = remember(query, gender, repository.names) {
        repository.search(query, gender)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("نام، معنی، واژهٔ هم‌نام، ریشه یا نوشتار لاتین") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = gender == null, onClick = { gender = null }, label = { Text("همه") })
            FilterChip(selected = gender == Gender.FEMALE, onClick = { gender = Gender.FEMALE }, label = { Text("دخترانه") })
            FilterChip(selected = gender == Gender.MALE, onClick = { gender = Gender.MALE }, label = { Text("پسرانه") })
            FilterChip(selected = gender == Gender.UNISEX, onClick = { gender = Gender.UNISEX }, label = { Text("مشترک") })
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "${results.size} نتیجه",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(results, key = { it.name }) { entry ->
                NameRow(
                    entry = entry,
                    isFavorite = entry.name in favorites,
                    onFavorite = { onFavorite(entry.name) },
                    onClick = onNameClick,
                )
            }
        }
    }
}

@Composable
private fun CultureScreen(
    cultures: List<CultureCategory>,
    repository: NameRepository,
    onCultureClick: (CultureCategory) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Text(
                "این دسته‌ها برای مرور رابط کاربری هستند. فرهنگ استفاده و ریشه واژه دو مفهوم جدا محسوب می‌شوند.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(cultures, key = { it.id }) { culture ->
            val count = repository.search("", cultureId = culture.id).size
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCultureClick(culture) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(culture.titleFa, fontWeight = FontWeight.Bold)
                        Text(culture.subtitleFa, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("$count", color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Rounded.ChevronLeft, null)
                }
            }
        }
    }
}

@Composable
private fun NameListScreen(
    names: List<NameEntry>,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onNameClick: (NameEntry) -> Unit,
) {
    if (names.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("موردی برای نمایش وجود ندارد.")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(names, key = { it.name }) { entry ->
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
                    when {
                        entry.meaning.isNotBlank() -> add(entry.meaning)
                        entry.lexicalMeaningFa.isNotBlank() -> add("واژهٔ هم‌نام: ${entry.lexicalMeaningFa}")
                        entry.latin.isNotBlank() -> add(entry.latin)
                    }
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

/** جزئیات نام؛ ریشه، فرهنگ استفاده و داده واژگانی عمداً مستقل نمایش داده می‌شوند. */
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
                    if (entry.meaning.isNotBlank()) InfoRow("معنی / توضیح نام", entry.meaning)
                    if (entry.origin.isNotBlank()) InfoRow("ریشه", entry.origin)
                    if (cultureNames.isNotEmpty()) InfoRow("فرهنگ / فهرست", cultureNames.joinToString("، "))
                    if (entry.latin.isNotBlank()) InfoRow("نوشتار لاتین", entry.latin)
                    if (entry.pronunciation.isNotBlank()) InfoRow("تلفظ", entry.pronunciation)

                    if (entry.lexicalMeaningFa.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        InfoRow("هم‌معنی‌های واژهٔ هم‌نام", entry.lexicalMeaningFa)
                        if (entry.lexicalAntonymsFa.isNotBlank()) {
                            InfoRow("متضادهای واژهٔ هم‌نام", entry.lexicalAntonymsFa)
                        }
                        Text(
                            "این بخش دربارهٔ واژهٔ فارسیِ هم‌نوشت با نام است و به‌تنهایی معنی یا ریشهٔ شخص‌نام را اثبات نمی‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (entry.tags.isNotEmpty()) InfoRow("برچسب‌ها", entry.tags.joinToString("، "))
                    if (entry.sourceTitle.isNotBlank()) InfoRow("منابع داده", entry.sourceTitle)
                    if (entry.notes.isNotBlank()) InfoRow("یادداشت پژوهش", entry.notes)
                    if (entry.meaning.isBlank()) {
                        Text(
                            if (entry.origin.isBlank()) {
                                "برای معنی مستقیم و ریشه این نام هنوز منبع معتبر پژوهشی به رکورد متصل نشده است."
                            } else {
                                "برای معنی مستقیم این نام هنوز منبع معتبر پژوهشی به رکورد متصل نشده است."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                "رایج بودن یک نام در یک فرهنگ، هم‌نوشت بودن با یک واژه، و ریشه زبانی سه ویژگی جدا هستند و در نام‌نامه ایران مستقل ثبت می‌شوند.",
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
                    if (item.era.isNotBlank()) Text(item.era, style = MaterialTheme.typography.labelMedium)
                    Text(item.summary)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("نام‌نامه ایران", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "نام‌نامه ایران یک فرهنگ‌نامه آفلاین و چندمنبعی برای مرور نام‌ها، معنی مستقیمِ منبع‌دار، ریشه، تلفظ، فرهنگ استفاده و میراث نام‌های ایران است. دادهٔ واژه‌های هم‌نام نیز جدا از معنی شخص‌نام نمایش داده می‌شود تا از نسبت‌دادن معنی یا ریشه نادرست جلوگیری شود.",
        )
        Text("نسخه ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ContactScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("برای گزارش خطا در معنی، ریشه، تلفظ یا دسته‌بندی نام‌ها با تیم توسعه در ارتباط باشید.")
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))
        Text(
            "گروه توسعه فناوری و نرم افزاری as Team",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "AS.Support.info@Gmail.com",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(55.dp))
    }
}
