package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD487), // Soft woodland green for dark mode accessibility
    secondary = SkyBlue,
    tertiary = SunnyYellow,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    primaryContainer = Color(0xFF2D322A),
    onPrimaryContainer = Color(0xFFFCFDF6),
    onPrimary = Color(0xFF0F3900),
    onSecondary = Color(0xFF111E0E),
    onTertiary = Color(0xFF111E0E),
    onBackground = Color(0xFFE2E3DC),
    onSurface = Color(0xFFE2E3DC),
    onSurfaceVariant = CoolGray,
    error = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary = MintGreen, // Rich organic green (#386B20)
    secondary = SkyBlue,
    tertiary = SunnyYellow,
    background = LightBackground, // Soft cream background (#FCFDF6)
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant, // Soft warm sage variant (#F0F2E7)
    primaryContainer = Color(0xFFD7E8CD), // Creamy green (#D7E8CD)
    onPrimaryContainer = Color(0xFF1A1C18), // Warm dark slate (#1A1C18)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DeepSlate, // Natural charcoal (#1A1C18)
    onSurface = DeepSlate,
    onSurfaceVariant = Color(0xFF43493E), // Soft slate green (#43493E)
    error = CoralRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve our bespoke branded interface style
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
