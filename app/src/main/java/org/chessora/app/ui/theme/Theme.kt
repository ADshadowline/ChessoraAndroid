package org.chessora.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Un solo schema colori (scuro, coerente con lo sfondo scuro della navbar del
 * sito web) invece di light/dark dinamico: l'app non ha ancora un'impostazione
 * "segui il tema di sistema" - se in futuro serve, è qui che va aggiunta la
 * seconda ColorScheme e lo switch in base a isSystemInDarkTheme().
 */
private val ChessoraColorScheme = darkColorScheme(
    primary = ChessoraGold,
    onPrimary = ChessoraInk,
    secondary = ChessoraGold2,
    background = ChessoraInk,
    onBackground = ChessoraCream,
    surface = ChessoraInk2,
    onSurface = ChessoraCream,
    error = ChessoraError,
)

@Composable
fun ChessoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChessoraColorScheme,
        typography = ChessoraTypography,
        content = content,
    )
}
