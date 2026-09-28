package org.chessora.app.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Testo forzato su una riga sola (mai a capo): se non ci sta nella larghezza
 * disponibile, il font si restringe di un punto alla volta finché non entra (o finché
 * non raggiunge [minFontSize]) - usato per il nome del circolo nella barra superiore
 * (vedi ChessoraNavHost.ClubBrandingTopBar), dove un nome lungo andrebbe altrimenti a
 * capo su due righe. Nessuna libreria esterna: si appoggia solo a onTextLayout, che
 * segnala se il testo ha superato la larghezza disponibile alla dimensione corrente. */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    minFontSize: TextUnit = 12.sp,
) {
    val startingFontSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 16.sp
    var fontSize by remember(text, startingFontSize) { mutableStateOf(startingFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1).sp
            } else {
                readyToDraw = true
            }
        },
    )
}
