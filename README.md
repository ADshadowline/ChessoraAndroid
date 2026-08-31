# Chessora Android

App companion Android della piattaforma Chessora (circoli scacchistici):
sola consultazione + notifiche push, nessun login personale. Scritta a
partire dalla specifica `docs/android-app-spec.md` del repository server
**Chessora** (`c:\Users\adorato\source\repos\ADshadowline\Chessora`, repository
sibling di questo — leggilo per primo, è la fonte di verità su cosa l'app deve
fare e quali endpoint usa).

> **Questo file è scritto per una futura sessione di Claude Code che
> riprenderà lo sviluppo senza memoria di questa conversazione.** È lungo
> apposta: leggilo tutto prima di modificare qualcosa, specialmente le sezioni
> "Stato del progetto e limitazioni" e "Decisioni architetturali" - spiegano
> il *perché* di scelte che altrimenti sembrerebbero arbitrarie o incomplete.

## Stato del progetto e limitazioni

Questo progetto è stato scaffoldato da zero (Domain layer, networking,
DataStore, FCM, navigazione, tutte le 9 schermate previste dalla specifica)
in un ambiente **senza JDK, Gradle o Android SDK installati** - solo i file
sorgente sono stati scritti, **nessuna riga di codice è mai stata compilata o
eseguita**. Questo è il primo compito da fare aprendo il progetto in Android
Studio:

1. Apri la cartella con Android Studio (versione recente, 2024.x+). Ti
   proporrà di generare/scaricare il Gradle wrapper mancante (vedi sotto) e
   sincronizzare - accetta.
2. **`app/google-services.json` è già presente** (progetto Firebase
   "chessora-app", package `org.chessora.app` - vedi
   `Chessora/scripts/deploy.settings.json` chiave `firebaseServiceAccountJson`
   per la controparte server, stesso progetto Firebase). Non serve
   ri-scaricarlo, a meno che l'app venga ri-registrata con un package name
   diverso in futuro.
3. `gradle/wrapper/gradle-wrapper.jar` è MANCANTE (impossibile generarlo senza
   Gradle/JVM installati in questo ambiente) - `gradle/wrapper/gradle-wrapper.properties`
   e `gradlew.bat` ci sono già, puntano a Gradle 8.9. Android Studio lo
   genera in automatico al primo sync ("Sync Project with Gradle Files").
   Se serve rigenerarlo a mano con Gradle già installato altrove: `gradle
   wrapper --gradle-version 8.9`. **Una volta generato, va committato** (non
   è nel `.gitignore` - il wrapper esiste apposta per essere versionato, cosi'
   chiunque clona il repo builda con la stessa versione di Gradle senza
   doverla installare a parte).
4. Solo `gradlew.bat` è presente (sviluppo su Windows) - se serve sviluppare
   anche da Mac/Linux, genera `gradlew` (script Unix) con lo stesso comando
   `gradle wrapper` del punto precedente.
5. Nessuna icona "vera" per l'app: `app/src/main/res/drawable/ic_launcher_foreground.xml`
   è un placeholder (una pedina stilizzata disegnata a mano in XML vettoriale,
   vedi il commento nel file) - da sostituire con un'icona disegnata prima di
   pubblicare su Play Store.
6. **Zero test scritti** (né unit né strumentali) - solo lo scaffolding di
   default delle dipendenze di test in `app/build.gradle.kts`.
7. Nessuna build è mai stata lanciata: aspettati piccoli errori di sintassi o
   import mancanti/superflui alla prima compilazione (scritto a mano,
   controllato a occhio, ma senza un compilatore a verificare). Sistemali via
   via che emergono, non serve riscrivere la struttura per questo.

## Cosa fa l'app (in breve)

Vedi `docs/android-app-spec.md` §1 nel repository server per lo scope
completo. In sintesi: l'utente sceglie un circolo (niente login), poi
consulta in sola lettura news, calendario, tornei (con locandina se
presente), classifica (circolo/nazionale/assoluta, standard/rapid/blitz),
direttivo, negozio (solo vetrina); riceve notifiche push inviate dal
super-amministratore della piattaforma.

## Decisioni architetturali

- **Kotlin + Jetpack Compose (Material 3), MVVM, un solo modulo Gradle**
  (`:app`) - niente multi-modulo, l'app è piccola e di sola consultazione.
