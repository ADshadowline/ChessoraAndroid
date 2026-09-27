// Punto di ingresso della build Gradle: elenca i moduli inclusi nel progetto.
// Questo repository ha un solo modulo applicativo ("app") - niente moduli
// multipli (:core, :feature-x, ecc.) perché l'app è volutamente piccola e di
// sola consultazione (vedi README.md "Filosofia architetturale").
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Solo per com.github.bhlangonijr:chesslib (motore/parser PGN per "Scacchi
        // Online", vedi ui/lichess/) - non distribuito su Maven Central.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ChessoraAndroid"
include(":app")
