package org.chessora.app.ui.chessclock

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Metà dello schermo: TOP è quella capovolta, rivolta al giocatore di fronte. */
enum class ClockSide { TOP, BOTTOM;
    val other: ClockSide get() = if (this == TOP) BOTTOM else TOP
}

enum class ClockPhase {
    /** Scelta di tempo e incremento. */
    SETUP,
    /** Orologio pronto: il primo tocco avvia il tempo dell'avversario di chi tocca. */
    READY,
    RUNNING,
    PAUSED,
    /** Un giocatore ha finito il tempo ([ClockState.flagged]). */
    FLAGGED,
}

data class ClockState(
    val minutes: Int = 5,
    val incrementSeconds: Int = 3,
    val phase: ClockPhase = ClockPhase.SETUP,
    val topMs: Long = 0,
    val bottomMs: Long = 0,
    /** Lato il cui tempo sta scorrendo (null finché nessuno ha toccato). */
    val active: ClockSide? = null,
    val topMoves: Int = 0,
    val bottomMoves: Int = 0,
    val flagged: ClockSide? = null,
) {
    fun timeOf(side: ClockSide) = if (side == ClockSide.TOP) topMs else bottomMs
    fun movesOf(side: ClockSide) = if (side == ClockSide.TOP) topMoves else bottomMoves
}

/**
 * Orologio da scacchi a due lati (Fischer: l'incremento si aggiunge a chi ha appena
 * mosso). Il tempo è misurato con [SystemClock.elapsedRealtime] tra un aggiornamento e
 * l'altro, non contando i "tick": un ritardo del ridisegno non fa mai perdere o
 * guadagnare tempo. Nessun dato salvato sul server.
 */
class ChessClockViewModel : ViewModel() {
    private val _state = MutableStateFlow(ClockState())
    val state: StateFlow<ClockState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var lastTick = 0L

    /** Dalla schermata di scelta: imposta la cadenza e porta l'orologio a READY. */
    fun start(minutes: Int, incrementSeconds: Int) {
        val ms = minutes * 60_000L
        stopTicker()
        _state.value = ClockState(
            minutes = minutes,
            incrementSeconds = incrementSeconds,
            phase = ClockPhase.READY,
            topMs = ms,
            bottomMs = ms,
        )
    }

    /**
     * Tocco sul lato [side]: chi tocca ha appena mosso, quindi parte il tempo
     * dell'avversario. Al primo tocco (READY) nessun incremento: chi tocca avvia
     * l'orologio di chi muove per primo, come su un orologio da torneo. Un tocco sul lato
     * che non sta giocando è ignorato.
     */
    fun press(side: ClockSide) {
        val s = _state.value
        when (s.phase) {
            ClockPhase.READY -> {
                _state.value = s.copy(phase = ClockPhase.RUNNING, active = side.other)
                startTicker()
            }
            ClockPhase.RUNNING -> {
                if (s.active != side) return
                tick()
                // Tempo finito proprio in questo istante: vale la bandierina, niente incremento.
                if (_state.value.phase != ClockPhase.RUNNING) return
                val inc = s.incrementSeconds * 1000L
                _state.update {
                    if (side == ClockSide.TOP) it.copy(topMs = it.topMs + inc, topMoves = it.topMoves + 1, active = ClockSide.BOTTOM)
                    else it.copy(bottomMs = it.bottomMs + inc, bottomMoves = it.bottomMoves + 1, active = ClockSide.TOP)
                }
            }
            else -> Unit
        }
    }

    fun pause() {
        if (_state.value.phase != ClockPhase.RUNNING) return
        tick()
        stopTicker()
        _state.update { it.copy(phase = ClockPhase.PAUSED) }
    }

    fun resume() {
        if (_state.value.phase != ClockPhase.PAUSED) return
        _state.update { it.copy(phase = ClockPhase.RUNNING) }
        startTicker()
    }

    /** Stessa cadenza, tempi azzerati: nuova partita. */
    fun reset() = start(_state.value.minutes, _state.value.incrementSeconds)

    fun backToSetup() {
        stopTicker()
        _state.update { ClockState(minutes = it.minutes, incrementSeconds = it.incrementSeconds) }
    }

    private fun startTicker() {
        stopTicker()
        lastTick = SystemClock.elapsedRealtime()
        ticker = viewModelScope.launch {
            while (isActive) {
                tick()
                if (_state.value.phase != ClockPhase.RUNNING) break
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    /** Scala dal lato attivo il tempo passato dall'ultimo aggiornamento. */
    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastTick
        lastTick = now
        val s = _state.value
        val side = s.active ?: return
        if (s.phase != ClockPhase.RUNNING) return
        val left = (s.timeOf(side) - elapsed).coerceAtLeast(0)
        _state.value = if (side == ClockSide.TOP) s.copy(topMs = left) else s.copy(bottomMs = left)
        if (left == 0L) {
            _state.update { it.copy(phase = ClockPhase.FLAGGED, flagged = side) }
        }
    }

    override fun onCleared() {
        stopTicker()
    }

    private companion object {
        const val TICK_MS = 50L
    }
}
