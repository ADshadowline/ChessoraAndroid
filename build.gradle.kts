// Build script radice: dichiara SOLO le versioni dei plugin (via `apply false`),
// l'applicazione effettiva avviene nel build.gradle.kts del modulo :app.
// Versioni: Android Gradle Plugin 8.10.1 (ottobre 2026: serve almeno l'8.10 per
// compileSdk/targetSdk 36, richiesto dal Google Play Store; prima era 8.7.3), Kotlin
// 2.0.21 con il plugin Compose dedicato (da Kotlin 2.0 in poi il compilatore Compose
// non è più legato a una versione hard-coded nel modulo, si aggiorna insieme a Kotlin).
// AGP 8.10 richiede Gradle 8.11.1 (vedi gradle/wrapper/gradle-wrapper.properties).
// Se apri il progetto con una versione più recente di Android Studio, probabilmente
// suggerirà un aggiornamento di AGP/Kotlin: è sicuro accettarlo.
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}
