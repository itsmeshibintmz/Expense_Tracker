package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val userName: String = "User",
    val currencySymbol: String = "$",
    val themeMode: String = "System", // System, Light, Dark
    val accentColor: String = "MintGreen" // MintGreen, SkyBlue, LavenderPurple, CoralRed, SunnyYellow
)

class UserPreferencesRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val userName = sharedPreferences.getString("user_name", "User") ?: "User"
        val currencySymbol = sharedPreferences.getString("currency_symbol", "$") ?: "$"
        val themeMode = sharedPreferences.getString("theme_mode", "System") ?: "System"
        val accentColor = sharedPreferences.getString("accent_color", "MintGreen") ?: "MintGreen"
        return UserPreferences(userName, currencySymbol, themeMode, accentColor)
    }

    fun updateUserName(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
        _preferences.value = loadPreferences()
    }

    fun updateCurrencySymbol(symbol: String) {
        sharedPreferences.edit().putString("currency_symbol", symbol).apply()
        _preferences.value = loadPreferences()
    }

    fun updateThemeMode(mode: String) {
        sharedPreferences.edit().putString("theme_mode", mode).apply()
        _preferences.value = loadPreferences()
    }

    fun updateAccentColor(color: String) {
        sharedPreferences.edit().putString("accent_color", color).apply()
        _preferences.value = loadPreferences()
    }
}
