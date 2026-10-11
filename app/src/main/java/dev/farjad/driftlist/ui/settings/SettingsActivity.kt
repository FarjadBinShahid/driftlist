package dev.farjad.driftlist.ui.settings

import androidx.compose.runtime.produceState
import dev.farjad.driftlist.domain.ThemeFile
import dev.farjad.driftlist.ui.components.IconPickerSheet
import dev.farjad.driftlist.system.WallpaperArt
import dev.farjad.driftlist.domain.SearchEngine
import dev.farjad.driftlist.data.Fonts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import dev.farjad.driftlist.data.ThemeMode
import androidx.compose.material3.Slider
import androidx.compose.ui.semantics.contentDescription
import dev.farjad.driftlist.data.Look
import dev.farjad.driftlist.data.db.ThemeEntity
import dev.farjad.driftlist.system.FontStore
import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.farjad.driftlist.BuildConfig
import dev.farjad.driftlist.DriftlistApp
import dev.farjad.driftlist.data.Backup
import dev.farjad.driftlist.data.SettingsSnapshot
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.SummaryRuleEntity
import dev.farjad.driftlist.data.db.UsageRuleEntity
import dev.farjad.driftlist.system.QuickLockService
import dev.farjad.driftlist.system.SystemActions
import dev.farjad.driftlist.system.apps.IconPack
import dev.farjad.driftlist.ui.components.AppMultiPicker
import dev.farjad.driftlist.ui.components.Explainers
import dev.farjad.driftlist.ui.components.PermissionDialog
import dev.farjad.driftlist.ui.components.SheetAppRow
import dev.farjad.driftlist.ui.theme.DriftlistTheme
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SettingsUiState(
    val settings: SettingsSnapshot = SettingsSnapshot(),
    val apps: List<AppEntity> = emptyList(),
    val usageRules: Set<String> = emptySet(),
    val summaryRules: Set<String> = emptySet(),
    val themes: List<ThemeEntity> = emptyList(),
    val wallpaperSeed: Int? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val c = (application as DriftlistApp).container
    val icons = c.icons

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            c.settings.flow,
            c.db.appDao().observeAll(),
            c.db.usageDao().observeRules(),
            c.db.summaryDao().observeRules(),
        ) { s, apps, rules, summary ->
            SettingsUiState(
                s,
                apps.sortedBy { it.label.lowercase() },
                rules.mapTo(HashSet()) { it.packageName },
                summary.mapTo(HashSet()) { it.packageName },
            )
        },
        c.db.themeDao().observeAll(),
        c.wallpaper.state,
    ) { s, themes, wallpaper -> s.copy(themes = themes, wallpaperSeed = wallpaper.seed) }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun iconPacks(): List<IconPack> = c.iconPacks.installed()
    fun setIconPack(pkg: String?) = viewModelScope.launch { c.settings.setIconPack(pkg) }
    fun setSwipeUpSearch(v: Boolean) = viewModelScope.launch { c.settings.setSwipeUpSearch(v) }
    fun setSearchIncludesHidden(v: Boolean) = viewModelScope.launch { c.settings.setSearchIncludesHidden(v) }
    fun setUsageBreaker(v: Boolean) = viewModelScope.launch { c.settings.setUsageBreaker(v) }
    fun setFavoritesAtBottom(v: Boolean) = viewModelScope.launch { c.settings.setFavoritesAtBottom(v) }
    fun setSearchEngine(engine: SearchEngine) = viewModelScope.launch { c.settings.setSearchEngine(engine) }
    fun hasContactsAccess() = c.contacts.hasPermission()
    fun setFlag(name: String, v: Boolean) = viewModelScope.launch { c.settings.setFlag(name, v) }
    fun setButtonTarget(target: String?) = viewModelScope.launch { c.settings.setButtonTarget(target) }
    fun unhide(app: AppEntity) = viewModelScope.launch { c.db.appDao().setHidden(app.id, false, System.currentTimeMillis()) }

    fun toggleUsageRule(app: AppEntity) = viewModelScope.launch {
        val current = state.value.usageRules
        val next = if (app.packageName in current) current - app.packageName else current + app.packageName
        val now = System.currentTimeMillis()
        val serials = state.value.apps.associate { it.packageName to it.userSerial }
        c.db.usageDao().setRules(next.map { pkg -> UsageRuleEntity(pkg, serials[pkg] ?: app.userSerial, now) })
    }

    fun toggleSummaryRule(app: AppEntity) = viewModelScope.launch {
        val current = state.value.summaryRules
        val next = if (app.packageName in current) current - app.packageName else current + app.packageName
        val serials = state.value.apps.associate { it.packageName to it.userSerial }
        c.db.summaryDao().setRules(next.map { pkg -> SummaryRuleEntity(pkg, serials[pkg] ?: app.userSerial) })
    }

    // --- look and themes ---
    fun setLook(look: Look) = viewModelScope.launch { c.settings.setLook(look) }

    /** Applies a look; with [useWallpaper], also sets its wallpaper on the home screen. */
    fun applyLook(look: Look, useWallpaper: Boolean) = viewModelScope.launch {
        val current = state.value.settings.look
        val next = if (useWallpaper) look else look.copy(wallpaper = current.wallpaper)
        c.settings.setLook(next)
        val art = next.wallpaper
        if (useWallpaper && art != null && art != current.wallpaper) WallpaperArt.apply(getApplication(), art, WallpaperArt.Target.Home)
    }

    suspend fun setWallpaper(id: String, target: WallpaperArt.Target): Boolean {
        val ok = WallpaperArt.apply(getApplication(), id, target)
        if (ok && target != WallpaperArt.Target.Lock) c.settings.setLook(state.value.settings.look.copy(wallpaper = id))
        return ok
    }

    fun setWallpaperBlur(level: Int) = viewModelScope.launch { c.settings.setWallpaperBlur(level) }
    fun setHeaderPosition(ratio: Float) = viewModelScope.launch { c.settings.setHeaderPosition(ratio) }
    fun importedFonts(): List<String> = FontStore.imported(getApplication())
    suspend fun importFont(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) { FontStore.import(context, uri) }
    fun deleteFont(context: Context, id: String) = FontStore.delete(context, id)

    fun saveTheme(name: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val s = state.value.settings
        c.db.themeDao().upsert(
            ThemeEntity(java.util.UUID.randomUUID().toString(), name, s.look.toJson(), s.iconPack ?: ThemeEntity.ICONS_SYSTEM, now, now),
        )
    }

    /** Applies a saved theme: its look, and its icon pack if that pack is still installed. */
    fun applyTheme(theme: ThemeEntity) = viewModelScope.launch {
        val look = Look.fromJson(theme.look)
        val previous = state.value.settings.look.wallpaper
        c.settings.setLook(look)
        look.wallpaper?.takeIf { it != previous }?.let { WallpaperArt.apply(getApplication(), it, WallpaperArt.Target.Home) }
        when (val pack = theme.iconPack) {
            null -> Unit
            ThemeEntity.ICONS_SYSTEM -> c.settings.setIconPack(null)
            else -> if (iconPacks().any { it.packageName == pack }) c.settings.setIconPack(pack)
        }
    }

    /** Writes the theme to the app cache and opens the share sheet. */
    fun shareTheme(context: Context, name: String, look: Look, iconPack: String?) {
        val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "_").trim().ifEmpty { "Theme" }
        val file = java.io.File(dir, safe + ThemeFile.EXTENSION)
        file.writeText(ThemeFile.write(name, look, iconPack))
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType(ThemeFile.MIME)
            .putExtra(android.content.Intent.EXTRA_STREAM, uri)
            .putExtra(android.content.Intent.EXTRA_SUBJECT, "$name (Driftlist theme)")
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        SystemActions.start(context, android.content.Intent.createChooser(send, "Share $name"))
    }

    /** Reads a theme file into My themes. */
    suspend fun importTheme(context: Context, uri: Uri): Result<ThemeFile.Parsed> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { input ->
                val bytes = input.readNBytesCompat(ThemeFile.MAX_BYTES + 1)
                bytes.toString(Charsets.UTF_8)
            }
            ThemeFile.read(text).getOrThrow().also { parsed ->
                val now = System.currentTimeMillis()
                c.db.themeDao().upsert(ThemeEntity(java.util.UUID.randomUUID().toString(), parsed.name, parsed.look.toJson(), parsed.iconPack, now, now))
            }
        }
    }

    /** Apps the current icon pack has no icon for (and that the user hasn't picked one for). */
    suspend fun uncoveredApps(pack: String): List<AppEntity> = withContext(Dispatchers.IO) {
        val overrides = c.db.iconOverrideDao().all().filter { it.iconPack == pack }.mapTo(HashSet()) { it.appId }
        state.value.apps.filter { app ->
            !app.hidden && app.id !in overrides &&
                c.iconPacks.drawableFor(pack, android.content.ComponentName(app.packageName, app.activityClass).flattenToString()) == null
        }
    }

    fun setIcon(app: AppEntity, pack: String, drawableName: String?) = viewModelScope.launch {
        if (drawableName == null) c.db.iconOverrideDao().delete(app.id, pack) else c.db.iconOverrideDao().set(app.id, pack, drawableName, System.currentTimeMillis())
    }

    fun renameTheme(theme: ThemeEntity, name: String) = viewModelScope.launch { c.db.themeDao().rename(theme.id, name, System.currentTimeMillis()) }
    fun deleteTheme(theme: ThemeEntity) = viewModelScope.launch { c.db.themeDao().delete(theme.id) }

    fun hasQuickLock() = QuickLockService.isEnabled(getApplication())
    fun hasCalendarAccess() = c.calendar.hasPermission().also { c.calendar.refreshAccess() }
    fun calendars() = c.calendar.calendars()
    fun setCalendarHidden(id: Long, hidden: Boolean) = viewModelScope.launch { c.settings.setCalendarHidden(id, hidden) }
    fun hasUsageAccess() = c.usageBreaker.hasUsageAccess()
    fun canPostNotifications() = c.usageBreaker.canPostNotifications()
    fun hasNotificationAccess() = c.notifications.hasAccess()
    fun isDefaultLauncher() = SystemActions.isDefaultLauncher(getApplication())

    suspend fun writeBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.openOutputStream(uri)!!.use { c.backup.write(it) } }
    }

    suspend fun restoreBackup(context: Context, uri: Uri): Result<Backup.Result> = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.openInputStream(uri)!!.use { c.backup.restore(it) } }
    }
}

