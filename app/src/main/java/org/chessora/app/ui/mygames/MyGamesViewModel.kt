package org.chessora.app.ui.mygames

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.PlayerGame
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.data.repository.MyGameCache
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toFriendlyMessage
import java.io.File

/** Stato della generazione del PDF delle partite selezionate (vedi MyGamesPdf). */
sealed interface MyGamesPdfState {
    data object Idle : MyGamesPdfState
    data object Generating : MyGamesPdfState
    data class Ready(val file: File) : MyGamesPdfState
    data object Failed : MyGamesPdfState
}

/** null = il socio non si è ancora identificato (vedi ClubPreferences.identifiedPlayerId) -
 * "Le mie partite" richiede sempre un idPlayer, a differenza di "Scacchi Online" che può
 * mostrare partite anche senza identificazione (sono username pubblici esterni, non legate
 * a un socio Chessora). */
class MyGamesViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<PlayerGame>?>>(UiState.Loading)
    val state: StateFlow<UiState<List<PlayerGame>?>> = _state.asStateFlow()

    private val _pdfState = MutableStateFlow<MyGamesPdfState>(MyGamesPdfState.Idle)
    val pdfState: StateFlow<MyGamesPdfState> = _pdfState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val idPlayer = clubPreferences.identifiedPlayerId.first()
                if (idPlayer == null) {
                    _state.value = UiState.Success(null)
                    return@launch
                }
                val games = repository.getMyGames(idPlayer).getOrThrow()
                MyGameCache.putAll(games)
                _state.value = UiState.Success(games)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                _state.value = UiState.Error(e.toFriendlyMessage())
            }
        }
    }

    /** [games] nell'ordine in cui compaiono nell'elenco. */
    fun generatePdf(context: Context, games: List<PlayerGame>) {
        if (games.isEmpty() || _pdfState.value is MyGamesPdfState.Generating) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _pdfState.value = MyGamesPdfState.Generating
            _pdfState.value = try {
                val myName = clubPreferences.identifiedPlayerName.first()
                val file = withContext(Dispatchers.IO) { MyGamesPdf.write(appContext, games, myName) }
                MyGamesPdfState.Ready(file)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                MyGamesPdfState.Failed
            }
        }
    }

    /** Dopo aver aperto la condivisione (o mostrato l'errore). */
    fun pdfHandled() {
        _pdfState.value = MyGamesPdfState.Idle
    }
}
