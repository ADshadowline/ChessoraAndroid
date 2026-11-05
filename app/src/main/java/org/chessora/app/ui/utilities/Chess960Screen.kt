package org.chessora.app.ui.utilities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.chessboard.ChessBoardView

/** "Combinazione 960" (Utilità): sceglie una delle 960 posizioni iniziali Fischer Random -
 * a caso, per numero (0..959) o scorrendo con le frecce - e la mostra sulla scacchiera,
 * così si possono disporre i pezzi sulla scacchiera vera. Vedi [Chess960]. */
@Composable
fun Chess960Screen() {
    val context = LocalContext.current
    var number by rememberSaveable { mutableIntStateOf((0 until Chess960.COUNT).random()) }
    var input by rememberSaveable { mutableStateOf(number.toString()) }

    fun select(n: Int) {
        number = ((n % Chess960.COUNT) + Chess960.COUNT) % Chess960.COUNT
        input = number.toString()
    }

    val italian = LocalConfiguration.current.locales[0].language == "it"
    val backRank = if (italian) Chess960.italianBackRank(number) else Chess960.backRank(number).toList().joinToString(" ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.chess960_title), style = MaterialTheme.typography.titleLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            FilledTonalIconButton(onClick = { select(number - 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                stringResource(R.string.chess960_position, number),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            FilledTonalIconButton(onClick = { select(number + 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Text(
            stringResource(R.string.chess960_back_rank, backRank),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        ChessBoardView(fen = Chess960.fen(number), modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { select((0 until Chess960.COUNT).random()) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Casino, contentDescription = null)
                Text(stringResource(R.string.chess960_random), modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = { select(Chess960.CLASSIC) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chess960_standard))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    val digits = v.filter(Char::isDigit).take(3)
                    input = digits
                    digits.toIntOrNull()?.takeIf { it in 0 until Chess960.COUNT }?.let { number = it }
                },
                label = { Text(stringResource(R.string.chess960_number_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = input.toIntOrNull()?.let { it !in 0 until Chess960.COUNT } ?: input.isNotEmpty(),
                modifier = Modifier.width(170.dp),
            )
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("FEN", Chess960.fen(number)))
                    Toast.makeText(context, context.getString(R.string.chess960_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Text(stringResource(R.string.chess960_copy_fen), modifier = Modifier.padding(start = 6.dp))
            }
        }
        Text(
            stringResource(R.string.chess960_rules),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}
