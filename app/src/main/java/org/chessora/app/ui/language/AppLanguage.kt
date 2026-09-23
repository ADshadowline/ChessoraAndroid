package org.chessora.app.ui.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.chessora.app.R

/** Lingue disponibili nell'app - la bandierina (emoji, nessuna risorsa immagine) è
 * mostrata sia nella tile "Lingua" della griglia Home desktop sia nel selettore a tutto
 * schermo al primo avvio (vedi LanguagePicker.kt). Per aggiungerne una nuova: una voce
 * qui + il corrispondente app/src/main/res/values-XX/strings.xml. */
enum class AppLanguage(val tag: String, val flag: String, val labelRes: Int) {
    ITALIAN("it", "🇮🇹", R.string.language_name_italian),
    ENGLISH("en", "🇬🇧", R.string.language_name_english),
    SPANISH("es", "🇪🇸", R.string.language_name_spanish),
}

/** "it"/"en"/"es" - "it" di fallback se non è mai stata scelta esplicitamente una lingua
 * (vedi anche hasChosenLanguage, che invece serve a distinguere "mai scelta" da "scelta
 * l'italiano", per il selettore al primo avvio). */
fun currentAppLanguageTag(): String = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "it"

/** False solo alla primissima apertura dell'app, prima che l'utente scelga una lingua dal
 * selettore a tutto schermo (vedi ChessoraNavHost, destinazione LANGUAGE_PICKER) - dopo la
 * prima scelta resta sempre true, anche se in seguito si riseleziona di nuovo l'italiano. */
fun hasChosenLanguage(): Boolean = !AppCompatDelegate.getApplicationLocales().isEmpty

/** Applica la lingua e fa ricreare da sola MainActivity con le nuove risorse (richiede
 * MainActivity : AppCompatActivity, vedi il commento lì per il perché). */
fun setAppLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
