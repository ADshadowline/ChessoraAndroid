package org.chessora.app.ui.chessclock

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.chessora.app.R

/** Cadenze più usate: minuti + incremento in secondi. */
private val PRESETS = listOf(1 to 0, 3 to 0, 3 to 2, 5 to 0, 5 to 3, 10 to 0, 10 to 5, 15 to 10, 25 to 10, 90 to 30)

/**
 * Orologio da scacchi: prima si scelgono tempo e incremento, poi lo schermo si divide in
 * due metà, una per giocatore. Il telefono va appoggiato accanto alla scacchiera: la metà
 * in alto è capovolta così che ognuno legga il proprio tempo dal suo lato. Dopo la mossa
 * si tocca la propria metà e parte il tempo dell'avversario. Durante il gioco lo schermo
 * resta acceso e l'orientamento è bloccato in verticale (un telefono appoggiato piatto
 * altrimenti può ruotare da solo).
 */
@Composable
fun ChessClockScreen(onBack: () -> Unit) {
    val viewModel: ChessClockViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    if (state.phase == ClockPhase.SETUP) {
        ClockSetup(
            initialMinutes = state.minutes,
            initialIncrement = state.incrementSeconds,
            onStart = viewModel::start,
            onBack = onBack,
        )
    } else {
        KeepScreenOnPortrait()
        // Indietro durante il gioco mette in pausa invece di uscire; da fermo torna alla
        // scelta della cadenza.
        BackHandler {
            if (state.phase == ClockPhase.RUNNING) viewModel.pause() else viewModel.backToSetup()
        }
        ClockBoard(
            state = state,
            onPress = viewModel::press,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onReset = viewModel::reset,
            onSettings = viewModel::backToSetup,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClockSetup(
    initialMinutes: Int,
    initialIncrement: Int,
    onStart: (Int, Int) -> Unit,
    onBack: () -> Unit,
) {
    var minutesText by rememberSaveable { mutableStateOf(initialMinutes.toString()) }
    var incrementText by rememberSaveable { mutableStateOf(initialIncrement.toString()) }
    val minutes = minutesText.toIntOrNull()
    val increment = incrementText.toIntOrNull()
    val valid = minutes != null && minutes in 1..600 && increment != null && increment in 0..600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Text(stringResource(R.string.chess_clock_title), style = MaterialTheme.typography.headlineSmall)
        }
        Text(stringResource(R.string.chess_clock_setup_hint), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = minutesText,
            onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(R.string.chess_clock_minutes)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = incrementText,
            onValueChange = { incrementText = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(R.string.chess_clock_increment)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.chess_clock_presets), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { (m, inc) ->
                FilterChip(
                    selected = minutes == m && increment == inc,
                    onClick = { minutesText = m.toString(); incrementText = inc.toString() },
                    label = { Text("$m+$inc") },
                )
            }
        }

        if (!valid) {
            Text(stringResource(R.string.chess_clock_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { if (valid) onStart(minutes!!, increment!!) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.chess_clock_start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ClockBoard(
    state: ClockState,
    onPress: (ClockSide) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Metà in alto capovolta: la legge il giocatore seduto di fronte.
        ClockHalf(state, ClockSide.TOP, onPress, Modifier.weight(1f).rotate(180f))
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val controlTint = Color.White
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.chess_clock_settings), tint = controlTint)
            }
            when (state.phase) {
                ClockPhase.RUNNING -> IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.chess_clock_pause), tint = controlTint)
                }
                ClockPhase.PAUSED -> IconButton(onClick = onResume) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.chess_clock_resume), tint = controlTint)
                }
                else -> Spacer(Modifier.height(48.dp))
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.chess_clock_reset), tint = controlTint)
            }
        }
        ClockHalf(state, ClockSide.BOTTOM, onPress, Modifier.weight(1f))
    }
}

@Composable
private fun ClockHalf(state: ClockState, side: ClockSide, onPress: (ClockSide) -> Unit, modifier: Modifier) {
    val haptics = LocalHapticFeedback.current
    val isActive = state.active == side && state.phase == ClockPhase.RUNNING
    val isFlagged = state.flagged == side
    val background = when {
        isFlagged -> Color(0xFFC62828)
        isActive -> Color(0xFFF2B33D)
        else -> Color(0xFF2B2B2B)
    }
    val foreground = if (isActive) Color(0xFF111111) else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            // onPress, non onTap: il tempo passa all'avversario appena il dito tocca,
            // come il pulsante di un orologio vero.
            .pointerInput(state.phase, state.active) {
                detectTapGestures(onPress = {
                    val accepted = state.phase == ClockPhase.READY ||
                        (state.phase == ClockPhase.RUNNING && state.active == side)
                    if (accepted) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPress(side)
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val time = formatClock(state.timeOf(side))
            Text(
                text = time,
                color = foreground,
                // Più piccolo solo per "1:30:00": deve stare su una riga anche su uno
                // schermo stretto. Legato alla lunghezza, non misurato a ogni tick.
                fontSize = if (time.length >= 7) 64.sp else 88.sp,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                // Cifre a larghezza fissa: il numero non "balla" mentre scorre.
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center,
            )
            val caption = when {
                isFlagged -> stringResource(R.string.chess_clock_flag)
                state.phase == ClockPhase.READY -> stringResource(R.string.chess_clock_tap_to_start)
                state.phase == ClockPhase.PAUSED -> stringResource(R.string.chess_clock_paused)
                else -> stringResource(R.string.chess_clock_moves, state.movesOf(side))
            }
            Text(
                text = caption,
                color = foreground.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

/** Ore:mm:ss oltre l'ora, m:ss sotto, e i decimi negli ultimi 10 secondi. */
private fun formatClock(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        ms < 10_000 -> "%d:%02d.%d".format(minutes, seconds, (ms % 1000) / 100)
        else -> "%d:%02d".format(minutes, seconds)
    }
}

/** Schermo sempre acceso e verticale finché l'orologio è visibile; ripristinati all'uscita. */
@Composable
private fun KeepScreenOnPortrait() {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
        view.keepScreenOn = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            view.keepScreenOn = false
            if (previousOrientation != null) activity?.requestedOrientation = previousOrientation
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
