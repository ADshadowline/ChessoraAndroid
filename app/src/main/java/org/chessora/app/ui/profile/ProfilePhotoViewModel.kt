package org.chessora.app.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.repository.ChessoraRepository

/** Stati della schermata "La mia foto" (vedi ProfilePhotoScreen.kt). [Ready.photoVersion]
 * cambia a ogni upload/rimozione riuscita solo per invalidare la cache di Coil
 * sull'URL della foto (altrimenti mostrerebbe quella vecchia, essendo lo stesso URL). */
sealed interface ProfilePhotoState {
    data object Loading : ProfilePhotoState
    data object NotIdentified : ProfilePhotoState
    data class Ready(val idPlayer: Int, val photoVersion: Int) : ProfilePhotoState
    data class Saving(val idPlayer: Int, val photoVersion: Int) : ProfilePhotoState
    data class Error(val message: String, val idPlayer: Int, val photoVersion: Int) : ProfilePhotoState
}

class ProfilePhotoViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfilePhotoState>(ProfilePhotoState.Loading)
    val state: StateFlow<ProfilePhotoState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = ProfilePhotoState.Loading
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            _state.value = if (idPlayer == null) ProfilePhotoState.NotIdentified else ProfilePhotoState.Ready(idPlayer, 0)
        }
    }

    fun uploadBitmap(bitmap: Bitmap) {
        val (idPlayer, version) = currentIdAndVersion() ?: return
        viewModelScope.launch {
            _state.value = ProfilePhotoState.Saving(idPlayer, version)
            repository.setPlayerPhoto(idPlayer, compressToJpeg(bitmap))
                .onSuccess { _state.value = ProfilePhotoState.Ready(idPlayer, version + 1) }
                .onFailure { _state.value = ProfilePhotoState.Error("Caricamento non riuscito, riprova.", idPlayer, version) }
        }
    }

    fun removePhoto() {
        val (idPlayer, version) = currentIdAndVersion() ?: return
        viewModelScope.launch {
            _state.value = ProfilePhotoState.Saving(idPlayer, version)
            repository.deletePlayerPhoto(idPlayer)
                .onSuccess { _state.value = ProfilePhotoState.Ready(idPlayer, version + 1) }
                .onFailure { _state.value = ProfilePhotoState.Error("Rimozione non riuscita, riprova.", idPlayer, version) }
        }
    }

    private fun currentIdAndVersion(): Pair<Int, Int>? = when (val current = _state.value) {
        is ProfilePhotoState.Ready -> current.idPlayer to current.photoVersion
        is ProfilePhotoState.Error -> current.idPlayer to current.photoVersion
        else -> null
    }

    /** Riduce al minimo lo spazio occupato dal blob in orga.PlayerContacts, su
     * richiesta esplicita: (1) ridimensiona al lato lungo massimo 256px - più
     * che sufficiente per un avatar mostrato accanto a un nome, mai a schermo
     * intero; (2) riduce la tavola dei colori convertendo a RGB_565 (16 bit,
     * 65536 colori) invece dei 32 bit ARGB_8888 tipici di una foto della
     * fotocamera/galleria, prima ancora di comprimere; (3) compressione JPEG a
     * qualità moderata (70) invece che alta. */
    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val maxDimension = 256
        val largestSide = maxOf(bitmap.width, bitmap.height)
        val resized = if (largestSide > maxDimension) {
            val scale = maxDimension.toFloat() / largestSide
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else bitmap
        val reducedPalette = resized.copy(Bitmap.Config.RGB_565, false) ?: resized

        return ByteArrayOutputStream().use { out ->
            reducedPalette.compress(Bitmap.CompressFormat.JPEG, 70, out)
            out.toByteArray()
        }
    }
}
