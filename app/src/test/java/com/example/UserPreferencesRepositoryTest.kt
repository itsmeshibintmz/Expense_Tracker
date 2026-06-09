package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesRepositoryTest {

    @Test
    fun `test user preferences default values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        val prefs = repository.preferences.value
        assertEquals("User", prefs.userName)
        assertEquals("$", prefs.currencySymbol)
        assertEquals("System", prefs.themeMode)
        assertEquals("MintGreen", prefs.accentColor)
    }

    @Test
    fun `test update preferences preserves values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        
        repository.updateUserName("John Doe")
        repository.updateCurrencySymbol("€")
        repository.updateThemeMode("Dark")
        repository.updateAccentColor("SkyBlue")
        
        val updatedPrefs = repository.preferences.value
        assertEquals("John Doe", updatedPrefs.userName)
        assertEquals("€", updatedPrefs.currencySymbol)
        assertEquals("Dark", updatedPrefs.themeMode)
        assertEquals("SkyBlue", updatedPrefs.accentColor)

        // Reload to verify it persisted to SharedPreferences
        val reloadedRepository = UserPreferencesRepository(context)
        val reloadedPrefs = reloadedRepository.preferences.value
        assertEquals("John Doe", reloadedPrefs.userName)
        assertEquals("€", reloadedPrefs.currencySymbol)
        assertEquals("Dark", reloadedPrefs.themeMode)
        assertEquals("SkyBlue", reloadedPrefs.accentColor)
    }
}
