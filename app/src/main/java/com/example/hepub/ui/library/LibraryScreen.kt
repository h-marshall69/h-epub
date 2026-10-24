package com.example.hepub.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hepub.data.local.BookEntity
import com.example.hepub.ui.translation.TranslationPanel

@Composable
fun LibraryScreen(state: LibraryState, onImport: () -> Unit, onDelete: (BookEntity) -> Unit, onOpen: (BookEntity) -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Biblioteca", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onImport, enabled = state !is LibraryState.Importing) { Text("Importar EPUB") }
        when (state) {
            LibraryState.Loading, LibraryState.Importing -> CircularProgressIndicator()
            is LibraryState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            is LibraryState.Success -> if (state.books.isEmpty()) Text("Aún no hay libros importados") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.books, key = { it.id }) { book -> ListItem(modifier = Modifier.fillMaxWidth(), headlineContent = { TextButton({ onOpen(book) }) { Text(book.title) } }, supportingContent = { Text(book.authors.ifBlank { "Autor desconocido" }) }, trailingContent = { TextButton({ onDelete(book) }) { Text("Eliminar") } }) }
            }
        }
        TranslationPanel()
    }
}