internal enum class Page(val title: String, val parent: Page? = null) {
    Root("Settings"),
    Home("Home screen", Root),
    Gestures("Gestures and search", Root),
    Look("Look", Root),
    Wellbeing("Wellbeing", Root),
    Backup("Backup", Root),
    About("About", Root),
    HiddenApps("Hidden apps", Home),
    Calendars("Calendars", Home),
    SearchEngines("Search engine", Gestures),
    IconPack("Icon pack", Look),
    Studio("Theme Studio", Look),
    MyThemes("My themes", Look),
    Wallpaper("Wallpaper", Look),
    FontLicences("Font licences", About),
    TimeChecks("Time checks", Wellbeing),
    Summary("Notification Summary", Wellbeing),
}

/** S17 settings and its sub-pages (S22 behaviour, S24 hidden apps, S25 backup). */
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val dark = when (state.settings.look.mode) {
                ThemeMode.Auto -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Status and navigation bar icons follow the theme's light/dark, not the system's.
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            DriftlistTheme(state.settings.look, state.wallpaperSeed) { SettingsScreen(vm, onExit = ::finish) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: SettingsViewModel, onExit: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.Root) }
    val snackbar = remember { SnackbarHostState() }
    val back = { page = page.parent ?: Page.Root }
    BackHandler(enabled = page != Page.Root, onBack = back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page.title) },
                navigationIcon = {
                    TextButton(onClick = { if (page == Page.Root) onExit() else back() }) {
                        Text(if (page == Page.Root) "Close" else "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
    ) { padding ->
        when (page) {
            Page.Root -> RootPage(vm, state, padding, onNavigate = { page = it })
            Page.Home -> HomePage(vm, state, padding, onNavigate = { page = it })
            Page.Gestures -> GesturesPage(vm, state, padding, onNavigate = { page = it })
            Page.SearchEngines -> SearchEnginePage(vm, state, padding)
            Page.Look -> LookHubPage(vm, state, padding, onNavigate = { page = it })
            Page.Studio -> ThemeStudioPage(vm, state, padding, snackbar)
            Page.MyThemes -> MyThemesPage(vm, state, padding, snackbar)
            Page.Wallpaper -> WallpaperPage(vm, state, padding, snackbar)
            Page.Wellbeing -> WellbeingPage(state, padding, onNavigate = { page = it })
            Page.Backup -> BackupPage(vm, padding, snackbar)
            Page.About -> AboutPage(padding, onNavigate = { page = it })
            Page.FontLicences -> FontLicencesPage(padding)
            Page.HiddenApps -> HiddenAppsPage(vm, state, padding)
            Page.Calendars -> CalendarsPage(vm, state, padding)
            Page.IconPack -> IconPackPage(vm, state, padding)
            Page.TimeChecks -> TimeChecksPage(vm, state, padding)
            Page.Summary -> SummaryPage(vm, state, padding)
        }
    }
}

@Composable
private fun RootPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues, onNavigate: (Page) -> Unit) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(vm.isDefaultLauncher()) }
    var hasNotif by remember { mutableStateOf(vm.hasNotificationAccess()) }
    LifecycleResumeEffect(Unit) {
        isDefault = vm.isDefaultLauncher()
        hasNotif = vm.hasNotificationAccess()
        onPauseOrDispose { }
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = vm.isDefaultLauncher()
    }
    val packLabel = remember(state.settings.iconPack) {
        state.settings.iconPack?.let { pkg -> vm.iconPacks().firstOrNull { it.packageName == pkg }?.label ?: pkg }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        if (!isDefault || !hasNotif) item { SectionHeader("Setup") }
        if (!isDefault) {
            item {
                NavRow("Home app", "Not set. Tap to choose Driftlist", warning = true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roles = context.getSystemService(RoleManager::class.java)
                        roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    } else {
                        SystemActions.start(context, SystemActions.homeSettingsIntent())
                    }
                }
            }
        }
        if (!hasNotif) {
            item {
                NavRow("Notification access", "Needed for previews, replies, music and summaries", warning = true) {
                    SystemActions.start(context, SystemActions.notificationAccessIntent(context))
                }
            }
        }
        item { Spacer(Modifier.height(SpaceTokens.s3)) }
        item { CategoryRow("Home screen", "Favorites, calendar, suggestions, music, the round button", Icons.Outlined.Home) { onNavigate(Page.Home) } }
        item { CategoryRow("Gestures and search", "Swipes, Home button, Quick Lock, left-handed", Icons.Outlined.Gesture) { onNavigate(Page.Gestures) } }
        item { CategoryRow("Look", "Themes, Theme Studio, icon pack", Icons.Outlined.Palette) { onNavigate(Page.Look) } }
        item { CategoryRow("Wellbeing", "Time checks, Notification Summary", Icons.Outlined.SelfImprovement) { onNavigate(Page.Wellbeing) } }
        item { CategoryRow("Backup", "Save or restore your setup", Icons.Outlined.Backup) { onNavigate(Page.Backup) } }
        item { CategoryRow("About", "Driftlist ${BuildConfig.VERSION_NAME}", Icons.Outlined.Info) { onNavigate(Page.About) } }
    }
}

