package com.example.hepub.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "books")
public class BookEntity {
    @PrimaryKey @NonNull public String id;
    @NonNull public String title;
    @NonNull public String authors;
    @NonNull public String privatePath;
    public String coverEntry;
    public long importedAt;
    public String locatorJson;

    public BookEntity(@NonNull String id, @NonNull String title, @NonNull String authors,
                      @NonNull String privatePath, String coverEntry, long importedAt) {
        this.id = id; this.title = title; this.authors = authors;
        this.privatePath = privatePath; this.coverEntry = coverEntry; this.importedAt = importedAt;
    }
}
