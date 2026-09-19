package org.chessora.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter

/**
 * Immagine di sfondo scelta dall'utente (splash/desktop, vedi
 * ClubPreferences.splashBackgroundUri/desktopBackgroundUri) con uno scrim semi-trasparente
 * SEMPRE applicato sopra, non configurabile - garantisce che testi/icone restino leggibili
 * qualunque immagine venga scelta, come richiesto esplicitamente.
 */
@Composable
fun BackgroundImageWithScrim(uri: String, modifier: Modifier = Modifier) {
    BackgroundImageWithScrim(painter = rememberAsyncImagePainter(uri), modifier = modifier)
}

/** Variante per un'immagine bundled nell'app (es. lo sfondo di apertura di default,
 * drawable/splash_background_default) invece di un content:// URI scelto dall'utente. */
@Composable
fun BackgroundImageWithScrim(painter: Painter, modifier: Modifier = Modifier) {
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
    )
}
