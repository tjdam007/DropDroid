package io.github.tjdam007.dropdroid.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
