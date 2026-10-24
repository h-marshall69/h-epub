package com.example.hepub.epub

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.hepub.data.local.AppDatabase
import com.example.hepub.theme.HEPUBTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import android.util.Log
import org.json.JSONObject
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

class ReaderActivity : FragmentActivity(), ReaderFragment.Host {
    private lateinit var navigatorContainer: FrameLayout
    private var navigator: EpubNavigatorFragment? = null
    private var publication: org.readium.r2.shared.publication.Publication? = null
    private var current by mutableStateOf<Locator?>(null)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = FrameLayout(this)
        navigatorContainer = FrameLayout(this).apply { id = View.generateViewId() }
        root.addView(navigatorContainer, FrameLayout.LayoutParams(-1, -1))
        val controls = ComposeView(this)
        root.addView(controls, FrameLayout.LayoutParams(-1, -2, android.view.Gravity.BOTTOM))
        setContentView(root)
        controls.setContent { HEPUBTheme { ReaderControls() } }
        openBook()
    }

    private fun openBook() {
        lifecycleScope.launch {
            val id = intent.getStringExtra("bookId")
            if (id.isNullOrBlank()) { failOpening("No se indicó el libro"); return@launch }
            val book = runCatching {
                withContext(Dispatchers.IO) { AppDatabase.get(this@ReaderActivity).bookDao().findById(id) }
            }.getOrElse { failOpening("No se pudo consultar la biblioteca"); return@launch }
            if (book == null) { failOpening("El libro ya no está disponible en la biblioteca"); return@launch }
            if (!java.io.File(book.privatePath).isFile) { failOpening("La copia privada del libro no está disponible"); return@launch }
            ReadiumPublicationOpener(this@ReaderActivity).open(book).onSuccess { opened ->
                publication = opened
                Log.i("HEPUB-Readium", "readingOrder=${opened.readingOrder.size}, resources=${opened.resources.size}, toc=${opened.tableOfContents.size}")
                opened.readingOrder.forEachIndexed { index, link ->
                    Log.i("HEPUB-Readium", "readingOrder[$index] href=${link.href} mime=${link.mediaType}")
                }
                ReaderFragment.publication = opened
                ReaderFragment.bookId = book.id
                ReaderFragment.initialLocator = book.locatorJson?.let { json ->
                    runCatching { Locator.fromJSON(JSONObject(json), null) }.getOrNull()
                        ?.takeIf { locator -> opened.readingOrder.any { link -> link.href.toString() == locator.href.toString() } }
                }
                supportFragmentManager.beginTransaction().replace(navigatorContainer.id, ReaderFragment()).commit()
            }.onFailure { failOpening("No se pudo abrir el EPUB") }
        }
    }

    private fun failOpening(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onNavigatorReady(navigator: EpubNavigatorFragment) { this.navigator = navigator }
    override fun onLocator(locator: Locator) { current = locator }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun ReaderControls() {
        var visible by remember { mutableStateOf(true) }
        var tocOpen by remember { mutableStateOf(false) }
        Box(Modifier.wrapContentSize()) {
            if (visible) {
                Column(Modifier.wrapContentSize(), verticalArrangement = Arrangement.Bottom) {
                    val progression = current?.locations?.totalProgression
                    if (progression != null) {
                        LinearProgressIndicator({ progression.toFloat() }, Modifier.fillMaxWidth())
                        Text("${(progression * 100).toInt()}%", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface)
                    } else Text("Progreso no disponible", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button({ (navigator as? OverflowableNavigator)?.goBackward() }) { Text("Anterior") }
                        Button({ tocOpen = true }, enabled = !publication?.tableOfContents.isNullOrEmpty()) { Text("Índice") }
                        Button({ (navigator as? OverflowableNavigator)?.goForward() }) { Text("Siguiente") }
                        OutlinedButton({ visible = false }) { Text("Ocultar") }
                    }
                }
            } else {
                Button({ visible = true }, Modifier.padding(16.dp)) { Text("Mostrar controles") }
            }
            if (tocOpen) {
                ModalBottomSheet(onDismissRequest = { tocOpen = false }) {
                    Text("Índice", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                    val links = remember(publication) { publication?.tableOfContents.orEmpty().flattenLinks() }
                    if (links.isEmpty()) Text("Este libro no tiene índice", Modifier.padding(16.dp))
                    LazyColumn { items(links) { (level, link) ->
                        TextButton({ navigator?.go(link); tocOpen = false }, Modifier.padding(start = (level * 16).dp).fillMaxWidth()) { Text(link.title?.ifBlank { "Sin título" } ?: "Sin título") }
                    } }
                }
            }
        }
    }
}

private fun List<Link>.flattenLinks(level: Int = 0): List<Pair<Int, Link>> = flatMap { link -> listOf(level to link) + link.children.flattenLinks(level + 1) }
