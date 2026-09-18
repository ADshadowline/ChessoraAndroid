package org.chessora.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.ui.common.chessoraViewModel

/**
 * "La mia foto" (menu Altro): permette al socio già identificato (vedi
 * ui/identity/) di scattare una foto o sceglierne una dalla galleria come
 * proprio avatar - salvata come blob in orga.PlayerContacts, mostrata accanto
 * al proprio nome in Classifica (Circolo/Nazionale) e nelle future Iscrizioni.
 * Nessun crop/editor: solo ridimensionamento/compressione automatici
 * (ProfilePhotoViewModel.compressToJpeg), coerente con la semplicità voluta
 * per questa prima versione.
 */
@Composable
fun ProfilePhotoScreen(onIdentify: () -> Unit) {
    val viewModel = chessoraViewModel { app -> ProfilePhotoViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.load() }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap -> if (bitmap != null) viewModel.uploadBitmap(bitmap) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val bitmap = decodeOrientedBitmap(context, uri)
            if (bitmap != null) viewModel.uploadBitmap(bitmap)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is ProfilePhotoState.Loading -> CircularProgressIndicator()
            is ProfilePhotoState.NotIdentified -> NotIdentifiedContent(onIdentify)
            is ProfilePhotoState.Ready -> ReadyContent(
                photoUrl = playerPhotoUrl(current.idPlayer, current.photoVersion),
                saving = false,
                errorMessage = null,
                onCamera = { cameraLauncher.launch(null) },
                onGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { viewModel.removePhoto() },
            )
            is ProfilePhotoState.Saving -> ReadyContent(
                photoUrl = playerPhotoUrl(current.idPlayer, current.photoVersion),
                saving = true,
                errorMessage = null,
                onCamera = { cameraLauncher.launch(null) },
                onGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { viewModel.removePhoto() },
            )
            is ProfilePhotoState.Error -> ReadyContent(
                photoUrl = playerPhotoUrl(current.idPlayer, current.photoVersion),
                saving = false,
                errorMessage = current.message,
                onCamera = { cameraLauncher.launch(null) },
                onGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { viewModel.removePhoto() },
            )
        }
    }
}

private fun playerPhotoUrl(idPlayer: Int, photoVersion: Int): String =
    NetworkModule.resolveAssetUrl("/api/players/$idPlayer/photo") + "?v=$photoVersion"

@Composable
private fun ReadyContent(
    photoUrl: String,
    saving: Boolean,
    errorMessage: String?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_photo_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.profile_photo_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.size(160.dp).clip(CircleShape),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(160.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (saving) CircularProgressIndicator()
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }

        Button(onClick = onCamera, enabled = !saving, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.profile_photo_camera))
        }
        OutlinedButton(onClick = onGallery, enabled = !saving, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.profile_photo_gallery))
        }
        TextButton(onClick = onRemove, enabled = !saving, modifier = Modifier.padding(top = 12.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.profile_photo_remove))
        }
    }
}

@Composable
private fun NotIdentifiedContent(onIdentify: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.profile_photo_not_identified),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
        )
        Button(onClick = onIdentify) { Text(stringResource(R.string.settings_identity_button)) }
    }
}

/** Le foto scelte dalla galleria portano quasi sempre l'orientamento reale solo
 * nel tag EXIF (i pixel restano "sdraiati") - senza questo, molte foto scattate
 * in verticale arriverebbero ruotate di lato nell'avatar. */
private fun decodeOrientedBitmap(context: Context, uri: Uri): Bitmap? = try {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    if (bitmap == null) {
        null
    } else {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        rotateIfNeeded(bitmap, orientation)
    }
} catch (e: Exception) {
    null
}

private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
