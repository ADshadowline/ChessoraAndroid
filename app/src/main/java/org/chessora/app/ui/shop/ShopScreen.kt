package org.chessora.app.ui.shop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Solo vetrina (docs/android-app-spec.md §1 "Fuori scope": niente
 * carrello/acquisto in app - quello resta sul wizard/sito web) e §7.8
 * ("opzionale in v1, sola consultazione").
 */
@Composable
fun ShopScreen(club: String) {
    val viewModel = chessoraViewModel { app -> ShopViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { products ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(products, key = { it.id }) { product -> ProductRow(product) }
        }
    }
}

@Composable
private fun ProductRow(product: ShopProduct) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            val imageUrl = NetworkModule.resolveAssetUrl(product.imagePath)
            if (imageUrl != null) {
                AsyncImage(model = imageUrl, contentDescription = product.name, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }
            Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            product.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Text("€ ${product.price}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