@Composable
private fun HomePage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues, onNavigate: (Page) -> Unit) {
    val context = LocalContext.current
    val s = state.settings
    val hiddenCount = state.apps.count { it.hidden }
    var calendarAccess by remember { mutableStateOf(vm.hasCalendarAccess()) }
    var explainCalendar by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        calendarAccess = vm.hasCalendarAccess()
        onPauseOrDispose { }
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarAccess = vm.hasCalendarAccess()
        if (granted) {
            vm.setFlag("calendar_events", true)
        } else {
            val activity = context as? android.app.Activity
            if (activity != null && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALENDAR)) {
                explainCalendar = true
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            ToggleRow(
                "Calendar",
                "Your next event under the date. Tap the date for the week ahead",
                s.calendarEvents && calendarAccess,
            ) { on ->
                when {
                    !on -> vm.setFlag("calendar_events", false)
                    calendarAccess -> vm.setFlag("calendar_events", true)
                    else -> calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                }
            }
        }
        if (s.calendarEvents && calendarAccess) {
            item {
                val hidden = s.hiddenCalendars.size
                NavRow("Calendars to show", if (hidden == 0) "All" else "$hidden hidden") { onNavigate(Page.Calendars) }
            }
        }
        item {
            ToggleRow(
                "Favorites at the bottom",
                "Off: favorites follow the clock. On: they sit at the bottom, closest to your thumb",
                s.favoritesAtBottom,
            ) { vm.setFlag("favorites_at_bottom", it) }
        }
        item {
            ToggleRow("Suggest apps", "Your most-opened apps that aren't favorites, listed under them", s.suggestApps) {
                vm.setFlag("suggest_apps", it)
            }
        }
        item {
            ToggleRow("Music card", "Play, pause and skip from home while something is playing", s.mediaCard) {
                vm.setFlag("media_card", it)
            }
        }
        item {
            ToggleRow("Round button", "A shortcut to one app or folder at the bottom corner. Long-press it to change what it opens", s.buttonEnabled) {
                vm.setFlag("button_enabled", it)
                if (!it) vm.setButtonTarget(null)
            }
        }
        item { NavRow("Hidden apps", if (hiddenCount == 0) "None" else "$hiddenCount hidden") { onNavigate(Page.HiddenApps) } }
        item {
            ToggleRow("Most used first", "In the A-Z list, the apps you open most come first within each letter", s.listSortUsage) {
                vm.setFlag("list_sort_usage", it)
            }
        }
        item {
            ToggleRow("Folders in the app list", "Your favorites folders also appear at the end of the A-Z list", s.foldersInList) {
                vm.setFlag("folders_in_list", it)
            }
        }
        item {
            ToggleRow("Quick replies", "Reply to messages from the pop-up without opening the app", s.quickReplies) {
                vm.setFlag("quick_replies", it)
            }
        }
        item {
            ToggleRow("Music when headphones connect", "Your music apps appear under the clock while headphones are plugged in", s.plugAndPlay) {
                vm.setFlag("plug_and_play", it)
            }
        }
        item {
            ToggleRow("Flashlight reminder", "A row on home while the flashlight is on, with a button to turn it off", s.torchReminder) {
                vm.setFlag("torch_reminder", it)
            }
        }
        item {
            ToggleRow("Widget instead of the clock", "Needs a widget under the clock first (long-press the clock)", s.replaceClock) {
                vm.setFlag("replace_clock", it)
            }
        }
        item { SectionHeader("Display") }
        item {
            ToggleRow("Rotate with the phone", "Off: home stays upright. Tablets and foldables always rotate", s.allowRotation) {
                vm.setFlag("allow_rotation", it)
            }
        }
        item {
            ToggleRow("Hide the status bar", "Swipe down from the top edge to see it for a moment", s.hideStatusBar) {
                vm.setFlag("hide_status_bar", it)
            }
        }
        item { SectionHeader("Clock position") }
        item {
            Column(Modifier.padding(horizontal = SpaceTokens.s7)) {
                Slider(
                    value = s.headerPosition,
                    onValueChange = { vm.setHeaderPosition(it) },
                    valueRange = SizeTokens.headerPositionMin..SizeTokens.headerPositionMax,
                    modifier = Modifier.semantics { contentDescription = "Clock position, top to lower down" },
                )
                Row {
                    Text("Higher", style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f))
                    Text("Lower", style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }
    }
    if (explainCalendar) {
        PermissionDialog(
            explainer = Explainers.calendar,
            onConfirm = {
                explainCalendar = false
                SystemActions.start(
                    context,
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            onDismiss = { explainCalendar = false },
        )
    }
}

@Composable
private fun SearchEnginePage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        items(SearchEngine.entries, key = { it.name }) { engine ->
            ChoiceRow(engine.label, state.settings.searchEngine == engine) { vm.setSearchEngine(engine) }
        }
        item {
            Text(
                "Used for “Search the web” in search. Live suggestions are a separate switch and are off unless you turn them on.",
                style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(SpaceTokens.s7),
            )
        }
    }
}

@Composable
private fun CalendarsPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues) {
    val calendars = remember { vm.calendars() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        if (calendars.isEmpty()) {
            item {
                Text(
                    "No calendars on this phone yet. Add an account in your calendar app and it will show up here.",
                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(SpaceTokens.s7),
                )
            }
        }
        items(calendars, key = { it.id }) { cal ->
            ToggleRow(cal.name.ifBlank { "Unnamed calendar" }, cal.account.takeIf { it.isNotBlank() && it != cal.name }, cal.id !in state.settings.hiddenCalendars) { show ->
                vm.setCalendarHidden(cal.id, !show)
            }
        }
    }
}

