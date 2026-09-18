package org.chessora.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.chessora.app.R
import org.chessora.app.ui.theme.ChessoraCream
import org.chessora.app.ui.theme.ChessoraGold
import org.chessora.app.ui.theme.ChessoraGold2
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
 * stesso "C" dell'icona dell'app) su un fondo chiaro avorio - elegante e
 * professionale, non lo sfondo scuro della versione precedente.
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
fun SplashScreen(clubLogoUrl: String? = null, onFinished: () -> Unit) {
    var lineProgress by remember { mutableFloatStateOf(0f) }
    var patternAlpha by remember { mutableFloatStateOf(0f) }
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
        val pattern = Animatable(0f)
        pattern.animateTo(1f, tween(700, easing = EaseOutCubic)) { patternAlpha = value }
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

        // Pezzi degli scacchi nei due angoli, disegnati a mano (Path, non glifi
        // Unicode: su Android i simboli scacchistici vengono dirottati dal font
        // emoji a colori di sistema, ignorando qualunque colore/gradiente
        // impostato - da qui l'aspetto "da emoji" lamentato). Ogni pezzo ha
        // un'ombra sfocata che segue la sua sagoma vera più un riempimento a
        // gradiente metallico, su un piccolo tassello di scacchiera. Quelli in
        // basso si muovono dolcemente sopra la scacchiera in loop.
        ChessCorner(
            alpha = patternAlpha,
            alignment = Alignment.TopStart,
            pieces = listOf(PieceType.BISHOP to GoldGradient, PieceType.KNIGHT to RubyGradient),
            animated = false,
        )
        ChessCorner(
            alpha = patternAlpha,
            alignment = Alignment.BottomEnd,
            pieces = listOf(PieceType.ROOK to EmeraldGradient, PieceType.PAWN to GoldGradient),
            animated = true,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

private val GoldGradient = listOf(Color(0xFFF7E7AE), Color(0xFFB8862E))
private val RubyGradient = listOf(Color(0xFFDD8FA0), Color(0xFF6E1229))
private val EmeraldGradient = listOf(Color(0xFF74C9AC), Color(0xFF0E4736))

private enum class PieceType { BISHOP, KNIGHT, ROOK, PAWN }

@Composable
private fun ChessCorner(
    alpha: Float,
    alignment: Alignment,
    pieces: List<Pair<PieceType, List<Color>>>,
    animated: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = alignment,
    ) {
        Box(contentAlignment = Alignment.Center) {
            MiniBoard()
            Row(horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
                pieces.forEachIndexed { index, (type, gradient) ->
                    PieceGlyph(type = type, gradient = gradient, animated = animated, phaseIndex = index)
                }
            }
        }
    }
}

/** Piccolo tassello di scacchiera (2x2) dietro ai pezzi - li ancora visivamente
 * a un contesto scacchistico invece di lasciarli "fluttuare nel vuoto". */
@Composable
private fun MiniBoard() {
    val cell = 26.dp
    Canvas(modifier = Modifier.size(cell * 2)) {
        val c = cell.toPx()
        for (row in 0 until 2) {
            for (col in 0 until 2) {
                val dark = (row + col) % 2 == 1
                drawRect(
                    color = if (dark) ChessoraGold2.copy(alpha = 0.30f) else ChessoraCream.copy(alpha = 0.55f),
                    topLeft = Offset(col * c, row * c),
                    size = Size(c, c),
                )
            }
        }
    }
}

/** Un pezzo disegnato a mano (non un glifo Unicode, vedi commento in
 * [SplashScreen]): un'ombra sfocata che segue la sagoma vera più il pezzo sopra
 * con un riempimento a gradiente che simula un riflesso metallico. Se
 * [animated], oscilla dolcemente sopra la scacchiera (su e giù, avanti e
 * indietro) in loop infinito. */
@Composable
private fun PieceGlyph(type: PieceType, gradient: List<Color>, animated: Boolean, phaseIndex: Int) {
    val pieceSize = 34.dp
    var bobOffset = 0f
    var slideOffset = 0f
    if (animated) {
        val transition = rememberInfiniteTransition(label = "piece$phaseIndex")
        val bob by transition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300 + phaseIndex * 300, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pieceBob$phaseIndex",
        )
        val slide by transition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1700 + phaseIndex * 250, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pieceSlide$phaseIndex",
        )
        bobOffset = bob
        slideOffset = slide
    }

    Box(
        modifier = Modifier
            .size(pieceSize)
            .graphicsLayer { translationY = bobOffset; translationX = slideOffset },
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 3.dp)
                .blur(3.dp),
        ) {
            drawPath(path = pathFor(type, size), color = Color.Black.copy(alpha = 0.30f))
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            drawPath(path = pathFor(type, size), brush = Brush.linearGradient(gradient))
        }
    }
}

