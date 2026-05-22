package io.github.tjdam007.dropdroid

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class AppThemePreference(val label: String) {
  System("System"),
  Light("Light"),
  Dark("Dark"),
}

object AppThemeController {
  private const val PREFS_NAME = "dropdroid_theme"
  private const val KEY_THEME = "theme"

  private val mutableTheme = MutableStateFlow(AppThemePreference.System)
  val theme: StateFlow<AppThemePreference> = mutableTheme

  fun load(context: Context) {
    val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, AppThemePreference.System.name)
    mutableTheme.update { runCatching { AppThemePreference.valueOf(stored ?: AppThemePreference.System.name) }.getOrDefault(AppThemePreference.System) }
  }

  fun set(context: Context, preference: AppThemePreference) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_THEME, preference.name)
      .apply()
    mutableTheme.update { preference }
  }
}
