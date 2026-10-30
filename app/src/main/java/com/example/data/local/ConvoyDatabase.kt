package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocationCacheEntity::class], version = 1, exportSchema = false)
abstract class ConvoyDatabase : RoomDatabase() {

    abstract fun locationCacheDao(): LocationCacheDao

    companion object {
        @Volatile
        private var INSTANCE: ConvoyDatabase? = null

        fun getInstance(context: Context): ConvoyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConvoyDatabase::class.java,
                    "convoy_cache.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
