package com.example.hepub.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hepub.data.LibraryRepository
import com.example.hepub.data.local.BookEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state: StateFlow<LibraryState> = _state.asStateFlow()
    init { viewModelScope.launch { repository.books.catch { _state.value = LibraryState.Error(it.message ?: "Error de biblioteca") }.collect { _state.value = LibraryState.Success(it) } } }
    fun import(uri: Uri) { viewModelScope.launch { _state.value = LibraryState.Importing; repository.import(uri).onFailure { _state.value = LibraryState.Error(it.message ?: "No se pudo importar") } } }
    fun delete(book: BookEntity) { viewModelScope.launch { repository.delete(book) } }
}

sealed interface LibraryState {
    data object Loading : LibraryState
    data object Importing : LibraryState
    data class Success(val books: List<BookEntity>) : LibraryState
    data class Error(val message: String) : LibraryState
}
