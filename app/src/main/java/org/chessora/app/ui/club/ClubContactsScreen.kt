package org.chessora.app.ui.club

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import org.chessora.app.R
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Contatti del circolo (indirizzo/telefono/email/social) - stessi campi già configurati
 * dal circolo nel wizard (sezione "Identità del Circolo" > Contatti), letti da GET
 * api/site-settings?club= (vedi ClubViewModel). */
@Composable
fun ClubContactsScreen(club: String) {
    val viewModel = chessoraViewModel { app -> ClubViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { settings ->
        val contact = settings.contact
        val hasAnything = listOf(contact.address, contact.phone, contact.email, contact.social.facebook, contact.social.instagram, contact.social.youtube)
            .any { it.isNotBlank() && it != "#" }

        if (!hasAnything) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.club_contacts_empty), textAlign = TextAlign.Center)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                if (contact.address.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(contact.address) },
                        supportingContent = if (contact.cityLine.isNotBlank()) {{ Text(contact.cityLine) }} else null,
                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    )
                }
                if (contact.phone.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(contact.phone) },
                        supportingContent = { Text(stringResource(R.string.club_contacts_call)) },
                        leadingContent = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                        },
                    )
                }
                if (contact.email.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(contact.email) },
                        supportingContent = { Text(stringResource(R.string.club_contacts_email)) },
                        leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")))
                        },
                    )
                }
                listOfNotNull(
                    contact.social.facebook.takeIf { it.isNotBlank() && it != "#" }?.let { "Facebook" to it },
                    contact.social.instagram.takeIf { it.isNotBlank() && it != "#" }?.let { "Instagram" to it },
                    contact.social.youtube.takeIf { it.isNotBlank() && it != "#" }?.let { "YouTube" to it },
                ).forEach { (label, url) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    )
                }
            }
        }
    }
}
