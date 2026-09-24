package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TranscriptionEntity

@Database(entities = [TranscriptionEntity::class], version = 1, exportSchema = false)
abstract class WhisperDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: WhisperDatabase? = null

        fun getDatabase(context: Context): WhisperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhisperDatabase::class.java,
                    "whisper_transcriptions_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