private fun pathFor(type: PieceType, size: Size): Path = when (type) {
    PieceType.PAWN -> pawnPath(size)
    PieceType.ROOK -> rookPath(size)
    PieceType.BISHOP -> bishopPath(size)
    PieceType.KNIGHT -> knightPath(size)
}

private fun pawnPath(size: Size): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(w * 0.16f, h * 0.86f, w * 0.84f, h * 0.98f))
        moveTo(w * 0.34f, h * 0.86f)
        lineTo(w * 0.41f, h * 0.54f)
        lineTo(w * 0.59f, h * 0.54f)
        lineTo(w * 0.66f, h * 0.86f)
        close()
        addOval(androidx.compose.ui.geometry.Rect(w * 0.32f, h * 0.14f, w * 0.68f, h * 0.50f))
    }
}

private fun rookPath(size: Size): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(w * 0.14f, h * 0.86f, w * 0.86f, h * 0.98f))
        addRect(androidx.compose.ui.geometry.Rect(w * 0.24f, h * 0.42f, w * 0.76f, h * 0.88f))
        moveTo(w * 0.20f, h * 0.42f)
        lineTo(w * 0.20f, h * 0.14f)
        lineTo(w * 0.34f, h * 0.14f)
        lineTo(w * 0.34f, h * 0.24f)
        lineTo(w * 0.44f, h * 0.24f)
        lineTo(w * 0.44f, h * 0.14f)
        lineTo(w * 0.56f, h * 0.14f)
        lineTo(w * 0.56f, h * 0.24f)
        lineTo(w * 0.66f, h * 0.24f)
        lineTo(w * 0.66f, h * 0.14f)
        lineTo(w * 0.80f, h * 0.14f)
        lineTo(w * 0.80f, h * 0.42f)
        close()
    }
}

private fun bishopPath(size: Size): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(w * 0.18f, h * 0.86f, w * 0.82f, h * 0.98f))
        moveTo(w * 0.5f, h * 0.30f)
        quadraticTo(w * 0.74f, h * 0.52f, w * 0.66f, h * 0.86f)
        lineTo(w * 0.34f, h * 0.86f)
        quadraticTo(w * 0.26f, h * 0.52f, w * 0.5f, h * 0.30f)
        close()
        addOval(androidx.compose.ui.geometry.Rect(w * 0.36f, h * 0.10f, w * 0.64f, h * 0.34f))
        addOval(androidx.compose.ui.geometry.Rect(w * 0.45f, h * 0.02f, w * 0.55f, h * 0.11f))
    }
}

private fun knightPath(size: Size): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(w * 0.16f, h * 0.86f, w * 0.84f, h * 0.98f))
        moveTo(w * 0.30f, h * 0.88f)
        lineTo(w * 0.26f, h * 0.56f)
        quadraticTo(w * 0.24f, h * 0.32f, w * 0.40f, h * 0.20f)
        lineTo(w * 0.34f, h * 0.08f)
        lineTo(w * 0.50f, h * 0.16f)
        lineTo(w * 0.62f, h * 0.08f)
        lineTo(w * 0.76f, h * 0.24f)
        lineTo(w * 0.70f, h * 0.36f)
        lineTo(w * 0.58f, h * 0.32f)
        lineTo(w * 0.62f, h * 0.44f)
        quadraticTo(w * 0.60f, h * 0.58f, w * 0.66f, h * 0.70f)
        lineTo(w * 0.72f, h * 0.88f)
        close()
    }
}
