// Modulo applicativo unico (vedi settings.gradle.kts). Stack: Kotlin + Jetpack
// Compose (Material 3) + Retrofit/OkHttp + kotlinx.serialization + Coil + Jetpack
// DataStore + Firebase Cloud Messaging - esattamente lo stack raccomandato in
// docs/android-app-spec.md (nel repository server Chessora) §2.
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

// Credenziali della chiave di firma release, lette da keystore.properties
// (mai committato - vedi .gitignore e il commento in quel file). Se manca
// (es. clone fresco senza ancora una chiave di release), releaseSigning
// resta null e la release build FALLISCE con un errore chiaro invece di
// produrre silenziosamente un pacchetto non firmato o firmato col debug key.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val releaseSigning: Properties? = if (keystorePropertiesFile.exists()) {
    Properties().apply { load(keystorePropertiesFile.inputStream()) }
} else null

android {
    namespace = "org.chessora.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.chessora.app"
        // minSdk 26 (Android 8.0) come da specifica: sotto questa versione Jetpack
        // Compose e le API moderne di notifica perdono troppe funzionalità per
        // valerne la pena su un'app di sola consultazione.
        minSdk = 26
        targetSdk = 35
        versionCode = 72
        versionName = "1.33.1"

        // URL base dell'Api Chessora in produzione: iniettato come BuildConfig
        // string invece che hard-codato nel client Retrofit, cosi' un domani un
        // build "debug" potrebbe puntare a un ambiente diverso senza toccare il
        // codice (vedi NetworkModule.kt).
        buildConfigField("String", "API_BASE_URL", "\"https://api.chessora.org/\"")
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Jetpack Compose (BOM: allinea automaticamente le versioni delle librerie Compose) ---
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Ciclo di vita / ViewModel / Navigation ---
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    // Solo per AppCompatDelegate.setApplicationLocales (selettore lingua in
    // Impostazioni, ui/settings/) - funziona anche senza AppCompatActivity (MainActivity
    // resta un ComponentActivity puro Compose): dalla 1.6.0 in poi la libreria registra
    // da sé, via manifest merge, il meccanismo di "auto-store" della lingua scelta e il
    // recreate() automatico dell'Activity, senza bisogno di ereditare da AppCompatActivity
    // né di persistere la scelta a mano (vedi SettingsViewModel.setLanguage).
    implementation("androidx.appcompat:appcompat:1.7.0")
    // Dichiarata esplicitamente (e' gia' presente transitivamente via
    // activity-compose) solo perche' altrimenti lint non riesce a risolvere la
    // versione effettiva e segnala erroneamente "InvalidFragmentVersionForActivityResult"
    // su MainActivity.registerForActivityResult, bloccando la release build.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // LifecycleEventEffect (ON_RESUME) - usato per riaggiornare i segni di spunta
    // "preiscritto" quando si torna su Home/Eventi/Calendario/Iscrizioni dal dettaglio
    // di un torneo, senza il quale resterebbero al valore letto alla primissima
    // apertura della schermata (il ViewModel non viene ricreato tornando indietro).
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // ProcessLifecycleOwner - sapere se l'app è in primo piano quando arriva una push
    // "turno pubblicato" (vedi push/AppForegroundTracker.kt): in foreground si mostra
    // subito la schermata a tutto schermo, in background la normale notifica di sistema.
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // --- Rete: Retrofit + OkHttp + kotlinx.serialization (niente Gson: coerente
    // con l'uso di @Serializable per i DTO, evita di tenere due librerie JSON) ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // Estensione .await() su com.google.android.gms.tasks.Task, usata in
    // push/DeviceRegistration.kt per FirebaseMessaging.getInstance().token.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // --- Persistenza leggera locale: idClub scelto, preferenza notifiche ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Immagini remote (loghi, foto giocatori, locandine) ---
    implementation("io.coil-kt:coil-compose:2.7.0")

    // --- Orientamento EXIF delle foto scelte dalla galleria per il profilo
    // (ui/profile/): senza questo, molte foto scattate in verticale
    // arriverebbero ruotate di lato, perché il rotolo/EXIF applica la
    // rotazione solo a lettura, non ai pixel del file. ---
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // --- Firebase Cloud Messaging (solo il modulo Messaging, niente Analytics: non
    // serve e complicherebbe l'informativa privacy) ---
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // --- Login Google (ui/auth/): Credential Manager (API moderna raccomandata da
    // Google, sostituisce la vecchia GoogleSignInClient) - restituisce l'ID token
    // grezzo, verificato lato server con Google.Apis.Auth, non più uno scambio con
    // Firebase Authentication. ---
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // --- Test (scaffolding minimo di default, nessun test scritto in questa
    // prima versione - vedi README.md "Cosa NON è stato fatto") ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
