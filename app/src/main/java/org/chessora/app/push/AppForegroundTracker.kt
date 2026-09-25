package org.chessora.app.push

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Sa se l'app è attualmente in primo piano - usato da ChessoraFirebaseMessagingService
 * per decidere, quando arriva la push di un turno pubblicato, se mostrare subito la
 * schermata a tutto schermo (vedi ui/pairings/RoundPublishedOverlay.kt, TournamentRoundEvents)
 * o una normale notifica di sistema. Registrato una sola volta in
 * ChessoraApplication.onCreate() su ProcessLifecycleOwner, che segue il ciclo di vita
 * dell'intero processo (non di una singola Activity, che verrebbe ricreata a ogni
 * rotazione/cambio schermata).
 */
object AppForegroundTracker {
    @Volatile
    var isForeground: Boolean = false
        private set

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isForeground = false
            }
        })
    }
}
