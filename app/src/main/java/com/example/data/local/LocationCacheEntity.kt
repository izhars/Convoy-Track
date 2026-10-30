package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_cache")
data class LocationCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tripId: String,
    val vehicleId: String,
    val driverName: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val heading: Double,
    val batteryPercent: Double,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
