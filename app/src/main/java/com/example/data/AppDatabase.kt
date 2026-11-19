package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BrandProfileEntity::class,
        OverlayPresetEntity::class,
        StreamDestinationEntity::class,
        BroadcastSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brandProfileDao(): BrandProfileDao
    abstract fun overlayPresetDao(): OverlayPresetDao
    abstract fun streamDestinationDao(): StreamDestinationDao
    abstract fun broadcastSessionDao(): BroadcastSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "asian_press_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
