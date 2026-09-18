package org.chessora.app.ui.identity

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Login Google via Credential Manager (API raccomandata da Google, sostituisce la
 * vecchia GoogleSignInClient) + Firebase Authentication, usato SOLO per ottenere
 * l'email dell'utente da confrontare con l'anagrafica soci - non crea alcun
 * account/sessione persistita lato app (l'unico stato salvato dopo è l'esito in
 * ClubPreferences, vedi IdentityScreen.kt).
 *
 * Richiede che nel progetto Firebase (già collegato via google-services.json) sia
 * abilitato il provider Google in Authentication, e che [webClientId] (il "Web
 * client ID" OAuth generato da quella console) sia stato inserito in
 * res/values/strings.xml sotto google_signin_web_client_id - finché è vuoto,
 * [signIn] ritorna sempre null senza tentare nulla.
 */
/** [displayName] è il nome dell'account Google (Nome Cognome), usato lato server
 * SOLO come fallback per un confronto per nome quando email/telefono non trovano
 * un socio nel circolo (vedi IdentityScreen.identifyWithGoogleEmail). */
data class GoogleAccountInfo(val email: String, val displayName: String?)

object GoogleSignInHelper {

    suspend fun signIn(context: Context, webClientId: String): GoogleAccountInfo? {
        if (webClientId.isBlank()) return null
        return try {
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result = CredentialManager.create(context).getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
            authResult.user?.email?.let { GoogleAccountInfo(it, authResult.user?.displayName) }
        } catch (e: GetCredentialException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
