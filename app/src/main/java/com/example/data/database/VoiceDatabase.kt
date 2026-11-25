package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VoiceProfileEntity::class, GeneratedAudioEntity::class],
    version = 3,
    exportSchema = false
)
abstract class VoiceDatabase : RoomDatabase() {
    abstract fun voiceDao(): VoiceDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN language TEXT NOT NULL DEFAULT 'English'")
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN voiceTraits TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN estimatedAge TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN speakingCadence TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN confidenceScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE voice_profiles ADD COLUMN audioMimeType TEXT NOT NULL DEFAULT 'audio/mp4'")
            }
        }

        fun getDatabase(context: Context): VoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceDatabase::class.java,
                    "voice_clone_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
