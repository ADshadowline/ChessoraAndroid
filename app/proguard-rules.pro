# Regole ProGuard/R8 aggiuntive. isMinifyEnabled è false nella release attuale
# (vedi app/build.gradle.kts) quindi questo file non è ancora attivo: è qui
# pronto per quando si deciderà di abilitare la minificazione prima della
# pubblicazione definitiva su Play Store.

# kotlinx.serialization: mantiene i @Serializable usati per i DTO di rete.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class org.chessora.app.data.remote.dto.** {
    *** Companion;
}
