package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(entity: LocationCacheEntity): Long

    @Query("SELECT * FROM location_cache WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT 50")
    suspend fun getPendingLocations(): List<LocationCacheEntity>

    @Query("UPDATE location_cache SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT * FROM location_cache WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun observeCachedLocations(tripId: String): Flow<List<LocationCacheEntity>>

    @Query("DELETE FROM location_cache WHERE isSynced = 1 AND timestamp < :olderThanTimestamp")
    suspend fun purgeSyncedLocations(olderThanTimestamp: Long)
}
