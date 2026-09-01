package org.chessora.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Prima schermata mai vista dall'utente (docs/android-app-spec.md §3, §7.1).
 * [onClubSelected] è fornito dal chiamante (ChessoraNavHost) e normalmente
 * invoca SessionViewModel.selectClub + naviga verso Home.
 */
@Composable
fun OnboardingScreen(onClubSelected: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> OnboardingViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val codeError by viewModel.codeError.collectAsState()
    var codeInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text(stringResource(R.string.onboarding_search_hint)) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            singleLine = true,
        )

        UiStateContent(state = state, onRetry = viewModel::loadDirectory) { clubs ->
            if (clubs.isEmpty()) {
                Text(stringResource(R.string.onboarding_empty), modifier = Modifier.padding(top = 24.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(clubs, key = { it.publicCode }) { club ->
                        ClubRow(club = club, onClick = { viewModel.resolveCode(club.publicCode, onResolved = onClubSelected) })
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text(stringResource(R.string.onboarding_code_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                isError = codeError != null,
                supportingText = { codeError?.let { Text(it) } },
            )
        }
        Button(
            onClick = { viewModel.resolveCode(codeInput, onResolved = onClubSelected) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.onboarding_code_button))
        }
    }
}

@Composable
private fun ClubRow(club: ClubDirectoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(club.name, style = MaterialTheme.typography.titleMedium)

            // Es. "Milano (MI)" - solo città, solo provincia, o niente se entrambe mancanti.
            val locationLine = when {
                club.cityName != null && club.provinceCode != null -> "${club.cityName} (${club.provinceCode})"
                club.cityName != null -> club.cityName
                else -> club.provinceCode
            }
            if (!locationLine.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(locationLine, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
