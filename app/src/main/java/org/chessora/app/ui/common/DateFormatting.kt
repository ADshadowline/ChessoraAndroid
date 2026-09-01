package org.chessora.app.ui.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Le date arrivano dal server come stringhe ISO-8601 grezze (vedi i commenti
 * nei DTO in data/remote/dto/): questi helper le convertono in un formato
 * leggibile in italiano solo nel punto in cui vengono mostrate, cosi' i DTO
 * restano semplici stringhe senza dipendere da java.time.
 *
 * java.time è disponibile senza libreria aggiuntiva perché minSdk 26 (Android
 * 8.0) include già il desugaring nativo delle API java.time - niente bisogno
 * di kotlinx-datetime o di core library desugaring esplicito in Gradle.
 */
private val ITALIAN_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy")

/** Es. "31 agosto 2026". Restituisce la stringa originale se il parsing fallisce. */
fun String.toItalianDate(): String = runCatching {
    LocalDateTime.parse(this).format(ITALIAN_DATE)
}.getOrElse {
    runCatching { LocalDate.parse(this.take(10)).format(ITALIAN_DATE) }.getOrDefault(this)
}

/** Utility per costruire l'intervallo "oggi -> oggi+giorni" richiesto da GET /api/calendar. */
object DateRange {
    private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayIso(): String = LocalDate.now().format(ISO_DATE)

    fun todayPlusDaysIso(days: Long): String = LocalDate.now().plusDays(days).format(ISO_DATE)

    /** Usato per evitare crash se un formato data inatteso arriva dal server. */
    fun parseIsoDateOrNull(value: String): LocalDate? =
        try {
            LocalDate.parse(value, ISO_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
}
