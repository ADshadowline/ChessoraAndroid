package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** displayName: nome dell'account Google (se disponibile), usato lato server SOLO
 * come fallback per un confronto per nome quando email/telefono non trovano un
 * socio - vedi PlayerIdentityService.IdentifyAsync. Non esiste alcun endpoint per
 * sfogliare l'elenco soci di un circolo dal client. */
@Serializable
data class IdentifyRequestDto(
    val idClub: Int,
    val email: String? = null,
    val phoneNumber: String? = null,
    val displayName: String? = null,
)

/** Come IdentifyRequestDto ma per chi non è socio di alcun circolo (vedi
 * ui/onboarding/MembershipQuestionScreen.kt): ricerca su tutta l'anagrafica
 * nazionale. idFide (facoltativo, digitato dal socio) ha priorità come
 * identificatore forte su email/telefono. */
@Serializable
data class IdentifyNationalRequestDto(
    val email: String? = null,
    val phoneNumber: String? = null,
    val idFide: String? = null,
    val displayName: String? = null,
)

@Serializable
data class IdentifyResultDto(
    val matched: Boolean,
    val idPlayer: Int? = null,
    val name: String? = null,
)

/** Un punto mensile dello storico rating Fide - null nelle tre cadenze significa
 * "nessuna partita valutata quel mese", non zero. */
@Serializable
data class PerformancePointDto(
    val year: Int,
    val month: Int,
    val standard: Int? = null,
    val rapid: Int? = null,
    val blitz: Int? = null,
)

/** hasFide=false quando il socio non ha un IdFide associato in anagrafica: in quel
 * caso points è sempre vuoto e la UI non deve provare a disegnare il grafico. */
@Serializable
data class PerformanceHistoryDto(
    val hasFide: Boolean,
    val points: List<PerformancePointDto> = emptyList(),
)
