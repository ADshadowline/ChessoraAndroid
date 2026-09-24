package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.PlayerAuth.PlayerAuthResponse - restituito da ogni
 * login/registrazione riuscita. profileComplete=false solo subito dopo un primo login
 * Google di un utente nuovo: il client deve proseguire con POST /api/me/complete-profile
 * (stesso accessToken, già utilizzabile) prima di considerarsi loggato del tutto. */
@Serializable
data class PlayerAuthResponseDto(
    val accessToken: String,
    val email: String,
    val idFide: Int? = null,
    val idPlayer: Int? = null,
    val idClub: Int? = null,
    val clubPublicCode: String? = null,
    val clubName: String? = null,
    val emailConfirmed: Boolean = false,
    val profileComplete: Boolean = false,
)

/** Specchio minimo di Chessora.Contracts.Auth.LoginResponse - risposta di POST
 * api/tourn-organizers/claim (vedi OrganizerSession/ChessoraRepository.ensureOrganizerAuth):
 * un contratto DIVERSO da PlayerAuthResponseDto (Token invece di AccessToken, più un
 * Role/IdClub che il token giocatore non ha). Bastano i tre campi usati: ignoreUnknownKeys
 * (vedi NetworkModule) scarta il resto (ExpiresAtUtc, UserName, AllowedMenuCategories/
 * Steps, ClubPublicCode/Name - pensati per il wizard, non usati qui). */
@Serializable
data class OrganizerLoginResponseDto(
    val token: String,
    val role: String,
    val idClub: Int? = null,
)

@Serializable
data class GoogleLoginResponseDto(val auth: PlayerAuthResponseDto)

@Serializable
data class GoogleLoginRequestDto(val idToken: String)

@Serializable
data class PlayerLoginRequestDto(val emailOrIdFide: String, val password: String)

/** Secondo passo dopo un login Google con profilo incompleto (FLUSSO 3/4) - il
 * chiamante è già autenticato (Bearer già ottenuto dal login Google). hasIdFide=false
 * (amatoriale) richiede firstName/lastName/password/confirmPassword; hasIdFide=true
 * richiede idFide (già verificato lato client con checkFide) e isClubMember, con
 * selectedIdClub/selectedIdPlayer valorizzati solo se il club non si è auto-risolto e
 * l'utente ha scelto manualmente dall'elenco soci. */
@Serializable
data class CompletePlayerProfileRequestDto(
    val hasIdFide: Boolean,
    val idFide: Int? = null,
    val isClubMember: Boolean = false,
    val selectedIdClub: Int? = null,
    val selectedIdPlayer: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
)

/** Usato quando un ID FIDE risulta valido ma non ancora associato a nessun utente
 * (FLUSSO 6, accesso con ID FIDE) - crea l'account SUBITO associato a quell'IdFide,
 * richiede conferma email prima di poter accedere. */
@Serializable
data class RegisterWithFideRequestDto(
    val idFide: Int,
    val email: String,
    val password: String,
    val isClubMember: Boolean = false,
    val selectedIdClub: Int? = null,
    val selectedIdPlayer: Int? = null,
)

@Serializable
data class RegisterResponseDto(val requiresEmailConfirmation: Boolean, val email: String)

@Serializable
data class ForgotPasswordRequestDto(val email: String)

@Serializable
data class ResendConfirmationRequestDto(val email: String)

/** Esito di GET /api/auth/check-fide - resolvedIdClub/clubPublicCode/clubName sono
 * valorizzati solo se il club è auto-risolvibile (orga.PlayerCard, anno corrente); se
 * null e l'utente dichiara di essere socio di un circolo, va proposta la selezione
 * manuale (club + proprio nome dall'elenco soci, vedi getClubRoster). */
@Serializable
data class CheckFideResultDto(
    val alreadyRegistered: Boolean,
    val name: String,
    val idPlayer: Int? = null,
    val resolvedIdClub: Int? = null,
    val clubPublicCode: String? = null,
    val clubName: String? = null,
)

/** Un risultato della ricerca per nome su anag.FidePlayers.Name - name è già formattato
 * "Cognome, Nome" per la UI di conferma esplicita ("Hai selezionato: COGNOME, Nome —
 * ID FIDE XXXXXXXX"). */
@Serializable
data class FidePlayerSearchResultDto(
    val idFide: Int,
    val name: String,
    val standardRating: Int? = null,
    val rapidRating: Int? = null,
    val blitzRating: Int? = null,
)

/** Una riga dell'elenco soci di un circolo, per la selezione manuale del proprio
 * nominativo quando il club non è auto-risolvibile. */
@Serializable
data class ClubRosterEntryDto(val idPlayer: Int, val name: String)
