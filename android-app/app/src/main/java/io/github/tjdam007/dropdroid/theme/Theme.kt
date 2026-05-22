package io.github.tjdam007.dropdroid.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import io.github.tjdam007.dropdroid.AppThemePreference

private val DarkColorScheme =
  darkColorScheme(
    primary = Mist80,
    onPrimary = DarkInk,
    secondary = Coral80,
    onSecondary = DarkInk,
    tertiary = Blue80,
    onTertiary = DarkInk,
    background = DarkInk,
    onBackground = Mist80,
    surface = DarkSurface,
    onSurface = Mist80,
    surfaceVariant = DarkSurfaceSoft,
    onSurfaceVariant = MistMuted,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Teal40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Clay40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Blue40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = Paper,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = SurfaceSoft,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = LineSoft,
  )

@Composable
fun MyApplicationTheme(
  themePreference: AppThemePreference = AppThemePreference.System,
  content: @Composable () -> Unit,
) {
  val darkTheme =
    when (themePreference) {
      AppThemePreference.System -> isSystemInDarkTheme()
      AppThemePreference.Light -> false
      AppThemePreference.Dark -> true
    }
  val colorScheme =
    when {
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
