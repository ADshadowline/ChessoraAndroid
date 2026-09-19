package org.chessora.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import org.chessora.app.BuildConfig
import org.chessora.app.R
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.ui.common.chessoraViewModel

/**
 * "La mia foto" è stata spostata qui da ui/more/MoreScreen.kt su richiesta esplicita.
 * Le immagini di sfondo (apertura app/desktop) sono scelte una volta e restano solo sul
 * dispositivo (mai inviate al server) - vedi ClubPreferences.splashBackgroundUri/
 * desktopBackgroundUri, applicate con scrim automatico in SplashScreen.kt/HomeScreen.kt.
 */
@Composable
fun SettingsScreen(
    club: String?,
    identifiedPlayerName: String?,
    onChangeClub: () -> Unit,
    onIdentify: () -> Unit,
    onOpenProfilePhoto: () -> Unit,
) {
    val viewModel = chessoraViewModel { app -> SettingsViewModel(app.repository, app.clubPreferences) }
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val splashBackgroundUri by viewModel.splashBackgroundUri.collectAsState()
    val desktopBackgroundUri by viewModel.desktopBackgroundUri.collectAsState()
    val context = LocalContext.current

    val splashPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            persistUriPermission(context, uri)
            viewModel.setSplashBackgroundUri(uri.toString())
        }
    }
    val desktopPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            persistUriPermission(context, uri)
            viewModel.setDesktopBackgroundUri(uri.toString())
        }
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

        OutlinedButton(onClick = onChangeClub, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(stringResource(R.string.settings_change_club))
        }

        Text(
            text = identifiedPlayerName?.let { stringResource(R.string.settings_identity_as, it) }
                ?: stringResource(R.string.settings_identity_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
        OutlinedButton(onClick = onIdentify, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(R.string.settings_identity_button))
        }
        OutlinedButton(onClick = onOpenProfilePhoto, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(R.string.settings_my_photo))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it, club) },
            )
        }

        Text(
            stringResource(R.string.settings_display_mode),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 28.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DisplayModeOption(
                label = stringResource(R.string.settings_display_mode_classic),
                selected = displayMode == ClubPreferences.DISPLAY_MODE_CLASSIC,
                onClick = { viewModel.setDisplayMode(ClubPreferences.DISPLAY_MODE_CLASSIC) },
                modifier = Modifier.weight(1f),
            )
            DisplayModeOption(
                label = stringResource(R.string.settings_display_mode_desktop),
                selected = displayMode == ClubPreferences.DISPLAY_MODE_DESKTOP,
                onClick = { viewModel.setDisplayMode(ClubPreferences.DISPLAY_MODE_DESKTOP) },
                modifier = Modifier.weight(1f),
            )
        }

        BackgroundImagePicker(
            title = stringResource(R.string.settings_splash_background),
            uri = splashBackgroundUri,
            onPick = { splashPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemove = { viewModel.setSplashBackgroundUri(null) },
        )
        BackgroundImagePicker(
            title = stringResource(R.string.settings_desktop_background),
            uri = desktopBackgroundUri,
            onPick = { desktopPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemove = { viewModel.setDesktopBackgroundUri(null) },
        )

        Text(
            "${stringResource(R.string.settings_about)}: Chessora ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun DisplayModeOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun BackgroundImagePicker(title: String, uri: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (uri != null) {
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = onPick) { Text(stringResource(R.string.settings_choose_image)) }
            if (uri != null) {
                TextButton(onClick = onRemove, modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.settings_remove_image))
                }
            }
        }
    }
}

/** Le URI del Photo Picker di sistema (Android 11+ con modulo aggiornato, o 13+) non
 * supportano permessi persistenti - il picker garantisce già da sé un accesso di lunga
 * durata, e chiamare questo metodo su una di quelle URI lancia SecurityException. La
 * chiamata resta comunque utile per le URI del picker legacy (pre-Photo-Picker), quindi
 * si tenta e si ignora silenziosamente il fallimento invece di rimuoverla del tutto. */
private fun persistUriPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: SecurityException) {
        // Atteso per le URI del Photo Picker moderno - vedi commento sopra.
    }
}
