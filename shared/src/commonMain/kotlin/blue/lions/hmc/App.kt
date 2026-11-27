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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun App(state: ChronicleState, scope: CoroutineScope) {
    LaunchedEffect(state) { scope.launch { state.loadPlayers() } }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state.screen) {
                Screen.LOADING -> Box(Modifier.safeDrawingPadding().padding(16.dp)) {
                    CircularProgressIndicator()
                }
                Screen.LOAD_FAILED -> Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                    Text("選手を読み込めませんでした。")
                    Button(onClick = { scope.launch { state.loadPlayers() } }) {
                        Text("読み込み直す")
                    }
                }
                Screen.PLAYERS -> PlayerRegistrationScreen(state, scope)
                Screen.SCORES -> ScoreEntryScreen(state, scope)
            }
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
private fun ScoreEntryScreen(state: ChronicleState, scope: CoroutineScope) {
    val visibleConfirmation = state.visibleConfirmation
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
        OutlinedButton(enabled = state.canEdit, onClick = state::showPlayers) {
            Text("選手を登録・変更")
        }

        state.fields.forEachIndexed { index, field ->
            val error = visibleConfirmation.fieldErrors[index]
            Column {
                Text(scoreSeatNames[index], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerSelector(
                        player = state.seats[index],
                        candidates = state.players,
                        enabled = state.canEdit,
                        error = visibleConfirmation.playerErrors[index],
                        modifier = Modifier.weight(1f),
                        onSelect = { player -> state.select(index, player) },
                    )
                    OutlinedTextField(
                        state = field,
                        enabled = state.canEdit,
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

        Button(enabled = state.canEdit, onClick = {
            focusManager.clearFocus()
            scope.launch { state.confirm() }
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
        Button(enabled = state.canSave, onClick = {
            focusManager.clearFocus()
            scope.launch { state.save() }
        }) {
            Text("この半荘を保存")
        }
        when (state.saveState) {
            SaveState.IDLE -> Unit
            SaveState.WORKING -> Text("保存中")
            SaveState.SAVED -> if (state.seats.all { it == null } && state.fields.all { it.text.isEmpty() }) {
                Text("保存しました")
            }
            SaveState.FAILED -> Text("保存できませんでした。もう一度お試しください。")
            SaveState.UNKNOWN -> {
                Text("保存結果を確認できませんでした。")
                OutlinedButton(onClick = { scope.launch { state.reconcile() } }) {
                    Text("保存結果を確認")
                }
            }
            SaveState.CHECKING -> Text("保存結果を確認中")
            SaveState.CONFLICT -> Text(
                "同じ半荘IDに別の内容が保存されています。",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PlayerSelector(
    player: String?,
    candidates: List<Player>,
    enabled: Boolean,
    error: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Box {
            OutlinedButton(enabled = enabled, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(candidates.firstOrNull { it.id == player }?.name ?: "選手を選ぶ")
            }
            DropdownMenu(expanded = enabled && expanded, onDismissRequest = { expanded = false }) {
                candidates.forEach { (id, name) ->
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

@Composable
private fun PlayerRegistrationScreen(state: ChronicleState, scope: CoroutineScope) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val nameResult = state.nameResult
    val saveVersion = state.playerSaveVersion
    LaunchedEffect(saveVersion) {
        // Keep success available so a recreated form can also clear its restored input.
        if (state.playerSaveVersion == saveVersion && state.nameResult == NameResult.SAVED) {
            editingId = null
            name = ""
        }
    }
    val nameError = when (nameResult) {
        NameResult.EMPTY -> "表示名を入力してください。"
        NameResult.DUPLICATE -> "その表示名は登録済みです。"
        NameResult.FAILED -> "表示名を保存できませんでした。もう一度お試しください。"
        else -> null
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("選手登録", style = MaterialTheme.typography.headlineSmall)
        state.players.forEach { player ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(player.name, modifier = Modifier.weight(1f))
                OutlinedButton(enabled = !state.writingPlayer, onClick = {
                    editingId = player.id
                    name = player.name
                    state.clearNameResult()
                }) {
                    Text("名前を変更")
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                state.clearNameResult()
            },
            label = { Text("表示名") },
            supportingText = if (nameError == null) null else { { Text(nameError) } },
            isError = nameError != null,
            enabled = !state.writingPlayer,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = !state.writingPlayer, onClick = {
                val playerId = editingId
                val displayName = name
                focusManager.clearFocus()
                scope.launch { state.writePlayer(playerId, displayName) }
            }) {
                Text(if (editingId == null) "登録する" else "変更を保存")
            }
            if (editingId != null) {
                OutlinedButton(enabled = !state.writingPlayer, onClick = {
                    editingId = null
                    name = ""
                    state.clearNameResult()
                }) {
                    Text("キャンセル")
                }
            }
        }
        Button(enabled = state.players.size >= Seat.entries.size && !state.writingPlayer, onClick = state::showScores) {
            Text("素点入力へ")
        }
    }
}
