# Pubblicazione su Google Play

Materiale pronto da copiare nella Play Console per l'app **Chessora** (`org.chessora.app`).

## Build

| Canale | Comando | File |
|---|---|---|
| Google Play (`play`) | `gradlew bundlePlayRelease` | `app/build/outputs/bundle/playRelease/app-play-release.aab` |
| Download dal sito (`sideload`) | `gradlew assembleSideloadRelease` | `app/build/outputs/apk/sideload/release/app-sideload-release.apk` |

- Il flavor `play` non controlla `version.json`: gli aggiornamenti li gestisce lo store, perché le norme di Google Play vietano aggiornamenti da altre fonti.
- Entrambi sono firmati con la stessa chiave:
  - SHA-1 `E5:CD:8D:10:EB:8F:FF:F2:00:6E:35:A9:24:09:3B:86:E6:7A:DA:EA`
  - SHA-256 `90:96:2C:B7:35:2B:CA:A8:60:B2:08:DC:B6:DA:2E:E1:A3:6F:6E:3A:13:D0:4A:FC:BC:55:6E:47:7D:61:D1:FD`
- A ogni release `versionCode` va aumentato, e Play rifiuta un versionCode già caricato.
- Dal 13/10/2026 Play richiede **targetSdk 36** (Android 16): progetto su compileSdk/targetSdk 36, Android Gradle Plugin 8.10.1 e Gradle 8.11.1. La 1.50.0 (102), con target 35, era stata rifiutata.

## Stato attuale della firma (13 ottobre 2026)

**Chiave di firma dell'app.** È generata da Google il 12/10/2026, ibrida classica + post-quantistica.
- SHA-1 classico: `B5:10:49:C9:36:FB:69:1D:F7:55:3D:1D:F3:AE:47:1B:6C:49:79:E1`.
- È già registrato in Firebase, progetto `chessora-app`, per "Accedi con Google".
- In Firebase ci sono anche `11:28:8D:…:B0:8C` (copiato dalla Console, da verificare) ed `E5:CD:8D:…:DA:EA` (APK del sito).

**Chiave di caricamento.** È la nostra `release.keystore` (SHA-1 `E5:CD:8D:…:DA:EA`), registrata dal primo AAB accettato, la 1.50.0 (102), il 13/10/2026.
- Il primo tentativo del 12/10 era stato rifiutato con l'errore "chiave `CB:F4:30:…`", ma non è servito chiedere una reimpostazione.
- `docs/play-store/upload_certificate.pem` è il certificato di questa chiave.

**Conseguenza:** l'APK del sito e l'app del Play Store hanno chiavi di firma diverse. Chi passa dal sito allo store deve disinstallare l'app una volta.

## Firma dell'app (primo caricamento)

Alla prima release la Console chiede come gestire la chiave di firma. Scegli **"Usa una chiave diversa" → "Esporta e carica una chiave da un archivio Java"**, scarica la chiave di crittografia proposta (`encryption_public_key.pem`) ed esporta con PEPK la chiave attuale (`keystore.properties`).

Due motivi per usare la stessa chiave:
- chi ha l'APK del sito può aggiornarlo dallo store senza disinstallarlo;
- lo SHA-1 resta quello già registrato per "Accedi con Google".

Se si sceglie invece una chiave nuova generata da Google, il suo SHA-1 (Integrità app → Firma app) va aggiunto al client OAuth Android nella Google Cloud Console e in Firebase.

## Scheda dello store

- **Nome app** (30): `Chessora - Circoli di scacchi`
- **Descrizione breve** (80): `Il tuo circolo di scacchi in tasca: tornei, abbinamenti, classifiche e Elo FIDE`
- **Categoria**: Sport · **Email di contatto**: postmaster@chessora.org · **Sito web**: https://chessora.org
- **Norme sulla privacy**: https://chessora.org/privacy.html

**Descrizione completa:**

