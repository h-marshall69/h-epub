package com.example.hepub.data

import com.example.hepub.data.local.BookDao
import com.example.hepub.data.local.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads reader records off the main thread. */
class ReaderBookLoader(private val dao: BookDao) {
    suspend fun load(id: String): BookEntity? = withContext(Dispatchers.IO) { dao.findById(id) }
}