- **Niente Hilt/Dagger**: dependency injection manuale tramite
  `ChessoraApplication` (crea `ChessoraRepository`/`ClubPreferences` una sola
  volta) + `ui/common/ViewModelHelpers.kt` (`chessoraViewModel { app -> ... }`).
  Con così poche dipendenze, un framework DI con annotation processing
  sarebbe overhead puro. Se il progetto crescesse molto, questo è il primo
  punto da rivalutare.
- **Retrofit + kotlinx.serialization** (non Gson): i DTO sono `data class`
  `@Serializable` in `data/remote/dto/`, uno specchio 1:1 dei record C# in
  `Chessora.Contracts/*` nel repository server - se un DTO sembra "strano",
  confrontalo con il record C# corrispondente citato nel commento del file,
  non indovinare la forma del JSON.
- **Le date sono stringhe grezze nei DTO** (il server le serializza come
  ISO-8601 via System.Text.Json): niente parsing nei DTO stessi, solo negli
  helper di `ui/common/DateFormatting.kt`, chiamati nel punto in cui una data
  viene effettivamente mostrata. `java.time` funziona nativamente da minSdk 26
  senza librerie aggiuntive.
- **Nessun endpoint "singolo elemento per id"** per news/tornei: l'Api server
  ha solo endpoint "lista per circolo" (vedi `docs/android-app-spec.md` §6).
  Le schermate di dettaglio (`NewsDetailViewModel`, `TorneoDetailViewModel`)
  richiedono quindi SEMPRE l'intera lista e filtrano lato client per id -
  duplica la chiamata di rete rispetto alla schermata elenco (ogni
  destinazione di navigazione ha il proprio ViewModel, non condiviso), un
  compromesso accettato per non introdurre uno store condiviso cross-schermata
  solo per questo. Se in futuro il server espone un endpoint dedicato, è il
  primo posto da aggiornare.
