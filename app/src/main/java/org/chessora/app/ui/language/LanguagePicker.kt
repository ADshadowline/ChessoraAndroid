package org.chessora.app.ui.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.chessora.app.R

/** Una riga per lingua (bandierina + nome), riusata sia nel selettore a tutto schermo del
 * primo avvio sia nel dialog richiamabile dalla tile "Lingua" in Home desktop. */
@Composable
private fun LanguageOptionsList(onSelect: (String) -> Unit) {
    Column {
        AppLanguage.entries.forEach { language ->
            Card(
                onClick = { onSelect(language.tag) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(language.flag, fontSize = 28.sp)
                    Text(
                        stringResource(language.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }
}

/** Selettore lingua a tutto schermo mostrato SOLO al primissimo avvio (vedi
 * ChessoraNavHost, destinazione LANGUAGE_PICKER, prima ancora del login) - senza questo
 * l'utente si troverebbe l'app in una lingua non scelta senza capire come cambiarla. */
@Composable
fun LanguagePickerScreen(onSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.language_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
        )
        LanguageOptionsList(onSelect = onSelected)
    }
}

/** Stesso elenco, in un dialog - richiamato dalla tile "Lingua" nella griglia Home
 * desktop (tra Messaggi e Impostazioni, vedi HomeScreen.DesktopHomeGrid) per cambiare
 * lingua in qualunque momento dopo il primo avvio. */
@Composable
fun LanguagePickerDialog(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_picker_title)) },
        text = { LanguageOptionsList(onSelect = { tag -> onSelect(tag) }) },
        confirmButton = {},
    )
}
