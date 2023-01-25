package dev.flammky.compose_components.presentation.main

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.color.DynamicColors
import dev.flammky.compose_components.core.build.BuildVersion
import dev.flammky.compose_components.presentation.MainActivity
import dev.flammky.compose_components.presentation.theme.*

fun MainActivity.setContent() {
    setContent {
        MaterialDesign3Theme {
            Box(modifier = Modifier.fillMaxSize().background(Theme.backgroundColorAsState().value)) {
                RootNavigation()
            }
        }
    }
}

@Composable
private fun MaterialDesign3Theme(
    dynamic: Boolean = BuildVersion.hasSnowCone(),
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (dynamic && DynamicColors.isDynamicColorAvailable()) {
        if (useDarkTheme) {
            dynamicDarkColorScheme(LocalContext.current)
        } else {
            dynamicLightColorScheme(LocalContext.current)
        }
    } else {
        if (useDarkTheme) {
            Theme.defaultDarkColorScheme()
        } else {
            Theme.defaultLightColorScheme()
        }
    }
    Theme.ProvideMaterialTheme(
        isThemeDark = useDarkTheme,
        colorScheme = colors
    ) {
        with(rememberSystemUiController()) {
            setStatusBarColor(Theme.elevatedTonalPrimarySurfaceAsState(elevation = 2.dp).value.copy(0.4f))
            setNavigationBarColor(Color.Transparent)
            isNavigationBarContrastEnforced = true
            statusBarDarkContentEnabled = !useDarkTheme
            navigationBarDarkContentEnabled = !useDarkTheme
        }
        content()
    }
}