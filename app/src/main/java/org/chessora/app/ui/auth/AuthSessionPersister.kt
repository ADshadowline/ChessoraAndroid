package org.chessora.app.ui.auth

import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.AuthSession
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.PlayerAuthResponseDto

/**
 * Unico punto che traduce un login/registrazione riuscita in stato persistito -
 * chiamato da ogni ViewModel di questo package dopo una risposta 2xx. Scrive sia su
 * [AuthPreferences] (token/idFide/idClub/emailConfirmed/profileComplete, lo stato
 * davvero nuovo) sia su [ClubPreferences] (identifiedPlayerId/authenticatedEmail/
 * isAuthenticated/selectedClub - gli stessi campi già letti da SessionViewModel,
 * TournamentDetailViewModel, RegistrationsViewModel, ProfilePhotoViewModel: riusarli
 * evita di dover riscrivere quei ViewModel per il nuovo sistema di login).
 *
 * [displayName] non fa parte di [PlayerAuthResponseDto] (il server non lo calcola):
 * ogni chiamante lo passa esplicitamente con il nome che ha già a disposizione nel
 * proprio flusso (il nominativo scelto dalla ricerca/elenco soci FIDE, o
 * nome+cognome digitati per la registrazione amatoriale) - null per un login
 * Google/email dove non c'è alcun nome a portata di mano, va bene: mostra solo
 * l'icona di socio riconosciuto senza nome accanto finché l'utente non passa
 * comunque da un profilo con IdPlayer valorizzato.
 */
object AuthSessionPersister {
    suspend fun persist(
        auth: PlayerAuthResponseDto,
        displayName: String?,
        authPreferences: AuthPreferences,
        clubPreferences: ClubPreferences,
    ) {
        authPreferences.saveSession(
            accessToken = auth.accessToken,
            idFide = auth.idFide,
            idClub = auth.idClub,
            clubPublicCode = auth.clubPublicCode,
            emailConfirmed = auth.emailConfirmed,
            profileComplete = auth.profileComplete,
        )
        AuthSession.accessToken = auth.accessToken
        clubPreferences.setAuthenticated(auth.email, null)
        if (auth.idPlayer != null) {
            clubPreferences.setIdentified(auth.idPlayer, displayName ?: auth.email.substringBefore('@'))
        }
        // Se il login ha risolto un circolo (socio riconosciuto), lo si sceglie subito
        // come circolo dell'app - evita di richiedere di nuovo MembershipQuestionScreen/
        // OnboardingScreen a chi ha già dichiarato di essere socio di quel circolo
        // durante il login/completamento profilo (vedi ChessoraNavHost SPLASH).
        if (auth.clubPublicCode != null) {
            clubPreferences.setSelectedClub(auth.clubPublicCode)
        }
    }

    suspend fun clear(authPreferences: AuthPreferences, clubPreferences: ClubPreferences) {
        authPreferences.clearSession()
        AuthSession.accessToken = null
        clubPreferences.clearIdentity()
    }
}
