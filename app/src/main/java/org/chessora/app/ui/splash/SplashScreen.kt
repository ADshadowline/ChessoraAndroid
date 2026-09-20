package org.chessora.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.chessora.app.R
import org.chessora.app.ui.common.BackgroundImageWithScrim
import org.chessora.app.ui.theme.ChessoraCream
import org.chessora.app.ui.theme.ChessoraGold
import org.chessora.app.ui.theme.ChessoraInk

// Palette decorativa solo per questa schermata (non tocca i colori semantici del
// tema): oro + rubino + smeraldo profondi su un fondo chiaro color avorio, per un
// effetto elegante "da gioielleria/scacchiera di pregio" invece dello sfondo scuro
// di prima (giudicato troppo cupo).
private val SplashIvoryTop = Color(0xFFFFFDF8)
private val SplashRuby = Color(0xFF8C1F3B)
private val SplashEmerald = Color(0xFF1C6E5C)
private val SplashMuted = Color(0xFF8A7A68)

/**
 * Intro d'apertura dell'app: puramente decorativa, nessuna chiamata di rete -
 * solo Compose, niente Lottie/librerie di animazione esterne (coerente con le
 * dipendenze minime del progetto). Resta a schermo almeno 5 secondi apposta:
 * deve dare un'impressione di cura/qualità del brand, non un caricamento
 * lampo. Mostra l'icona vera di Chessora (drawable/ic_launcher_foreground, lo
 * stesso "C" dell'icona dell'app) su uno sfondo di apertura (personalizzabile
 * dall'utente in Impostazioni, altrimenti l'immagine di default del brand).
 *
 * Se [clubLogoUrl] è valorizzato (un circolo è già stato scelto in una sessione
 * precedente - "riconosciuto", non un primo avvio) mostra il logo di quel
 * circolo al posto della "C" di Chessora; se il circolo non ha un logo, o è il
 * primo avvio, resta l'icona di Chessora.
 *
 * [onFinished] viene chiamato una volta sola al termine dell'animazione:
 * ChessoraNavHost decide lì la vera destinazione (onboarding/identità/home) in
 * base allo stato già noto (circolo scelto? identità già risolta?).
 */
@Composable
fun SplashScreen(clubLogoUrl: String? = null, backgroundUri: String? = null, onFinished: () -> Unit) {
    var lineProgress by remember { mutableFloatStateOf(0f) }
    val glowScale = remember { Animatable(0.7f) }
    val glowAlpha = remember { Animatable(0f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.88f) }
    val logoScale = remember { Animatable(0.9f) }
    val logoAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        glowAlpha.animateTo(0.5f, tween(900, easing = EaseOutCubic))
        glowScale.animateTo(1f, tween(1300, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(150)
        // Sola dissolvenza, come richiesto - nessuna rotazione/rimbalzo sull'icona.
        iconAlpha.animateTo(1f, tween(900, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(150)
        iconScale.animateTo(1f, tween(900, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(550)
        logoAlpha.animateTo(1f, tween(600, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(550)
        logoScale.animateTo(1f, tween(700, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(900)
        val line = Animatable(0f)
        line.animateTo(1f, tween(550, easing = EaseOutCubic)) { lineProgress = value }
    }
    LaunchedEffect(Unit) {
        delay(5000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashIvoryTop, ChessoraCream))),
        contentAlignment = Alignment.Center,
    ) {
        if (backgroundUri != null) {
            BackgroundImageWithScrim(uri = backgroundUri)
        } else {
            BackgroundImageWithScrim(painter = painterResource(R.drawable.splash_background_default))
        }

        // Alone dorato tenue dietro al logo, per profondità - molto più delicato
        // della versione scura precedente.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowAlpha.value; scaleX = glowScale.value; scaleY = glowScale.value },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ChessoraGold.copy(alpha = 0.22f), Color.Transparent),
                        radius = size.minDimension * 0.5f,
                    ),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width / 2f, size.height * 0.42f),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Riquadro bianco semi-trasparente dietro simbolo + scritta "CHESSORA":
            // garantisce leggibilità anche quando l'immagine di apertura è quella
            // personalizzata dall'utente (Impostazioni > Immagine di apertura), che
            // potrebbe non contrastare abbastanza con logo/testo scuri.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.55f))
                    .padding(horizontal = 32.dp, vertical = 24.dp),
            ) {
                val iconModifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        alpha = iconAlpha.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
                    .padding(bottom = 4.dp)
                if (clubLogoUrl != null) {
                    AsyncImage(
                        model = clubLogoUrl,
                        contentDescription = null,
                        modifier = iconModifier.clip(CircleShape),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = iconModifier,
                    )
                }
                Text(
                    text = "CHESSORA",
                    color = ChessoraInk.copy(alpha = logoAlpha.value),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        },
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .width(120.dp)
                    .height(2.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width * lineProgress
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(SplashRuby, ChessoraGold, SplashEmerald)),
                        start = Offset((size.width - w) / 2f, size.height / 2f),
                        end = Offset((size.width + w) / 2f, size.height / 2f),
                        strokeWidth = size.height,
                    )
                }
            }
            Text(
                text = "Il tuo circolo, sempre con te",
                color = SplashMuted.copy(alpha = logoAlpha.value * 0.9f),
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}
