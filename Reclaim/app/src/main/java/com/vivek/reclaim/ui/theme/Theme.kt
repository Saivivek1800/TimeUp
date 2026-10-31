package com.vivek.reclaim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = Ink,
    tertiary = Amber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = OffWhite,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OutlineLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF003732),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealContainer,
    secondary = AmberDark,
    onSecondary = Color(0xFF3B2600),
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnDark,
    tertiary = AmberDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberContainer,
    background = BackgroundDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF9FB3BF),
    outline = Color(0xFF6B8290),
)

@Composable
fun ReclaimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ReclaimTypography,
        content = content,
    )
}
