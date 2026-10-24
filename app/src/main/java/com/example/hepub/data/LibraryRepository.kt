package com.example.hepub.data

import com.example.hepub.data.local.BookDao
import com.example.hepub.data.local.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class LibraryRepository(private val dao: BookDao, private val importer: EpubImporter) {
    val books: Flow<List<BookEntity>> = dao.observeAll()

    suspend fun import(uri: android.net.Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = importer.import(uri)
            val path = importer.lastImportedFile ?: error("No se encontró la copia privada")
            val id = importer.lastImportedId ?: error("No se pudo calcular el identificador")
            val existing = dao.findById(id)
            if (existing != null) path.delete() else dao.insert(BookEntity(id, metadata.title, metadata.authors.joinToString("\n"), path.absolutePath, metadata.coverEntry, System.currentTimeMillis()))
            Unit
        }
    }

    suspend fun delete(book: BookEntity) = withContext(Dispatchers.IO) {
        dao.delete(book)
        File(book.privatePath).delete()
    }
}
