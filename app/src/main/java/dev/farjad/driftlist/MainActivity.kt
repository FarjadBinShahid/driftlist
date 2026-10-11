package dev.farjad.driftlist

import android.view.WindowManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.farjad.driftlist.ui.home.HomeScreen
import dev.farjad.driftlist.ui.home.HomeViewModel
import dev.farjad.driftlist.ui.settings.SettingsActivity
import dev.farjad.driftlist.ui.theme.DriftlistTheme
import dev.farjad.driftlist.ui.theme.HomeColorsForDarkWallpaper
import dev.farjad.driftlist.ui.theme.HomeColorsForLightWallpaper
import dev.farjad.driftlist.ui.theme.LocalHomeColors

/** The HOME activity. Everything the launcher shows lives in here. */
class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private var darkText by mutableStateOf(false)
    private val widgetHost get() = (application as DriftlistApp).container.widgetHost

    private var pendingConfigure: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DriftlistApp).container
        darkText = container.wallpaper.state.value.darkText
        applySystemBars()
        setContent {
            val wallpaper by container.wallpaper.state.collectAsStateWithLifecycle()
            val look by remember { vm.state.map { it.settings.look }.distinctUntilChanged() }
                .collectAsStateWithLifecycle(initialValue = vm.state.value.settings.look)
            val display by remember { vm.state.map { it.settings.allowRotation to it.settings.hideStatusBar }.distinctUntilChanged() }
                .collectAsStateWithLifecycle(initialValue = false to false)
            LaunchedEffect(display) { applyDisplay(display.first, display.second) }
            val blur by remember { vm.state.map { it.settings.wallpaperBlur }.distinctUntilChanged() }
                .collectAsStateWithLifecycle(initialValue = 0)
            LaunchedEffect(blur) { applyBlur(blur) }
            LaunchedEffect(wallpaper.darkText) {
                darkText = wallpaper.darkText
                applySystemBars()
            }
            CompositionLocalProvider(
                LocalHomeColors provides if (wallpaper.darkText) HomeColorsForLightWallpaper else HomeColorsForDarkWallpaper,
            ) {
                DriftlistTheme(look, wallpaper.seed) {
                    HomeScreen(
                        vm = vm,
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onConfigureWidget = ::configureWidget,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshSetup()
        (application as DriftlistApp).container.wallpaper.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        // Same test the AOSP launcher uses: focused and not just brought to the front means
        // the user pressed Home while already looking at home.
        val broughtToFront = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0
        vm.onHomePressed(alreadyHome = hasWindowFocus() && !broughtToFront)
    }

    /**
     * Widget configure screens can only be started by the host itself, through
     * AppWidgetHost.startAppWidgetConfigureActivityForResult, which reports back to onActivityResult.
     */
    private fun configureWidget(appWidgetId: Int, done: (Boolean) -> Unit) {
        pendingConfigure = done
        try {
            widgetHost.host.startAppWidgetConfigureActivityForResult(this, appWidgetId, 0, REQUEST_CONFIGURE, null)
        } catch (e: RuntimeException) {
            pendingConfigure = null
            done(false)
        }
    }

    @Deprecated("Needed for AppWidgetHost configure results")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE) {
            pendingConfigure?.invoke(resultCode == Activity.RESULT_OK)
            pendingConfigure = null
        }
    }

    /**
     * Portrait unless the user allows rotation (tablets and foldables always rotate). The status bar
     * can be hidden on home; a swipe from the top shows it for a moment.
     */
    private fun applyDisplay(allowRotation: Boolean, hideStatusBar: Boolean) {
        val large = resources.configuration.smallestScreenWidthDp >= 600
        requestedOrientation = if (allowRotation || large) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hideStatusBar) hide(WindowInsetsCompat.Type.statusBars()) else show(WindowInsetsCompat.Type.statusBars())
        }
    }

    /** Blurs the wallpaper behind home with the system's window blur (Android 12+, where supported). */
    private fun applyBlur(level: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!windowManager.isCrossWindowBlurEnabled) return
        val dp = when (level) {
            1 -> BLUR_LIGHT_DP
            2 -> BLUR_STRONG_DP
            else -> 0
        }
        val px = (dp * resources.displayMetrics.density).toInt()
        // The compositor only blurs behind a window that has some background and asks for it.
        window.setBackgroundDrawable(ColorDrawable(if (px > 0) BLUR_TINT else Color.TRANSPARENT))
        window.attributes = window.attributes.apply {
            if (px > 0) flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND else flags = flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            blurBehindRadius = px
        }
        window.setBackgroundBlurRadius(px)
    }

    private fun applySystemBars() {
        val style = if (darkText) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private companion object {
        const val REQUEST_CONFIGURE = 0x41D
        const val BLUR_LIGHT_DP = 12
        const val BLUR_STRONG_DP = 32
        /** Nearly invisible: enough for the compositor to treat the window as having a background. */
        const val BLUR_TINT = 0x01000000
    }
}
