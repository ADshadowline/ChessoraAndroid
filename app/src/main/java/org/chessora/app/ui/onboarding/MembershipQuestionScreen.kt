package org.chessora.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chessora.app.R

/**
 * Primissima domanda al primo avvio (prima ancora della scelta del circolo, vedi
 * ChessoraNavHost.kt): stabilisce se il socio appartiene a un circolo specifico
 * (in tal caso segue il flusso attuale: scelta circolo + identificazione) oppure
 * no (entra in "modalità piattaforma": nessun circolo, Home/News mostrano
 * tornei/notizie di tutti i circoli - vedi ClubPreferences.PLATFORM_CLUB_CODE).
 * In entrambi i casi l'identificazione (Google o telefono) è sempre richiesta più
 * avanti, senza possibilità di saltarla (vedi ui/identity/IdentityScreen.kt).
 */
@Composable
fun MembershipQuestionScreen(onHasClub: () -> Unit, onNoClub: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.membership_question_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.membership_question_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        Button(onClick = onHasClub, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.membership_question_yes))
        }
        OutlinedButton(onClick = onNoClub, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(R.string.membership_question_no))
        }
    }
}
