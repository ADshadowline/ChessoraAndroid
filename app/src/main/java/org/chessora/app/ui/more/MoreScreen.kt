package org.chessora.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Raccoglie le schermate secondarie (docs/android-app-spec.md §7.7-7.9:
 * Direttivo, Negozio, Impostazioni) in un unico menu invece di occupare altri
 * 3 slot nella bottom bar - vedi ui/navigation/ChessoraDestinations.kt.
 */
@Composable
fun MoreScreen(onBoardClick: () -> Unit, onShopClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        ListItem(
            headlineContent = { Text("Direttivo") },
            leadingContent = { Icon(Icons.Default.People, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onBoardClick),
        )
        ListItem(
            headlineContent = { Text("Negozio") },
            leadingContent = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onShopClick),
        )
        ListItem(
            headlineContent = { Text("Impostazioni") },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onSettingsClick),
        )
    }
}
