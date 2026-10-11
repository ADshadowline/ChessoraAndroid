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
import java.util.Locale
import org.chessora.app.R
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Come raggiungere il circolo - apre Maps sul punto esatto di latitude/longitude del
 * circolo (anag.Clubs, via GET api/clubs/directory) quando disponibili; altrimenti il
 * link Maps impostato dal circolo nel wizard (contact.mapsUrl) o, in ultima istanza, una
 * ricerca Maps dall'indirizzo (contact.address), che può cadere nel punto sbagliato - mai
 * una mappa incorporata, coerente con ogni altro "apri il link esterno" dell'app. */
@Composable
fun ClubLocationScreen(club: String) {
    val viewModel = chessoraViewModel { app -> ClubViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val coordinates by viewModel.coordinates.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(club) {
        viewModel.load(club)
        viewModel.loadCoordinates(club)
    }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { settings ->
        val contact = settings.contact
        // URL universale di Google Maps (apre l'app se installata, altrimenti il browser)
        // con un segnaposto esattamente su lat,lon - Locale.ROOT per avere sempre il punto
        // decimale, mai la virgola della lingua del telefono.
        val mapsUrl = coordinates?.let { "https://www.google.com/maps/search/?api=1&query=${"%.6f".format(Locale.ROOT, it.latitude)},${"%.6f".format(Locale.ROOT, it.longitude)}" }
            ?: contact.mapsUrl.takeIf { it.isNotBlank() && it != "#" }
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