- **Un solo tema colori app-wide** (oro/antracite, `ui/theme/`), NON i 12 temi
  personalizzabili del sito web: solo nome e logo del circolo si adattano a
  runtime (da `GET /api/site-settings`, vedi `SessionViewModel.branding`) -
  scelta esplicita della specifica (`docs/android-app-spec.md` §4: "non è
  detto serva replicare 1:1 la palette del sito, ma il logo e il nome sì").
- **`SessionViewModel`** (in `ui/session/`) è l'UNICO ViewModel creato a
  livello di `ChessoraNavHost` (non per-schermata): tiene `selectedClubId`
  (da DataStore) e `branding` (da `/api/site-settings`), condivisi da tutto
  il grafo di navigazione. Ogni altra schermata ha il proprio ViewModel
  "normale", con un metodo `load(idClub)` richiamato da un
  `LaunchedEffect(idClub) { viewModel.load(idClub) }` nel composable -
  pattern ripetuto identico in ogni schermata, seguilo per coerenza se ne
  aggiungi una nuova.
- **Notifiche push (FCM)**: `push/ChessoraFirebaseMessagingService.kt` riceve
  i messaggi, `push/DeviceRegistration.kt` registra il token su
  `POST /api/devices/register`. Non esiste un endpoint di "de-registrazione"
  lato server: il toggle "Notifiche" nelle Impostazioni funziona semplicemente
  NON richiamando mai la registrazione quando è spento (vedi il commento in
  `DeviceRegistration.registerCurrentToken`).
- **Nessun deep-link** dalle notifiche push verso una schermata specifica: il
  messaggio è solo titolo+testo libero lato server, toccarlo apre l'app sulla
  Home. Se in futuro serve (vedi `docs/android-app-spec.md` §9 "Domande
  aperte" nel repository server), va aggiunto un campo lato server prima, poi
  gestito in `ChessoraFirebaseMessagingService.onMessageReceived`.
- **Bottom bar a 6 tab, non 9**: le schermate "Direttivo", "Negozio",
  "Impostazioni" sono raggiungibili da un menu unico "Altro"
  (`ui/more/MoreScreen.kt`) invece di occupare altri 3 slot - 9 tab in una
  bottom bar sarebbero illeggibili. Vedi `ui/navigation/ChessoraDestinations.kt`.
- **La locandina di un torneo si apre nel browser di sistema** (Intent
  `ACTION_VIEW`), non in una WebView incorporata - più semplice per questa
  prima versione, la specifica permette entrambe le opzioni.

## Cosa NON è stato fatto (backlog per la prossima sessione)

In ordine indicativo di priorità:

1. **Verificare che il progetto compili** in Android Studio e correggere gli
   inevitabili piccoli errori (vedi "Stato del progetto e limitazioni" sopra).
2. Test: zero scritti finora. Almeno qualche test di `ChessoraRepository`
   (con un `MockWebServer` di OkHttp) e dei ViewModel più semplici sarebbe il
   primo investimento sensato.
3. **Vista calendario a griglia mensile** (oggi è solo una lista raggruppata
   per giorno, `ui/calendar/CalendarScreen.kt`) - la specifica menziona
   entrambe le opzioni come valide, la griglia è più costosa da implementare
   bene ed è stata rimandata.
4. **Cache locale / offline**: oggi ogni schermata rifà sempre la chiamata di
   rete (nessuna persistenza di news/tornei/ecc. - solo `idClub` e la
   preferenza notifiche sono in DataStore). Se serve un minimo di
   funzionamento offline, valuta Room per una cache semplice.
5. **Estrarre in `strings.xml` le stringhe ancora hard-codate nei Composable**
   (es. "Livello $level" in BoardScreen, "Commenti" in NewsDetailScreen,
   le tre voci di MoreScreen) - fatto solo parzialmente in questa prima
   versione, non è stata una priorità rispetto a coprire tutte le 9 schermate.
6. **Selettore di anno per il Direttivo** (`GET /api/board/years` esiste già
   in `ChessoraApi`/`ChessoraRepository`, non ancora usato dalla UI - oggi
   `BoardScreen` mostra sempre l'anno più recente, comportamento di default
   del server quando `year` è omesso).
7. **Font di marchio** ('Playfair Display', usato dal sito web) non replicato
   in app (`ui/theme/Type.kt` usa i font di sistema) - valutare se vale la
   pena scaricare il font via Google Fonts / Downloadable Fonts API.
8. **Icona reale dell'app** e **screenshot per il Play Store** (checklist
   completa in `docs/android-app-spec.md` §8, repository server).
9. **Firma release**: `app/build.gradle.kts` non ha ancora un `signingConfig`
   per `buildTypes.release` - da aggiungere quando si è pronti per il primo
   upload su Play Console (keystore da generare e MAI committare, vedi
   `.gitignore`).
10. Deep-link dalle notifiche push (vedi sopra).

## Struttura del progetto

```
app/src/main/java/org/chessora/app/
├── ChessoraApplication.kt        - DI manuale, crea repository/preferences una volta
├── MainActivity.kt                - unica Activity, richiede permesso notifiche, avvia Compose
├── data/
│   ├── remote/
│   │   ├── ChessoraApi.kt         - interfaccia Retrofit, specchio di android-app-spec.md §6
│   │   ├── NetworkModule.kt       - costruzione Retrofit/OkHttp/Json, resolveAssetUrl()
│   │   └── dto/                   - data class @Serializable, specchio dei record C# server
│   ├── local/ClubPreferences.kt   - DataStore: idClub scelto, toggle notifiche, ultimo token FCM
│   └── repository/ChessoraRepository.kt - unico punto di accesso rete per i ViewModel (Result<T>)
├── push/
│   ├── ChessoraFirebaseMessagingService.kt - riceve notifiche, gestisce onNewToken
│   └── DeviceRegistration.kt      - POST /api/devices/register
└── ui/
    ├── common/                    - UiState<T>, helper ViewModel/date/composable condivisi
    ├── theme/                     - Material3 theme fisso (oro/antracite)
    ├── session/SessionViewModel.kt - idClub + branding, condiviso da tutto il nav graph
    ├── navigation/                - ChessoraNavHost (Scaffold+bottom bar+NavHost), route
    └── onboarding|home|news|calendar|tornei|ranking|board|shop|more|settings/
                                     - una cartella per schermata, ognuna con Screen.kt (+ ViewModel.kt)
```

## Riferimenti

- `docs/android-app-spec.md` nel repository server Chessora: la specifica
  originale, endpoint per endpoint. **Sempre la fonte di verità per capire
  COSA deve fare l'app** - questo README spiega solo COME è stata costruita.
- `Chessora.Contracts/*` nel repository server: la forma esatta di ogni DTO
  (i file in `data/remote/dto/` di questo progetto li rispecchiano 1:1,
  citandoli nei commenti).
- Se un endpoint cambia forma lato server, l'ordine di aggiornamento è:
  `Chessora.Contracts` (server) → `data/remote/dto/*.kt` (qui) →
  `data/remote/ChessoraApi.kt` (qui) → `data/repository/ChessoraRepository.kt`
  (qui) → il/i ViewModel che lo consumano.
