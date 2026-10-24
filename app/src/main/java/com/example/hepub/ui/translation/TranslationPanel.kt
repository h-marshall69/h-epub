package com.example.hepub.ui.translation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hepub.translation.TranslationState
import com.example.hepub.translation.TranslationViewModel

@Composable
fun TranslationPanel(viewModel: TranslationViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Traducción offline (inglés → español)")
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Texto en inglés") })
        when (val current = state) {
            TranslationState.Checking -> CircularProgressIndicator()
            TranslationState.ModelUnavailable -> {
                Text("El modelo no está descargado. Se requiere conexión para prepararlo.")
                Button({ viewModel.downloadModel() }) { Text("Descargar modelo") }
            }
            TranslationState.Downloading -> { CircularProgressIndicator(); Text("Descargando modelo…") }
            TranslationState.Ready -> Button({ viewModel.translate(text) }, enabled = text.isNotBlank()) { Text("Traducir sin conexión") }
            is TranslationState.Translating -> { CircularProgressIndicator(); Text("Traduciendo…") }
            is TranslationState.Completed -> { Text(current.result); Button({ viewModel.translate(text) }) { Text("Traducir de nuevo") } }
            is TranslationState.Error -> { Text(current.message); OutlinedButton({ viewModel.refreshAvailability() }) { Text("Reintentar") } }
        }
    }
}