@Composable
private fun GesturesPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues, onNavigate: (Page) -> Unit) {
    val context = LocalContext.current
    val s = state.settings
    var hasLock by remember { mutableStateOf(vm.hasQuickLock()) }
    var explain by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        hasLock = vm.hasQuickLock()
        onPauseOrDispose { }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setFlag("search_contacts", granted)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            ToggleRow(
                "Swipe up or press Home to search",
                "A long swipe up on home, or Home while you're already there, opens search",
                s.swipeUpSearch,
            ) { vm.setFlag("swipe_up_search", it) }
        }
        item {
            ToggleRow("Find hidden apps in search", "Hidden apps stay out of the list but can still be searched", s.searchIncludesHidden) {
                vm.setFlag("search_includes_hidden", it)
            }
        }
        item { NavRow("Search engine", s.searchEngine.label) { onNavigate(Page.SearchEngines) } }
        item {
            ToggleRow(
                "Web suggestions while you type",
                if (s.searchEngine.hasSuggestions) {
                    "What you type in search is sent to ${s.searchEngine.label} for suggestions. Off: nothing leaves your phone"
                } else {
                    "Pick a search engine first"
                },
                s.webSuggestions && s.searchEngine.hasSuggestions,
            ) { if (s.searchEngine.hasSuggestions) vm.setFlag("web_suggestions", it) }
        }
        item {
            ToggleRow("Search contacts", "Find people in search, then call or message them", s.searchContacts && vm.hasContactsAccess()) { on ->
                when {
                    !on -> vm.setFlag("search_contacts", false)
                    vm.hasContactsAccess() -> vm.setFlag("search_contacts", true)
                    else -> contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                }
            }
        }
        item {
            ToggleRow("Quick Lock", "Double-tap the alphabet to turn the screen off", s.quickLock) { on ->
                vm.setFlag("quick_lock", on)
                if (on && !vm.hasQuickLock()) explain = true
            }
        }
        if (s.quickLock && !hasLock) {
            item { NavRow("Quick Lock needs one more step", "Switch on Driftlist Quick Lock in accessibility settings", warning = true) { explain = true } }
        }
        item {
            ToggleRow("Left-handed", "Puts the alphabet on the left edge", s.leftHanded) { vm.setFlag("left_handed", it) }
        }
        item {
            ToggleRow("Vibrate on letters", "A light tick as you slide past each letter", s.haptics) { vm.setFlag("haptics", it) }
        }
    }
    if (explain) {
        PermissionDialog(
            explainer = Explainers.quickLock(SystemActions.isSideloaded(context)),
            onConfirm = {
                explain = false
                SystemActions.start(context, android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { explain = false },
        )
    }
}

@Composable
private fun WellbeingPage(state: SettingsUiState, padding: PaddingValues, onNavigate: (Page) -> Unit) {
    val s = state.settings
    fun count(n: Int) = "$n ${if (n == 1) "app" else "apps"}"
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            NavRow("Time checks", if (s.usageBreaker) "On for ${count(state.usageRules.size)}" else "Off") { onNavigate(Page.TimeChecks) }
        }
        item {
            NavRow("Notification Summary", if (s.notificationSummary) "Holding ${count(state.summaryRules.size)}" else "Off") { onNavigate(Page.Summary) }
        }
    }
}

