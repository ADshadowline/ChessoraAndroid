package org.chessora.app.ui.help

/** Una release dell'app con le sue novità/correzioni, in ordine dalla più recente. */
data class AppRelease(val version: String, val date: String, val changes: List<String>)

/**
 * Novità per versione mostrate in Aiuto → "Novità e correzioni" (vedi HelpScreens.kt).
 * Ricostruite dalla storia git del progetto (versionName in app/build.gradle.kts) e scritte
 * per l'utente, non per chi sviluppa. Solo in italiano: sono note di rilascio, non testi
 * dell'interfaccia.
 *
 * A ogni nuova release: aggiungere una voce IN CIMA con lo stesso versionName di
 * build.gradle.kts.
 */
val APP_CHANGELOG: List<AppRelease> = listOf(
    AppRelease("1.56.0", "ottobre 2026", listOf(
        "Impostazioni → \"Usa grafica\": la Home a icone mostra icone illustrate a colori al posto di quelle semplici.",
        "Impostazioni: il pulsante Salva sotto gli username di Lichess e Chess.com si attiva solo quando scrivi un nome nuovo o diverso.",
    )),
    AppRelease("1.55.0", "ottobre 2026", listOf(
        "Nuova icona \"Orologio\": scegli tempo e incremento (o una cadenza rapida come 3+2 o 15+10) e il telefono diventa un orologio da scacchi.",
        "Lo schermo si divide in due: appoggia il telefono accanto alla scacchiera, ognuno vede il proprio tempo dal suo lato e tocca la sua metà dopo aver mosso.",
        "Pausa, ricomincia e cambio cadenza dalla barra al centro; negli ultimi 10 secondi compaiono i decimi e a tempo scaduto la metà diventa rossa.",
    )),
    AppRelease("1.54.0", "ottobre 2026", listOf(
        "Le mie partite: tieni premuta una partita per selezionarla (anche più di una, o tutte).",
        "Dalle partite selezionate puoi creare un PDF con intestazione, risultato, posizione finale e mosse, da salvare, inviare o stampare.",
    )),
    AppRelease("1.53.0", "ottobre 2026", listOf(
        "Messaggi: puoi inviare una foto, dalla galleria o scattandola al momento.",
        "Messaggi: puoi inviare un messaggio vocale (fino a 2 minuti) toccando il microfono.",
        "Foto e vocali li vede solo chi partecipa alla chat; tocca una foto per vederla a tutto schermo.",
    )),
    AppRelease("1.52.0", "ottobre 2026", listOf(
        "Se all'apertura il server non risponde (per esempio durante un aggiornamento), l'app lo dice chiaramente e ti fa riprovare.",
        "Direttivo: niente più scorrimento orizzontale, chi non ci sta va a capo; nomi centrati e mai troncati.",
        "Direttivo: ogni livello ha una striscia di colore diverso, a colori alternati.",
        "L'icona \"?\" diventa \"Info\", con una nuova grafica; dopo ogni aggiornamento un riflesso di luce la fa notare finché non la apri.",
    )),
    AppRelease("1.51.0", "ottobre 2026", listOf(
        "Nuova icona \"?\" in Home: cosa puoi fare con l'app e le novità di ogni versione.",
        "Messaggi: tieni premuta una chat per selezionarla ed eliminarla dal tuo elenco (anche più chat insieme).",
        "Messaggi: dal menu ⋮ di una chat puoi bloccare o sbloccare un giocatore. Con un blocco attivo nessuno dei due può più scrivere e chi ti ha bloccato non ti trova nella ricerca.",
        "La lingua ora si sceglie da Impostazioni (non più dalla bandierina in Home).",
        "L'elenco delle lingue ora scorre: prima l'ultima voce restava tagliata.",
    )),
    AppRelease("1.50.1", "13 ottobre 2026", listOf(
        "Aggiornamento tecnico richiesto da Google Play (Android 16).",
    )),
    AppRelease("1.50.0", "12 ottobre 2026", listOf(
        "Prima versione pubblicata su Google Play.",
        "Impostazioni: \"Elimina il mio account\" per cancellare account e dati.",
    )),
    AppRelease("1.49.0", "12 ottobre 2026", listOf(
        "Profilo: nome e cognome obbligatori per chi non ha un ID FIDE e non è socio di un circolo.",
    )),
    AppRelease("1.48.0", "11 ottobre 2026", listOf(
        "Le mie partite: colore dell'avversario, modifica e cancellazione di una partita.",
    )),
    AppRelease("1.47.1", "11 ottobre 2026", listOf(
        "Posizione del circolo: la mappa si apre sulle coordinate esatte.",
    )),
    AppRelease("1.47.0", "11 ottobre 2026", listOf(
        "Accesso: aggiunto \"Crea nuovo utente\" con email e password; tolto l'accesso con ID FIDE.",
    )),
    AppRelease("1.46.1", "11 ottobre 2026", listOf(
        "Utente della piattaforma: registrazione immediata con i dati del tuo account Google.",
    )),
    AppRelease("1.46.0", "11 ottobre 2026", listOf(
        "Dopo l'accesso con Google scegli chi sei: giocatore con ID FIDE, socio di un circolo o utente della piattaforma.",
    )),
    AppRelease("1.45.3", "11 ottobre 2026", listOf(
        "Le mie partite: data della partita e tempo di riflessione.",
    )),
    AppRelease("1.45.2", "8 ottobre 2026", listOf(
        "\"Componi sulla scacchiera\": la notazione PGN si vede mentre muovi i pezzi.",
    )),
    AppRelease("1.45.1", "7 ottobre 2026", listOf(
        "Corretto \"Componi sulla scacchiera\": i tocchi non spostavano i pezzi.",
    )),
    AppRelease("1.45.0", "6 ottobre 2026", listOf(
        "Nuova sezione \"Le mie partite\": incolla un PGN o componi la partita sulla scacchiera.",
        "\"Le mie performance\" si chiama ora \"I miei progressi\".",
    )),
    AppRelease("1.44.1", "6 ottobre 2026", listOf(
        "Aperture raggruppate per famiglia invece che per singola variante.",
    )),
    AppRelease("1.44.0", "6 ottobre 2026", listOf(
        "Scacchi Online: statistiche per apertura (vinte/patte/perse), ricerca per avversario e filtro per apertura.",
    )),
    AppRelease("1.43.0", "6 ottobre 2026", listOf(
        "Scacchiera a tutta larghezza con coordinate; mosse PGN toccabili e sincronizzate con la posizione.",
    )),
    AppRelease("1.42.1", "6 ottobre 2026", listOf(
        "Uscendo dall'account continui a ricevere i messaggi della chat.",
    )),
    AppRelease("1.42.0", "2 ottobre 2026", listOf(
        "Pulsante per girare la scacchiera quando rivedi una partita.",
    )),
    AppRelease("1.41.0", "1 ottobre 2026", listOf(
        "Corretto un crash aprendo le partite di Chess.com.",
        "Le News tornano in un'unica sezione.",
    )),
    AppRelease("1.40.0", "1 ottobre 2026", listOf(
        "News divise tra \"La Rete\" e \"Il Circolo\"; il Negozio è nascosto se non ha articoli.",
        "Segnalazione automatica dei crash per correggerli più in fretta.",
    )),
    AppRelease("1.39.0", "1 ottobre 2026", listOf(
        "Scacchi Online: anche le partite di Chess.com, in un unico elenco con Lichess.",
        "PGN visibile sotto la scacchiera.",
    )),
    AppRelease("1.38.0", "30 settembre 2026", listOf(
        "Eventi, Calendario, Video, News e Classifica: in Home quelli di tutti i circoli, in \"Il Circolo\" solo quelli del tuo.",
    )),
    AppRelease("1.37.4", "30 settembre 2026", listOf(
        "Scacchiera di Scacchi Online con i pezzi disegnati in modo realistico.",
    )),
    AppRelease("1.37.0 – 1.37.3", "28 settembre 2026", listOf(
        "Nuova sezione \"Il Circolo\" con il Direttivo.",
        "Logo del circolo più grande e badge utente a tutta larghezza.",
        "Corretti l'apertura delle partite Lichess e un crash all'avvio.",
    )),
    AppRelease("1.36.0", "27 settembre 2026", listOf(
        "Scacchi Online: le tue partite Lichess dentro \"I miei progressi\".",
        "Video: data di pubblicazione e numero di video.",
    )),
    AppRelease("1.35.0", "27 settembre 2026", listOf(
        "Sezione Video ridisegnata; \"Gestione tornei\" compare solo se hai tornei da gestire.",
    )),
    AppRelease("1.34.0", "26 settembre 2026", listOf(
        "Non ci si può più iscrivere a un torneo già iniziato.",
    )),
    AppRelease("1.33.0 – 1.33.1", "26 settembre 2026", listOf(
        "Premiazione a fine torneo, uguale tra app e sito.",
        "Elenco iscritti visibile prima del primo turno.",
    )),
    AppRelease("1.32.0 – 1.32.1", "25-26 settembre 2026", listOf(
        "Gestione tornei: classifica provvisoria, solo a turno completato.",
    )),
    AppRelease("1.31.0 – 1.31.2", "25 settembre 2026", listOf(
        "Turni e risultati aggiornati quasi in tempo reale.",
        "Avviso animato a tutto schermo quando viene pubblicato un turno.",
    )),
    AppRelease("1.30.0", "25 settembre 2026", listOf(
        "Notifica a tutto schermo quando esce un nuovo turno.",
    )),
    AppRelease("1.29.0", "24 settembre 2026", listOf(
        "Gestione tornei per gli organizzatori: genera e pubblica i turni, inserisci i risultati.",
    )),
    AppRelease("1.28.0", "24 settembre 2026", listOf(
        "\"I miei tornei\" con pallino \"in corso\", abbinamenti e classifica.",
    )),
    AppRelease("1.27.0", "23 settembre 2026", listOf(
        "Preiscrizione possibile solo nel periodo di iscrizione del torneo.",
    )),
    AppRelease("1.25.0 – 1.26.0", "23 settembre 2026", listOf(
        "App disponibile in inglese, spagnolo, francese, tedesco, russo, rumeno e turco.",
        "Selettore della lingua con le bandierine.",
    )),
    AppRelease("1.0 – 1.24", "settembre 2026", listOf(
        "Scelta del circolo, Home con icone personalizzabili e sfondo, calendario mensile, Direttivo.",
        "Accesso obbligatorio con Google o email; ricerca FIDE per nome.",
        "Preiscrizione ai tornei, ritiro, elenco iscritti e bando.",
        "Messaggi tra giocatori e con i circoli, con doppia spunta di consegna/lettura.",
        "Andamento Elo, sezione Video e notifiche push.",
    )),
)
