package com.ewastetriage.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

class ThemeSettings(context: Context) {
    private val prefs = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    var currentMode by mutableStateOf(loadThemeMode())
        private set

    private fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(saved)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        currentMode = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
}

val LocalThemeSettings = staticCompositionLocalOf<ThemeSettings> {
    error("ThemeSettings not provided")
}