```
Chessora è l'app dei circoli di scacchi: tutto quello che succede nel tuo circolo, sempre con te.

TORNEI
• Calendario dei tornei del tuo circolo e di tutta Italia
• Preiscrizione ai tornei con un tocco
• Abbinamenti, risultati e classifica aggiornati in tempo reale durante il torneo
• Notifica quando viene pubblicato un nuovo turno

IL TUO PROFILO DI GIOCATORE
• Collegamento al tuo ID FIDE e alla tessera FSI
• Andamento del tuo Elo Standard, Rapid e Blitz nel tempo
• "Le mie partite": registra le tue partite (anche componendole sulla scacchiera) e rivedile mossa per mossa
• Collegamento ai tuoi account Lichess e Chess.com

IL TUO CIRCOLO
• News, video e direttivo del circolo
• Classifica dei soci
• Messaggi con gli altri soci e con il circolo
• Mappa dei circoli italiani

Accedi con Google oppure con email e password. Se sei socio di un circolo che usa Chessora ritrovi subito i tuoi dati; se non lo sei puoi usare l'app come giocatore della piattaforma.
```

**Grafica:**
- icona 512×512 PNG;
- immagine in evidenza 1024×500;
- almeno 2 screenshot del telefono, consigliati 4–8, con lato minimo di 1080 px.

## Contenuti dell'app (sezione "Configura la tua app")

| Voce | Risposta |
|---|---|
| Norme sulla privacy | https://chessora.org/privacy.html |
| Annunci | No, l'app non contiene annunci |
| Accesso all'app | Alcune funzionalità sono limitate: fornire email e password dell'utente di prova per i revisori |
| Classificazione dei contenuti | Categoria "Tutte le altre tipologie di app". Nessun contenuto violento, sessuale, linguaggio volgare, droghe o gioco d'azzardo. **Sì** alla domanda sull'interazione tra utenti (messaggi tra soci). Nessun acquisto digitale. |
| Pubblico di destinazione | 13–15, 16–17, 18+ (non includere sotto i 13: farebbe scattare le norme "Famiglie") |
| App di notizie | No |
| App governativa / finanziaria / sanitaria | No |
| Eliminazione dell'account | URL: https://chessora.org/elimina-account.html. Nell'app: Impostazioni → Elimina il mio account |

## Sicurezza dei dati

**Domande generali:**
- I dati sono raccolti o condivisi? **Sì**, solo raccolti.
- Crittografati in transito? **Sì**, HTTPS.
- L'utente può chiedere la cancellazione? **Sì**.

Firebase (notifiche, Crashlytics) è un fornitore di servizi che tratta i dati per conto dell'app: secondo le definizioni di Google questo **non** è "condivisione".

| Tipo di dati | Raccolto | Obbligatorio | Scopi |
|---|---|---|---|
| Informazioni personali → Nome | Sì | Sì | Funzionalità dell'app, Gestione account |
| Informazioni personali → Indirizzo email | Sì | Sì | Funzionalità dell'app, Gestione account |
| Informazioni personali → ID utente (account, ID FIDE) | Sì | Sì | Funzionalità dell'app, Gestione account |
| Foto e video → Foto | Sì | No (facoltativa) | Funzionalità dell'app (foto profilo) |
| Messaggi → Altri messaggi in-app | Sì | No | Funzionalità dell'app |
| Attività nelle app → Altri contenuti generati dagli utenti (partite) | Sì | No | Funzionalità dell'app |
| Informazioni e prestazioni app → Log degli arresti anomali | Sì | Sì | Analisi (stabilità) |
| Informazioni e prestazioni app → Diagnostica | Sì | Sì | Analisi (stabilità) |
| ID dispositivo o altri ID (token notifiche) | Sì | Sì | Funzionalità dell'app (notifiche) |

**Non raccolti:**
- posizione;
- contatti;
- dati finanziari;
- salute;
- file e documenti;
- calendario;
- cronologia web.
## Test chiuso (account personale creato dopo novembre 2023)

1. **Test → Test chiuso → Crea traccia.** Aggiungi almeno **12 tester** (elenco di email Gmail o un Google Group) e carica l'AAB.
2. Invia la release in revisione e condividi con i tester il link di partecipazione: devono accettare l'invito e installare l'app dal Play Store.
3. Servono **14 giorni consecutivi** con almeno 12 tester iscritti, poi in **Dashboard → Richiedi l'accesso alla produzione** si risponde al questionario sul test.
4. Dopo l'approvazione: **Produzione → Crea release**, con lo stesso AAB o uno più recente.
