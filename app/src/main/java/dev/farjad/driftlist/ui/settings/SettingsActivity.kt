package dev.farjad.driftlist.ui.settings

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
import androidx.compose.material3.AlertDialog
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
import dev.farjad.driftlist.data.db.UsageRuleEntity
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
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val c = (application as DriftlistApp).container
    val icons = c.icons

    val state: StateFlow<SettingsUiState> = combine(
        c.settings.flow,
        c.db.appDao().observeAll(),
        c.db.usageDao().observeRules(),
    ) { s, apps, rules ->
        SettingsUiState(s, apps.sortedBy { it.label.lowercase() }, rules.mapTo(HashSet()) { it.packageName })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun iconPacks(): List<IconPack> = c.iconPacks.installed()
    fun setIconPack(pkg: String?) = viewModelScope.launch { c.settings.setIconPack(pkg) }
    fun setSwipeUpSearch(v: Boolean) = viewModelScope.launch { c.settings.setSwipeUpSearch(v) }
    fun setSearchIncludesHidden(v: Boolean) = viewModelScope.launch { c.settings.setSearchIncludesHidden(v) }
    fun setUsageBreaker(v: Boolean) = viewModelScope.launch { c.settings.setUsageBreaker(v) }
    fun setFavoritesAtBottom(v: Boolean) = viewModelScope.launch { c.settings.setFavoritesAtBottom(v) }
    fun unhide(app: AppEntity) = viewModelScope.launch { c.db.appDao().setHidden(app.id, false, System.currentTimeMillis()) }

    fun toggleUsageRule(app: AppEntity) = viewModelScope.launch {
        val current = state.value.usageRules
        val next = if (app.packageName in current) current - app.packageName else current + app.packageName
        val now = System.currentTimeMillis()
        val serials = state.value.apps.associate { it.packageName to it.userSerial }
        c.db.usageDao().setRules(next.map { pkg -> UsageRuleEntity(pkg, serials[pkg] ?: app.userSerial, now) })
    }

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

private enum class Page(val title: String) {
    Root("Settings"),
    HiddenApps("Hidden apps"),
    IconPack("Icon pack"),
    TimeChecks("Time checks"),
}

/** S17 settings and its sub-pages (S22 behaviour, S24 hidden apps, S25 backup). */
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DriftlistTheme { SettingsScreen(vm, onExit = ::finish) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: SettingsViewModel, onExit: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.Root) }
    val snackbar = remember { SnackbarHostState() }
    BackHandler(enabled = page != Page.Root) { page = Page.Root }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page.title) },
                navigationIcon = {
                    TextButton(onClick = { if (page == Page.Root) onExit() else page = Page.Root }) {
                        Text(if (page == Page.Root) "Close" else "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
    ) { padding ->
        when (page) {
            Page.Root -> RootPage(vm, state, padding, snackbar, onNavigate = { page = it })
            Page.HiddenApps -> HiddenAppsPage(vm, state, padding)
            Page.IconPack -> IconPackPage(vm, state, padding)
            Page.TimeChecks -> TimeChecksPage(vm, state, padding)
        }
    }
}

@Composable
private fun RootPage(
    vm: SettingsViewModel,
    state: SettingsUiState,
    padding: PaddingValues,
    snackbar: SnackbarHostState,
    onNavigate: (Page) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDefault by remember { mutableStateOf(vm.isDefaultLauncher()) }
    var hasNotif by remember { mutableStateOf(vm.hasNotificationAccess()) }
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    LifecycleResumeEffect(Unit) {
        isDefault = vm.isDefaultLauncher()
        hasNotif = vm.hasNotificationAccess()
        onPauseOrDispose { }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = vm.isDefaultLauncher()
    }
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

    val hiddenCount = state.apps.count { it.hidden }
    val packLabel = remember(state.settings.iconPack) {
        state.settings.iconPack?.let { pkg -> vm.iconPacks().firstOrNull { it.packageName == pkg }?.label ?: pkg }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { SectionHeader("Setup") }
        item {
            NavRow(
                title = "Home app",
                value = if (isDefault) "Driftlist is your home screen" else "Not set. Tap to choose Driftlist",
                warning = !isDefault,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isDefault) {
                    val roles = context.getSystemService(RoleManager::class.java)
                    roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                } else {
                    SystemActions.start(context, SystemActions.homeSettingsIntent())
                }
            }
        }
        item {
            NavRow(
                title = "Notification access",
                value = if (hasNotif) "Allowed" else "Needed for previews and replies",
                warning = !hasNotif,
            ) { SystemActions.start(context, SystemActions.notificationAccessIntent(context)) }
        }

        item { SectionHeader("Home and search") }
        item { NavRow("Hidden apps", if (hiddenCount == 0) "None" else "$hiddenCount hidden") { onNavigate(Page.HiddenApps) } }
        item {
            ToggleRow(
                "Favorites at the bottom",
                "Off: favorites follow the clock. On: they sit at the bottom, closest to your thumb",
                state.settings.favoritesAtBottom,
                vm::setFavoritesAtBottom,
            )
        }
        item {
            ToggleRow(
                "Swipe up or press Home to search",
                "A long swipe up on home, or Home while you're already there, opens search",
                state.settings.swipeUpSearch,
                vm::setSwipeUpSearch,
            )
        }
        item {
            ToggleRow(
                "Find hidden apps in search",
                "Hidden apps stay out of the list but can still be searched",
                state.settings.searchIncludesHidden,
                vm::setSearchIncludesHidden,
            )
        }

        item { SectionHeader("Look") }
        item { NavRow("Icon pack", packLabel ?: "System icons") { onNavigate(Page.IconPack) } }

        item { SectionHeader("Wellbeing") }
        item {
            NavRow(
                "Time checks",
                if (state.settings.usageBreaker) "On for ${state.usageRules.size} ${if (state.usageRules.size == 1) "app" else "apps"}" else "Off",
            ) { onNavigate(Page.TimeChecks) }
        }

        item { SectionHeader("Backup") }
        item {
            NavRow("Save a backup", "Favorites, folders, names, hidden apps, icons and settings") {
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
                createBackup.launch("driftlist-$stamp.json")
            }
        }
        item { NavRow("Restore a backup", "Replaces your current setup") { openBackup.launch(arrayOf("application/json", "*/*")) } }

        item { SectionHeader("About") }
        item { NavRow("Driftlist ${BuildConfig.VERSION_NAME}", "A calm, list-first launcher. Everything stays on this phone.") {} }
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { ChoiceRow("System icons", state.settings.iconPack == null) { vm.setIconPack(null) } }
        items(packs, key = { it.packageName }) { pack ->
            ChoiceRow(pack.label, state.settings.iconPack == pack.packageName) { vm.setIconPack(pack.packageName) }
        }
        item {
            Column(Modifier.padding(SpaceTokens.s7)) {
                Text(
                    if (packs.isEmpty()) "No icon packs installed yet." else "Icons the pack doesn't cover keep their normal look. Long-press an app and choose Change icon to pick one yourself.",
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
private fun SectionHeader(text: String) {
    Text(
        text,
        style = TypeTokens.label.style(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .padding(start = SpaceTokens.s7, end = SpaceTokens.s7, top = SpaceTokens.s7, bottom = SpaceTokens.s2)
            .semantics { heading() },
    )
}

@Composable
private fun NavRow(title: String, value: String?, warning: Boolean = false, onClick: () -> Unit) {
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
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
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
private fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
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
