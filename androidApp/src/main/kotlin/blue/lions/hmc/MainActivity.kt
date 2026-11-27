// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/androidApp/src/main/kotlin/com/jetbrains/kmpapp/MainActivity.kt
// Modified for hmc (https://github.com/7pairs/hmc).

package blue.lions.hmc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HmcApplication).container
        val draft = savedStateRegistry.consumeRestoredStateForKey(CHRONICLE_TASK_STATE)
        val model = ViewModelProvider.create(this, factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EntryViewModel(container.restoreAndroidTaskState(draft)) as T
        })[EntryViewModel::class]
        savedStateRegistry.registerSavedStateProvider(CHRONICLE_TASK_STATE, model.state::saveAndroidTaskState)
        setContent {
            App(model.state, model.viewModelScope)
        }
    }
}

private const val CHRONICLE_TASK_STATE = "chronicle.task"

private class EntryViewModel(val state: ChronicleState) : ViewModel()
