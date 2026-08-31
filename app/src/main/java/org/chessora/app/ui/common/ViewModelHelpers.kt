package org.chessora.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.chessora.app.ChessoraApplication

/**
 * Helper unico per creare ViewModel senza Hilt/Dagger: recupera
 * [ChessoraApplication] (che espone `repository` e `clubPreferences`, creati
 * una sola volta in ChessoraApplication.onCreate) e la passa al lambda [create]
 * che ogni schermata fornisce per costruire il proprio ViewModel specifico.
 *
 * Esempio d'uso in una schermata:
 * ```
 * val viewModel = chessoraViewModel { app -> HomeViewModel(app.repository, app.clubPreferences) }
 * ```
 */
@Composable
fun <VM : ViewModel> chessoraViewModel(create: (ChessoraApplication) -> VM): VM {
    val app = LocalContext.current.applicationContext as ChessoraApplication
    return viewModel(factory = viewModelFactory { initializer { create(app) } })
}
