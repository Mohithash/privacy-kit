package com.xplex.privacy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SealTeal40,
    onPrimary = Neutral99,
    secondary = DeepIndigo40,
    onSecondary = Neutral99,
    tertiary = Amber40,
    background = Neutral99,
    surface = Neutral99,
    surfaceVariant = Neutral95,
    onBackground = Neutral10,
    onSurface = Neutral10,
    error = DangerRed
)

private val DarkColors = darkColorScheme(
    primary = SealTeal80,
    onPrimary = SealTeal20,
    secondary = DeepIndigo80,
    onSecondary = DeepIndigo20,
    tertiary = Amber80,
    background = Neutral10,
    surface = Neutral10,
    surfaceVariant = Neutral20,
    onBackground = Neutral90,
    onSurface = Neutral90,
    error = Color(0xFFFFB4AB)
)

/**
 * Dynamic color (Material You, wallpaper-derived) on Android 12+, falling
 * back to our own seal-teal/indigo palette everywhere else - so the app
 * feels native to whatever device it's on, without ever falling back to
 * Compose's default purple if dynamic color isn't available.
 */
@Composable
fun XplexTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
