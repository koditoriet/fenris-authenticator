package se.koditoriet.fenris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun FenrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val accentColors = if (darkTheme) {
        AccentColors(
            on = Color(0xFFFDCA40),
            onForeground = Color(0xFFFDCA40),
            onBackground = Color(0x1FFDCA40),
        )
    } else {
        AccentColors(
            on = Color(0xFFFFC107),
            onForeground = colorScheme.surface,
            onBackground = Color(0xC0FFC107),
        )
    }

    CompositionLocalProvider(LocalAccentColors provides accentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val LocalAccentColors = staticCompositionLocalOf {
    AccentColors(
        on = Color.Unspecified,
        onBackground = Color.Unspecified,
        onForeground = Color.Unspecified,
    )
}

class AccentColors(
    /**
     * Color for on/enabled items on a surface background.
     */
    val on: Color,

    /**
     * Background color for on/enabled items that change both bg and fg color when enabled/disabled.
     */
    val onBackground: Color,

    /**
     * Foreground color for on/enabled items that change both bg and fg color when enabled/disabled.
     */
    val onForeground: Color,
)
