package org.chessora.app.ui.mygames

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.chessora.app.R

/** Stessi valori di Chessora.Domain.Tournaments.TournamentRegistrationCodes.TipologiaTempo
 * (Standard=0, Rapid=1, Blitz=2, Corrispondenza=3) - riusato qui per "Le mie partite" invece
 * di un enum proprio, cosi' il campo salvato ha lo stesso significato ovunque nell'app anche
 * se questa sezione non ha altro legame con i tornei ufficiali (vedi ui/performance/
 * PerformanceScreen.EloRatingType per il selettore analogo, limitato a 3 valori). */
enum class Cadenza(val tipologiaTempo: Int) {
    STANDARD(0),
    RAPID(1),
    BLITZ(2),
    CORRISPONDENZA(3),
    ;

    companion object {
        fun fromTipologiaTempo(value: Int): Cadenza = entries.firstOrNull { it.tipologiaTempo == value } ?: STANDARD
    }
}

@Composable
fun Cadenza.label(): String = when (this) {
    Cadenza.STANDARD -> stringResource(R.string.performance_standard)
    Cadenza.RAPID -> stringResource(R.string.performance_rapid)
    Cadenza.BLITZ -> stringResource(R.string.performance_blitz)
    Cadenza.CORRISPONDENZA -> stringResource(R.string.performance_correspondence)
}