@Composable
private fun SummaryPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues) {
    val context = LocalContext.current
    var hasNotif by remember { mutableStateOf(vm.hasNotificationAccess()) }
    LifecycleResumeEffect(Unit) {
        hasNotif = vm.hasNotificationAccess()
        onPauseOrDispose { }
    }
    val selected = remember(state.summaryRules, state.apps) {
        state.apps.filter { it.packageName in state.summaryRules }.mapTo(HashSet()) { it.id }
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text(
            "Notifications from the apps you pick skip the shade and wait. You get them together in one notification, at most every few hours, on the hour.",
            style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
        )
        ToggleRow("Notification Summary", null, state.settings.notificationSummary) { vm.setFlag("notification_summary", it) }
        if (state.settings.notificationSummary && !hasNotif) {
            NavRow("Notification access needed", "Driftlist can only hold notifications it can see", warning = true) {
                SystemActions.start(context, SystemActions.notificationAccessIntent(context))
            }
        }
        if (state.settings.notificationSummary) {
            AppMultiPicker(state.apps.filterNot { it.hidden }, vm.icons, selected) { vm.toggleSummaryRule(it) }
        }
    }
}

@Composable
private fun BackupPage(vm: SettingsViewModel, padding: PaddingValues, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Backup.MIME)) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val r = vm.writeBackup(context, uri)
            snackbar.showSnackbar(if (r.isSuccess) "Backup saved" else "Couldn't save the backup")
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            NavRow("Save a backup", "Favorites, folders, names, hidden apps, icons and settings") {
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
                createBackup.launch("driftlist-$stamp.json")
            }
        }
        item { NavRow("Restore a backup", "Replaces your current setup") { openBackup.launch(arrayOf("application/json", "*/*")) } }
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Restore this backup?") },
            text = { Text("Your current favorites, folders, app names and settings will be replaced. Widgets need to be added again afterwards.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    scope.launch {
                        val r = vm.restoreBackup(context, uri)
                        snackbar.showSnackbar(
                            r.fold(
                                onSuccess = { res ->
                                    if (res.skippedMissingApps > 0) {
                                        "Restored. ${res.skippedMissingApps} favorites skipped: those apps aren't installed."
                                    } else {
                                        "Restored"
                                    }
                                },
                                onFailure = { e -> e.message ?: "That file couldn't be restored" },
                            ),
                        )
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AboutPage(padding: PaddingValues, onNavigate: (Page) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { NavRow("Driftlist ${BuildConfig.VERSION_NAME}", "A calm, list-first launcher.") {} }
        item { NavRow("Privacy", "No accounts, no ads. Nothing leaves this phone unless you turn on web suggestions in search.") {} }
        item { NavRow("Font licences", "${Fonts.bundled.size} fonts under the SIL Open Font License") { onNavigate(Page.FontLicences) } }
    }
}

@Composable
private fun HiddenAppsPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues) {
    val hidden = state.apps.filter { it.hidden }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        if (hidden.isEmpty()) {
            item {
                Text(
                    "No hidden apps. Long-press any app and choose Hide from app list.",
                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(SpaceTokens.s7),
                )
            }
        }
        items(hidden, key = { it.id }) { app ->
            SheetAppRow(app, vm.icons, onClick = { vm.unhide(app) }) {
                TextButton(onClick = { vm.unhide(app) }) { Text("Show") }
            }
        }
    }
}

