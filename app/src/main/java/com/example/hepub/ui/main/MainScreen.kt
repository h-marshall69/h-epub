package com.example.hepub.ui.main

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.hepub.data.EpubImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(data: List<String>) {
    Column { data.forEach { Text("Hello $it!") } }
}

@Composable
fun MainScreen(
    uri: Uri? = null,
    onOpen: () -> Unit = {},
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uri) { uri?.let { scope.launch(Dispatchers.IO) {
        result = try { EpubImporter(context.contentResolver, context.filesDir.resolve("books")).import(it).let { book -> "${book.title}\n${book.authors.joinToString()}" } }
        catch (e: Exception) { "Error: ${e.message}" }
    } } }
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("H EPUB", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onOpen) { Text("Importar EPUB") }
        result?.let { Text(it) }
    }
}
