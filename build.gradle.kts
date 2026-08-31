// Build script radice: dichiara SOLO le versioni dei plugin (via `apply false`),
// l'applicazione effettiva avviene nel build.gradle.kts del modulo :app.
// Versioni scelte (agosto 2026): Android Gradle Plugin 8.7.3, Kotlin 2.0.21 con
// il plugin Compose dedicato (da Kotlin 2.0 in poi il compilatore Compose non è
// più legato a una versione hard-coded nel modulo, si aggiorna insieme a Kotlin).
// Se apri il progetto con una versione più recente di Android Studio, probabilmente
// suggerirà un aggiornamento di AGP/Kotlin: è sicuro accettarlo.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
