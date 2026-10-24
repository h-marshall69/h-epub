package com.example.hepub.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {BookEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BookDao bookDao();
    private static volatile AppDatabase instance;
    public static AppDatabase get(Context context) {
        if (instance == null) synchronized (AppDatabase.class) {
            if (instance == null) instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "hepub.db").fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
