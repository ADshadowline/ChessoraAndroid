# Screenshot per Play Store

Metti qui gli screenshot presi dall'app reale (telefono o emulatore), poi
avvisami: li rifinisco/ridimensiono secondo le specifiche Play Store.

## Requisiti tecnici (Play Console)

- Minimo **2**, consigliati 4-8.
- PNG o JPEG a 24 bit (no trasparenza).
- Lato più corto: minimo 320 px. Lato più lungo: massimo 3840 px.
- Rapporto lato lungo/lato corto: massimo 2:1 (uno screenshot da telefono
  in verticale "a tutto schermo" rientra tranquillamente).

## Come catturarli

Da un telefono/emulatore con l'app installata (`adb install app-debug.apk`
o direttamente da Android Studio):
- **Fisico**: tasto volume giù + accensione (varia per marca), oppure
  `adb shell screencap -p /sdcard/screen.png` seguito da
  `adb pull /sdcard/screen.png`.
- **Emulatore**: la fotocamera nella barra laterale dei comandi
  dell'emulatore, oppure lo stesso comando adb sopra.

## Schermate consigliate (in ordine di priorità)

1. **Home** — prima impressione, mostra il brand/circolo scelto.
2. **News** (lista o dettaglio) — contenuto informativo tipico.
3. **Tornei** — idealmente uno con locandina visibile.
4. **Classifica** — tabella dati, mostra la profondità dell'app.
5. Calendario, Direttivo o Negozio, se vuoi arrivare a 5-6.

Nessun dato/circolo "finto" da inventare: usa un circolo reale già
presente sulla piattaforma Chessora quando scatti.
