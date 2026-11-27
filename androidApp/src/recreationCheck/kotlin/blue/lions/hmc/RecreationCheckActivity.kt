// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlin.uuid.Uuid

class RecreationCheckActivity : ComponentActivity() {
    private val hostId = Uuid.random().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider.create(this)[RecreationCheckViewModel::class]
        setContent {
            model.check.Content(model.viewModelScope, hostId, ::recreate)
        }
    }
}

class RecreationCheckViewModel(application: Application) : AndroidViewModel(application) {
    val check = createAndroidSaveRecreationCheck(application)
}
