package org.chessora.app.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Login Google via Credential Manager (API raccomandata da Google, sostituisce la
 * vecchia GoogleSignInClient) - restituisce l'ID token grezzo, verificato lato server
 * con Google.Apis.Auth (vedi PlayerAuthService.LoginWithGoogleAsync), non più uno
 * scambio con Firebase Authentication: il client non deve più fidarsi di un'email
 * "già verificata da Firebase", solo il server decide dopo aver validato il token.
 *
 * Richiede che [webClientId] (il "Web client ID" OAuth, non l'Android client ID) sia
 * stato inserito in res/values/strings.xml sotto google_signin_web_client_id -
 * finché è vuoto, [signIn] ritorna sempre null senza tentare nulla.
 */
object GoogleSignInHelper {

    suspend fun signIn(context: Context, webClientId: String): String? {
        if (webClientId.isBlank()) return null
        return try {
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result = CredentialManager.create(context).getCredential(context, request)
            GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        } catch (e: GetCredentialException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
