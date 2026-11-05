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
    AppRelease("1.65.0", "novembre 2026", listOf(
        "Sul desktop \"Eventi\" diventa \"Tornei&Eventi\".",
        "L'icona \"Orologio\" diventa \"Utilità\", con una nuova grafica: dentro trovi l'orologio da scacchi e la nuova \"Combinazione 960\".",
        "Nuova grafica per l'icona \"Video\".",
        "Combinazione 960 (Fischer Random): scegli una delle 960 posizioni iniziali a caso, per numero o scorrendo con le frecce, e la vedi sulla scacchiera per disporre i pezzi. Puoi anche copiare il FEN.",
    )),
    AppRelease("1.64.0", "novembre 2026", listOf(
        "Eventi (dal desktop): in coda agli eventi dei circoli compaiono anche i tornei in rete di vesus.org da oggi in avanti, ricercabili dallo stesso campo; un tocco apre la pagina del torneo. Gli eventi dentro \"Il Circolo\" restano solo quelli del circolo.",
    )),
    AppRelease("1.63.0", "novembre 2026", listOf(
        "Partite online: sotto il campo di ricerca una barra a tre colori mostra quante partite hai perso, pattato e vinto (segue ricerca e filtro apertura).",
        "Aperture in ordine alfabetico e raggruppate per variante principale: tocca un'apertura per vedere le sue sottovarianti e scegline una per filtrare le partite.",
    )),
    AppRelease("1.62.3", "novembre 2026", listOf(
        "L'icona Home in alto a sinistra è ora dentro un cerchio con una leggera ombra, più visibile.",
    )),
    AppRelease("1.62.2", "novembre 2026", listOf(
        "Le notifiche hanno il logo Chessora; gli avvisi di aggiornamento e le comunicazioni della piattaforma mostrano anche il logo a colori.",
        "I messaggi di chat arrivano come notifiche ad alta priorità, con banner in alto, suono e vibrazione.",
    )),
    AppRelease("1.62.1", "novembre 2026", listOf(
        "Analisi della partita: ogni mossa ha una categoria (grande, migliore, ottima, buona, imprecisione, errore, mossa mancata, errore grave) e in fondo c'è la legenda con il conto per il Bianco e per il Nero.",
        "Scorrendo le mosse di una partita analizzata, sulla casella d'arrivo compare per un secondo il simbolo della qualità della mossa.",
    )),
    AppRelease("1.62.0", "novembre 2026", listOf(
        "Gestione tornei: a fine torneo, accanto a Premiazione, il pulsante \"Chiudi torneo\" (con conferma).",
        "Gestione tornei: quando i pulsanti in basso sono tre o più, diventano icone per starci tutti.",
        "La classifica Nazionale contiene ora solo giocatori italiani.",
    )),
    AppRelease("1.61.0 – 1.61.1", "novembre 2026", listOf(
        "Analisi computer con Stockfish 17: nelle partite di persona e online un pulsante apre i parametri (posizione o partita intera, profondità o tempo, linee, processori, memoria).",
        "Analisi della posizione: valutazione e linee migliori, aggiornate mentre scorri le mosse. Analisi della partita: grafico della valutazione e imprecisioni, errori e sviste.",
        "Alla prima analisi l'app scarica una sola volta la rete neurale di Stockfish (circa 75 MB).",
        "Il nome del circolo e il tuo nome restano in alto anche nei menu, con l'icona Home per tornare alla Home.",
    )),
    AppRelease("1.60.0", "novembre 2026", listOf(
        "\"I miei progressi\" si chiama ora \"Il mio gioco\" e apre un menu: Andamento Elo, Le mie partite di persona, Le mie partite online.",
        "L'icona \"Le mie partite\" non c'è più in Home: le trovi in Il mio gioco → Le mie partite di persona.",
    )),
    AppRelease("1.59.0 – 1.59.3", "novembre 2026", listOf(
        "Se quest'anno sei socio di più circoli, l'app ti chiede quale usare; lo puoi cambiare quando vuoi da Impostazioni → Cambia circolo.",
        "Classifica del circolo: ci sono tutti i soci, anche chi non ha ancora un Elo (in fondo, in ordine alfabetico).",
        "Classifica: i nomi troppo lunghi si accorciano con i puntini invece di sovrapporsi al punteggio.",
        "Classifiche: accanto al nome la bandiera della nazionalità e la foto del giocatore (la tua, se l'hai caricata, altrimenti quella FIDE).",
    )),
    AppRelease("1.58.2 – 1.58.4", "novembre 2026", listOf(
        "Logo del circolo: i loghi chiari (che sui fondi chiari sparivano) stanno su un riquadro scuro; quelli scuri si vedono grandi, senza cornice. Più largo per i loghi orizzontali.",
        "Lo stesso vale nella schermata di apertura dell'app.",
    )),
    AppRelease("1.58.1", "novembre 2026", listOf(
        "Il logo del circolo si vede intero, su un riquadro chiaro, invece che ritagliato in un cerchio.",
        "Eventi: il pulsante \"Preiscriviti\" non viene più schiacciato quando accanto c'è una nota lunga.",
    )),
    AppRelease("1.58.0", "novembre 2026", listOf(
        "Eventi: per un torneo già iniziato o concluso non c'è più \"Preiscriviti\", ma \"Risultati e classifica\".",
        "Abbinamenti: sotto ogni scacchiera le probabilità di vittoria del Bianco, di patta e di vittoria del Nero, calcolate dall'Elo come su tourn.chessora.org.",
        "Classifica di un torneo in corso: per ogni giocatore la probabilità di vincere il torneo e di salire sul podio.",
        "I turni ora si leggono 1°, 2°, 3°… per far stare più turni nella barra.",
    )),
    AppRelease("1.57.0", "novembre 2026", listOf(
        "Se hai fatto l'accesso e scegli un circolo (anche partendo da \"non sono iscritto a nessun circolo\"), il tuo nome resta nella barra in alto.",
        "Accanto al nome, nella barra in alto, compaiono anche il tuo ID FIDE e la tessera FSI, se li hai.",
    )),
    AppRelease("1.56.0", "ottobre 2026", listOf(
        "Nuove icone illustrate a colori nella Home a icone. Se preferisci quelle semplici, spegni \"Usa grafica\" in Impostazioni.",
        "Impostazioni: il pulsante Salva sotto gli username di Lichess e Chess.com si attiva solo quando scrivi un nome nuovo o diverso.",
        "Gestione tornei: la Premiazione è la stessa di tourn.chessora.org, con i premi assoluti e di fascia inseriti alla creazione del torneo. Compare solo se il torneo ha dei premi.",
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
