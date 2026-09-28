package org.chessora.app.ui.club

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import org.chessora.app.R
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Come raggiungere il circolo - usa il link Maps già pronto impostato dal circolo nel
 * wizard (contact.mapsUrl) se presente, altrimenti costruisce una ricerca Maps
 * dall'indirizzo (contact.address) - mai una mappa incorporata, coerente con ogni altro
 * "apri il link esterno" già presente nell'app (vedi ClubViewModel per il dato). */
@Composable
fun ClubLocationScreen(club: String) {
    val viewModel = chessoraViewModel { app -> ClubViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { settings ->
        val contact = settings.contact
        val mapsUrl = contact.mapsUrl.takeIf { it.isNotBlank() && it != "#" }
            ?: contact.address.takeIf { it.isNotBlank() }
                ?.let { "https://maps.google.com/?q=${URLEncoder.encode(it, "UTF-8")}" }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                contact.address.takeIf { it.isNotBlank() } ?: stringResource(R.string.club_location_address_missing),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (mapsUrl != null) {
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))) },
                    modifier = Modifier.padding(top = 20.dp),
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.club_location_open_maps))
                }
            }
        }
    }
}
