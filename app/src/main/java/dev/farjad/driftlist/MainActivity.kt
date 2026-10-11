package dev.farjad.driftlist

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
import dev.farjad.driftlist.ui.theme.wallpaperWantsDarkText

/** The HOME activity. Everything the launcher shows lives in here. */
class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private var darkText by mutableStateOf(false)
    private val widgetHost get() = (application as DriftlistApp).container.widgetHost

    private var pendingConfigure: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkText = wallpaperWantsDarkText(this)
        applySystemBars()
        setContent {
            LaunchedEffect(darkText) { applySystemBars() }
            CompositionLocalProvider(
                LocalHomeColors provides if (darkText) HomeColorsForLightWallpaper else HomeColorsForDarkWallpaper,
            ) {
                DriftlistTheme {
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
        darkText = wallpaperWantsDarkText(this)
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
    }
}
