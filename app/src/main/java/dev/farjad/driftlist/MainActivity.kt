package dev.farjad.driftlist

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
import dev.farjad.driftlist.ui.theme.DriftlistTheme
import dev.farjad.driftlist.ui.theme.HomeColorsForDarkWallpaper
import dev.farjad.driftlist.ui.theme.HomeColorsForLightWallpaper
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.wallpaperWantsDarkText

/** The HOME activity. Everything the launcher shows lives in here. */
class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private var darkText by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkText = wallpaperWantsDarkText(this)
        applySystemBars()
        setContent {
            LaunchedEffect(darkText) { applySystemBars() }
            CompositionLocalProvider(
                LocalHomeColors provides if (darkText) HomeColorsForLightWallpaper else HomeColorsForDarkWallpaper,
            ) {
                DriftlistTheme { HomeScreen(vm) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshSetup()
        darkText = wallpaperWantsDarkText(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
    }

    private fun applySystemBars() {
        val style = if (darkText) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
