package com.safelink.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF16735F)
private val Ink = Color.White
private val Surface = Color.Black
private val Warning = Color(0xFFB56A00)
private val Danger = Color(0xFFB3261E)

private val Scheme: ColorScheme = darkColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Warning,
    error = Danger,
    background = Color.Black,
    surface = Surface,
    onBackground = Ink,
    onSurface = Ink,
)

@Composable
fun SafeLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
