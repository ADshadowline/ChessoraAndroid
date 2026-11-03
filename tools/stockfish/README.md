# Stockfish nell'app

L'analisi computer usa **Stockfish 17.1**, software libero rilasciato con licenza
**GNU GPL v3** (https://www.gnu.org/licenses/gpl-3.0.html). L'app lo esegue come
processo separato e comunica con lui tramite il protocollo UCI.

- **Sorgenti**: https://github.com/official-stockfish/Stockfish/tree/sf_17.1, non modificati.
- **Compilazione**: `build-android.ps1` (NDK r27, `-DNNUE_EMBEDDING_OFF`). Produce
  `app/src/main/jniLibs/<abi>/libstockfish.so` per arm64-v8a, armeabi-v7a e x86_64.
- **Reti NNUE** (`nn-1c0000000000.nnue`, `nn-37f18f62d772.nnue`): sono quelle ufficiali di
  https://tests.stockfishchess.org/api/nn/. Si pubblicano su
  `chessora.org/downloads/engine/<nome>.bin` e l'app le scarica al primo uso
  (vedi `engine/EngineNetworks.kt`).

Per aggiornare Stockfish: scarica i sorgenti del nuovo tag, aggiorna i percorsi nello
script e i nomi delle reti (`EvalFileDefaultNameBig/Small` in `src/evaluate.h`), poi
ricompila.
