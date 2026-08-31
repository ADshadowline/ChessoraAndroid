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
    }
}

rootProject.name = "ChessoraAndroid"
include(":app")
