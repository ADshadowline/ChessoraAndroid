package org.chessora.app.ui.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.BuildConfig
import org.chessora.app.R

/** Scelta mostrata toccando l'icona "?" della Home desktop: cosa si può fare con l'app
 * oppure le novità per versione. */
@Composable
fun HelpChoiceDialog(onOpenFeatures: () -> Unit, onOpenChangelog: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column {
                HelpChoice(
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    title = stringResource(R.string.help_features_title),
                    subtitle = stringResource(R.string.help_features_subtitle),
                    onClick = { onDismiss(); onOpenFeatures() },
                )
                HelpChoice(
                    icon = { Icon(Icons.Default.NewReleases, contentDescription = null) },
                    title = stringResource(R.string.help_changelog_title),
                    subtitle = stringResource(R.string.help_changelog_subtitle),
                    onClick = { onDismiss(); onOpenChangelog() },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.help_close)) }
        },
    )
}

@Composable
private fun HelpChoice(icon: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = icon,
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Elenco puntato delle funzioni principali (string-array help_features, tradotto). */
@Composable
fun HelpFeaturesScreen() {
    val features = stringArrayResource(R.array.help_features)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.help_features_title), style = MaterialTheme.typography.titleLarge)
        }
        itemsIndexed(features) { index, feature ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(feature, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Novità/correzioni per release (vedi APP_CHANGELOG), con la versione installata evidenziata. */
@Composable
fun HelpChangelogScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.help_changelog_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.help_installed_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        items(APP_CHANGELOG, key = { it.version }) { release ->
            ReleaseCard(release, isInstalled = release.version == BuildConfig.VERSION_NAME)
        }
    }
}

@Composable
private fun ReleaseCard(release: AppRelease, isInstalled: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    release.version,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "  ·  ${release.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            release.changes.forEach { change ->
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                    Text("•", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(change, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
