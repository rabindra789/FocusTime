package com.rabindra.focustime.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rabindra.focustime.FocusTimeViewModel
import com.rabindra.focustime.ui.about.AboutScreen
import com.rabindra.focustime.ui.dashboard.DashboardScreen
import com.rabindra.focustime.ui.landscape.LandscapeClockScreen
import com.rabindra.focustime.ui.settings.SettingsScreen
import com.rabindra.focustime.ui.statistics.StatisticsScreen
import com.rabindra.focustime.ui.theme.LowPowerPalette
import com.rabindra.focustime.ui.theme.LocalFocusPalette

private enum class PortraitScreen { Timer, Statistics, Settings, About }

/**
 * Keeps the status bar out of the way, and takes the navigation bar too when the
 * landscape clock is showing. Bars can still be swiped in temporarily.
 */
@Composable
private fun ImmersiveSystemBars(fullScreen: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    val window = activity.window

    androidx.compose.runtime.DisposableEffect(fullScreen) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (fullScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FocusTimeApp(viewModel: FocusTimeViewModel) {
    val state by viewModel.state.collectAsState()
    val remaining by viewModel.remainingMillis.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val dndAccess by viewModel.dndAccess.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // No status bar anywhere; a true full-screen desk clock in landscape.
    ImmersiveSystemBars(fullScreen = isLandscape)

    // Low power mode drops every surface to true black. Only OLED panels switch those pixels
    // off, so this is a power saving on OLED and a purely visual choice on LCD.
    val palette = if (settings.lowPowerMode) LowPowerPalette else LocalFocusPalette.current
    val context = LocalContext.current

    CompositionLocalProvider(LocalFocusPalette provides palette) {
        if (isLandscape) {
            LandscapeClockScreen(
                state = state,
                remainingMillis = remaining,
                use24Hour = settings.use24Hour,
                onStartPause = viewModel::toggleTimer,
                onSkip = viewModel::skipSession,
                onReset = viewModel::resetTimer,
                watchFace = settings.watchFace,
                onWatchFaceChange = { face ->
                    viewModel.updateSettings { it.copy(watchFace = face) }
                },
            )
            return@CompositionLocalProvider
        }

        var screen by remember { mutableStateOf(PortraitScreen.Timer) }

        Box(Modifier.fillMaxSize().background(palette.background)) {
            when (screen) {
                PortraitScreen.Timer -> DashboardScreen(
                    state = state,
                    remainingMillis = remaining,
                    onStartPause = viewModel::toggleTimer,
                    onReset = viewModel::resetTimer,
                    onSkip = viewModel::skipSession,
                    onSettings = { screen = PortraitScreen.Settings },
                    onStatistics = { screen = PortraitScreen.Statistics },
                    keepScreenOn = settings.keepScreenOn,
                    showTutorial = !settings.tutorialCompleted,
                    onTutorialDone = {
                        viewModel.updateSettings { it.copy(tutorialCompleted = true) }
                    },
                )

                PortraitScreen.Statistics -> StatisticsScreen(
                    statistics = viewModel.statistics.collectAsState().value,
                    onBack = { screen = PortraitScreen.Timer },
                )

                PortraitScreen.Settings -> SettingsScreen(
                    settings = settings,
                    dndAccess = dndAccess,
                    onUpdate = { transform -> viewModel.updateSettings(transform) },
                    onResetStatistics = viewModel::resetStatistics,
                    onRefreshDndAccess = viewModel::refreshDndAccess,
                    onOpenDndSettings = {
                        runCatching { context.startActivity(viewModel.dndSettingsIntent()) }
                        viewModel.refreshDndAccess()
                    },
                    onBack = { screen = PortraitScreen.Timer },
                    onAbout = { screen = PortraitScreen.About },
                )

                PortraitScreen.About -> AboutScreen(
                    onBack = { screen = PortraitScreen.Settings },
                )
            }
        }
    }
}
