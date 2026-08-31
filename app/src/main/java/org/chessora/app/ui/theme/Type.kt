package org.chessora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Nessun font custom scaricato in questa prima versione (il sito web usa
// 'Playfair Display' per i titoli - vedi Chessora.Application/SiteSettings/
// default-site-config.json nel repository server): il default di sistema è
// una scelta deliberata per non appesantire l'APK con font Google Fonts finché
// non è chiaro se vale la pena replicare il branding tipografico in app.
val ChessoraTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
)
