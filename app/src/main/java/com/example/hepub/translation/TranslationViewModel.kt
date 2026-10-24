package com.example.hepub.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslationViewModel(private val engine: TranslationEngine = MlKitTranslationEngine()) : ViewModel() {
    private val _state = MutableStateFlow<TranslationState>(TranslationState.Checking)
    val state: StateFlow<TranslationState> = _state.asStateFlow()

    init { refreshAvailability() }

    fun refreshAvailability() = viewModelScope.launch {
        _state.value = TranslationState.Checking
        _state.value = runCatching { if (engine.isModelAvailable()) TranslationState.Ready else TranslationState.ModelUnavailable }
            .getOrElse { TranslationState.Error("No se pudo comprobar el modelo") }
    }

    fun downloadModel() = viewModelScope.launch {
        _state.value = TranslationState.Downloading
        _state.value = runCatching { engine.prepareModel(); TranslationState.Ready }
            .getOrElse { TranslationState.Error("No se pudo descargar el modelo: ${it.message ?: "error desconocido"}") }
    }

    fun translate(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        _state.value = TranslationState.Translating(text)
        _state.value = runCatching { TranslationState.Completed(text, engine.translate(text)) }
            .getOrElse { TranslationState.Error("No se pudo traducir el texto: ${it.message ?: "modelo no disponible"}") }
    }

    override fun onCleared() { engine.close() }
}

sealed interface TranslationState {
    data object Checking : TranslationState
    data object ModelUnavailable : TranslationState
    data object Downloading : TranslationState
    data object Ready : TranslationState
    data class Translating(val text: String) : TranslationState
    data class Completed(val source: String, val result: String) : TranslationState
    data class Error(val message: String) : TranslationState
}
