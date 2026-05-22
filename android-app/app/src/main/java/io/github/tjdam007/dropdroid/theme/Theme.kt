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
    secondary = Coral80,
    tertiary = Blue80,
    background = DarkInk,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceSoft,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Teal40,
    secondary = Clay40,
    tertiary = Blue40,
    background = Paper,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = SurfaceSoft,
    onBackground = Ink,
    onSurface = Ink,
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
