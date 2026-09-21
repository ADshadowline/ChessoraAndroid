package org.chessora.app.ui.theme

import androidx.compose.ui.graphics.Color

// Palette Chessora "di marchio", usata per tutta l'app indipendentemente dal
// circolo scelto - solo nome e logo cambiano per circolo (vedi
// docs/android-app-spec.md §4: "non è detto serva replicare 1:1 la palette
// del sito, ma il logo e il nome sì"). Riprodurre l'intero sistema di temi
// personalizzabili del sito web (12 combinazioni tema/colore) in app è un
// lavoro sostanzialmente più grande, volutamente fuori da questa prima
// versione - vedi README.md "Cosa NON è stato fatto".
val ChessoraGold = Color(0xFFE5BE34)
val ChessoraGold2 = Color(0xFFC8A24A)
val ChessoraInk = Color(0xFF1F1A17)
val ChessoraInk2 = Color(0xFF2A231E)
val ChessoraCream = Color(0xFFF7F3EC)
val ChessoraError = Color(0xFFB3261E)
// Sfondo bolla "miei messaggi" in Messaggistica (chiaro, testo scuro sopra leggibile
// come col precedente ChessoraGold) e sfondo dei badge numerici (Messaggi non letti,
// Iscrizioni ai tornei) - più saturo, pensato per il testo bianco del badge.
val ChessoraSkyBlue = Color(0xFF90CAF9)
val ChessoraSkyBlueBadge = Color(0xFF42A5F5)
