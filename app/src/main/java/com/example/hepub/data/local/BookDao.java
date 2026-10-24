package com.example.hepub.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface BookDao {
    @Query("SELECT * FROM books ORDER BY importedAt DESC") Flow<List<BookEntity>> observeAll();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insert(BookEntity book);
    @Delete void delete(BookEntity book);
    @Query("SELECT * FROM books WHERE id = :id LIMIT 1") BookEntity findById(String id);
    @Query("UPDATE books SET locatorJson = :locator WHERE id = :id") void saveLocator(String id, String locator);
}
