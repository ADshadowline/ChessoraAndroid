package org.chessora.app.ui.common

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val LIGHT_CHIP = Color.White
private val DARK_CHIP = Color(0xFF1E2A44)

/**
 * Logo del circolo intero (Fit, mai ritagliato) su un riquadro con bordo e ombra leggera.
 * Lo sfondo dipende dal logo: scuro per un logo chiaro (es. PoliTO, scritte bianche su
 * trasparente, che su bianco spariva), bianco per un logo scuro - deciso dalla luminosità
 * media dei pixel non trasparenti appena l'immagine è caricata.
 */
@Composable
fun ClubLogoChip(url: String, modifier: Modifier = Modifier, cornerRadius: Dp = 12.dp, contentPadding: Dp = 5.dp) {
    val context = LocalContext.current
    var lightLogo by remember(url) { mutableStateOf<Boolean?>(null) }
    // allowHardware(false): un bitmap "hardware" non si può leggere pixel per pixel.
    val request = remember(url) { ImageRequest.Builder(context).data(url).allowHardware(false).build() }
    val shape = RoundedCornerShape(cornerRadius)
    val background = if (lightLogo == true) DARK_CHIP else LIGHT_CHIP
    Box(
        modifier = modifier
            .shadow(3.dp, shape)
            .clip(shape)
            .background(background)
            .border(1.dp, if (lightLogo == true) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f), shape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { state ->
                val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) lightLogo = isMostlyLight(bitmap)
            },
        )
    }
}

/** True se i pixel visibili (alpha > 40%) sono in media chiari: campiona una griglia di
 * circa 60×60 punti, non tutti i pixel. */
private fun isMostlyLight(bitmap: Bitmap): Boolean {
    val stepX = maxOf(1, bitmap.width / 60)
    val stepY = maxOf(1, bitmap.height / 60)
    var sum = 0.0
    var count = 0
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val p = bitmap.getPixel(x, y)
            if ((p ushr 24) > 100) {
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                sum += 0.299 * r + 0.587 * g + 0.114 * b
                count++
            }
            x += stepX
        }
        y += stepY
    }
    return count > 0 && sum / count > 170
}