@Composable
private fun IconPackPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues) {
    val context = LocalContext.current
    val packs = remember { vm.iconPacks() }
    var picking by remember { mutableStateOf<AppEntity?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val uncovered by produceState(emptyList<AppEntity>(), state.settings.iconPack, state.apps, refresh) {
        value = state.settings.iconPack?.let { vm.uncoveredApps(it) }.orEmpty()
    }
    picking?.let { app ->
        val pack = state.settings.iconPack
        IconPickerSheet(
            app = app,
            icons = vm.icons,
            pack = pack,
            packLabel = packs.firstOrNull { it.packageName == pack }?.label,
            onPick = { name -> pack?.let { vm.setIcon(app, it, name) }; picking = null; refresh++ },
            onOpenIconSettings = { picking = null },
            onClose = { picking = null },
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { ChoiceRow("System icons", state.settings.iconPack == null) { vm.setIconPack(null) } }
        items(packs, key = { it.packageName }) { pack ->
            ChoiceRow(pack.label, state.settings.iconPack == pack.packageName) { vm.setIconPack(pack.packageName) }
        }
        val pack = state.settings.iconPack
        if (pack != null) {
            item { SectionHeader(if (uncovered.isEmpty()) "Every app has an icon from this pack" else "Not in this pack (${uncovered.size})") }
            if (uncovered.isNotEmpty()) {
                item {
                    Text(
                        "These keep their own icon. Tap one to pick a close match from the pack.",
                        style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = SpaceTokens.s7),
                    )
                }
            }
            items(uncovered, key = { "u:" + it.id }) { app -> SheetAppRow(app, vm.icons, onClick = { picking = app }) }
        }
        item {
            Column(Modifier.padding(SpaceTokens.s7)) {
                Text(
                    if (packs.isEmpty()) "No icon packs installed yet." else "Long-press any app on home and choose Change icon to pick one yourself.",
                    style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                TextButton(onClick = {
                    SystemActions.start(context, android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps")))
                }) { Text("Find icon packs on Play") }
            }
        }
    }
}

@Composable
private fun TimeChecksPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues) {
    val context = LocalContext.current
    var hasUsage by remember { mutableStateOf(vm.hasUsageAccess()) }
    var canNotify by remember { mutableStateOf(vm.canPostNotifications()) }
    LifecycleResumeEffect(Unit) {
        hasUsage = vm.hasUsageAccess()
        canNotify = vm.canPostNotifications()
        onPauseOrDispose { }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }
    val selected = remember(state.usageRules, state.apps) {
        state.apps.filter { it.packageName in state.usageRules }.mapTo(HashSet()) { it.id }
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Text(
            "Choose apps you tend to lose time in. After 5, 10, 20, 30 minutes and so on of continuous use you get a quiet notice. Nothing is ever blocked.",
            style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
        )
        ToggleRow("Time checks", null, state.settings.usageBreaker) { on ->
            vm.setUsageBreaker(on)
            if (on && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        var explain by remember { mutableStateOf(false) }
        if (state.settings.usageBreaker && !hasUsage) {
            NavRow("Usage access needed", "Lets Driftlist see which app is open. Tap to set it up.", warning = true) { explain = true }
        }
        if (explain) {
            PermissionDialog(
                explainer = Explainers.usageAccess(SystemActions.isSideloaded(context)),
                onConfirm = { explain = false; SystemActions.start(context, SystemActions.usageAccessIntent()) },
                onDismiss = { explain = false },
            )
        }
        if (state.settings.usageBreaker && !canNotify) {
            NavRow("Notifications are off", "Time checks are delivered as notifications", warning = true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (state.settings.usageBreaker) {
            AppMultiPicker(state.apps.filterNot { it.hidden }, vm.icons, selected) { vm.toggleUsageRule(it) }
        }
    }
}

// --- rows -------------------------------------------------------------------------------------

@Composable
private fun CategoryRow(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.settingsRow)
            .clickable(onClick = onClick)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s6),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface))
            Text(value, style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text,
        style = TypeTokens.label.style(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .padding(start = SpaceTokens.s7, end = SpaceTokens.s7, top = SpaceTokens.s7, bottom = SpaceTokens.s2)
            .semantics { heading() },
    )
}

@Composable
internal fun NavRow(title: String, value: String?, warning: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.settingsRow)
            .clickable(onClick = onClick)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface))
        value?.let {
            Text(
                it,
                style = TypeTokens.caption.style(if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
internal fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.settingsRow)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface))
            subtitle?.let { Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant)) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.settingsRow)
            .toggleable(value = selected, role = Role.RadioButton, onValueChange = { onClick() })
            .padding(horizontal = SpaceTokens.s7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(start = SpaceTokens.s4))
    }
}

private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < max) {
        val n = read(buf, 0, minOf(buf.size, max - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
