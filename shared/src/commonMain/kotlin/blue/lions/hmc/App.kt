// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScoreEntryScreen()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val scoreInputTransformation = InputTransformation {
    val candidate = asCharSequence()
    if (!isScoreInput(candidate)) {
        revertAllChanges()
    } else if (changes.changeCount > 0 && candidate.contentEquals(originalText)) {
        val selectionAfterEdit = selection
        revertAllChanges()
        selection = selectionAfterEdit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoreEntryScreen() {
    val scoreFields = scoreSeatNames.map { rememberTextFieldState() }
    var players by rememberSaveable {
        mutableStateOf(ArrayList(List<String?>(scoreSeatNames.size) { null }))
    }
    var editedPlayers by rememberSaveable {
        mutableStateOf(ArrayList(List(scoreSeatNames.size) { false }))
    }
    // Restoring the last submitted inputs is enough to reconstruct their confirmation result.
    var confirmedInputs by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    var confirmedPlayers by rememberSaveable { mutableStateOf<ArrayList<String?>>(arrayListOf()) }
    val confirmation = remember(confirmedInputs, confirmedPlayers) {
        confirmedInputs?.let { confirmScores(it, confirmedPlayers) } ?: ScoreConfirmation()
    }
    val visibleConfirmation = confirmation.afterEdits(
        editedScores = scoreFields.map { it.undoState.canUndo || it.undoState.canRedo },
        editedPlayers = editedPlayers,
    )
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("半荘の素点", style = MaterialTheme.typography.headlineSmall)
        Text("点単位で入力してください（例 35000、-1100）。")

        scoreFields.forEachIndexed { index, field ->
            val error = visibleConfirmation.fieldErrors[index]
            Column {
                Text(scoreSeatNames[index], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerSelector(
                        player = players[index],
                        error = visibleConfirmation.playerErrors[index],
                        modifier = Modifier.weight(1f),
                        onSelect = { player ->
                            val updatedPlayers = assignPlayer(players, index, player)
                            if (updatedPlayers != players) {
                                editedPlayers = players.mapIndexedTo(ArrayList()) { seat, previousPlayer ->
                                    editedPlayers[seat] || previousPlayer != updatedPlayers[seat]
                                }
                                players = ArrayList(updatedPlayers)
                            }
                        },
                    )
                    OutlinedTextField(
                        state = field,
                        modifier = Modifier.weight(1f),
                        label = { Text("素点（点）") },
                        supportingText = if (error == null) null else {
                            { Text(error) }
                        },
                        isError = error != null,
                        inputTransformation = scoreInputTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }
            }
        }

        Button(onClick = {
            focusManager.clearFocus()
            confirmedInputs = scoreFields.mapTo(ArrayList()) { it.text.toString() }
            confirmedPlayers = players
            editedPlayers = ArrayList(List(scoreSeatNames.size) { false })
            scoreFields.forEach { it.undoState.clearHistory() }
        }) {
            Text("成績を確認")
        }

        visibleConfirmation.calculationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        if (visibleConfirmation.rows.isNotEmpty()) {
            Text("半荘の成績", style = MaterialTheme.typography.titleLarge)
            visibleConfirmation.rows.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${scoreSeatNames[index]} ${result.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${result.score} 点")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${result.place}位")
                        Text(result.points, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSelector(
    player: String?,
    error: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(player?.let(scorePlayerNames::getValue) ?: "選手を選ぶ")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                scorePlayerNames.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        },
                    )
                }
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
