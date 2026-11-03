package org.chessora.app.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.chessora.app.data.remote.dto.MemberClubDto

/**
 * Chi è socio di più circoli nello stesso anno sceglie quale usare nell'app (vedi
 * SessionViewModel.memberClubChoice). Il circolo già selezionato è evidenziato; chiudere
 * il dialogo equivale a tenerlo. Si cambia poi da Impostazioni → "Cambia circolo".
 */
@Composable
fun MemberClubChoiceDialog(
    clubs: List<MemberClubDto>,
    selectedClubCode: String?,
    onChoose: (MemberClubDto) -> Unit,
) {
    val current = clubs.firstOrNull { it.publicCode == selectedClubCode } ?: clubs.first()
    AlertDialog(
        onDismissRequest = { onChoose(current) },
        title = { Text("Di quale circolo vuoi vedere l'app?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Quest'anno risulti socio di più circoli. Potrai cambiarlo quando vuoi da Impostazioni → Cambia circolo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                clubs.forEach { club ->
                    val label = club.name ?: club.publicCode ?: "Circolo ${club.idClub}"
                    if (club == current) {
                        Button(onClick = { onChoose(club) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { onChoose(club) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                }
            }
        },
        confirmButton = {},
    )
}
