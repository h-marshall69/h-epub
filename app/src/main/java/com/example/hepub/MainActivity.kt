package com.example.hepub

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.hepub.theme.HEPUBTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.hepub.data.EpubImporter
import com.example.hepub.data.LibraryRepository
import com.example.hepub.data.local.AppDatabase
import com.example.hepub.ui.library.LibraryScreen
import com.example.hepub.ui.library.LibraryViewModel
import android.content.Intent
import com.example.hepub.data.local.BookEntity

class MainActivity : ComponentActivity() {
  private val openEpub = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
    uri?.let { setContent { HEPUBTheme { renderMain(it) } } }
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      HEPUBTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { renderMain(null) } }
    }
  }

  @androidx.compose.runtime.Composable
  private fun renderMain(uri: Uri?): Unit {
    val context = LocalContext.current
    val repository = remember { LibraryRepository(AppDatabase.get(context).bookDao(), EpubImporter(context.contentResolver, context.filesDir.resolve("books"))) }
    val library: LibraryViewModel = viewModel(factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(repository) as T
    })
    LaunchedEffect(uri) { uri?.let(library::import) }
    LibraryScreen(library.state.collectAsStateWithLifecycle().value, { openEpub.launch(arrayOf("application/epub+zip", "application/epub", "application/octet-stream")) }, library::delete) { book ->
      startActivity(Intent(this, com.example.hepub.epub.ReaderActivity::class.java).putExtra("bookId", book.id))
    }
  }
}
