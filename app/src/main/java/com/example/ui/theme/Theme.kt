package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color(0xFF04261B),
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = MintText,
    secondary = SaffronBright,
    onSecondary = Color(0xFF2B1700),
    secondaryContainer = Color(0xFF472A00),
    onSecondaryContainer = SaffronSoft,
    tertiary = Color(0xFF7DD3FC),
    background = NightForestBg,
    onBackground = MintText,
    surface = NightForestSurface,
    onSurface = MintText,
    surfaceVariant = NightForestCard,
    onSurfaceVariant = MintMuted,
    outline = Color(0xFF334B3F),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF5C1414),
    onErrorContainer = TerracottaSoft,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDeep,
    secondary = SaffronAccent,
    onSecondary = Color.White,
    secondaryContainer = SaffronSoft,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldVibrant,
    onTertiary = Color.White,
    background = IvoryCanvas,
    onBackground = CharcoalInk,
    surface = CardWhite,
    onSurface = CharcoalInk,
    surfaceVariant = SandSurface,
    onSurfaceVariant = SageMuted,
    outline = OutlineStone,
    error = TerracottaAlert,
    onError = Color.White,
    errorContainer = TerracottaSoft,
    onErrorContainer = TerracottaAlert,
  )

val GramShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
  )

@Composable
fun GramConnectTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = GramShapes,
    content = content,
  )
}
