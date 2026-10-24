package com.example.hepub.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.example.hepub.data.local.AppDatabase
import com.example.hepub.data.local.BookEntity

class ReaderBookLoaderTest {
    private lateinit var db: AppDatabase
    @Before fun setUp() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build() }
    @After fun tearDown() { db.close() }
    @Test fun loadsBookThroughReaderFlow() = runTest {
        withContext(Dispatchers.IO) { db.bookDao().insert(BookEntity("id", "Title", "Author", "/private/book.epub", null, 1L)) }
        assertEquals("/private/book.epub", ReaderBookLoader(db.bookDao()).load("id")?.privatePath)
    }
}
