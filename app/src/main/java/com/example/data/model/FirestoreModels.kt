package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val vehicleName: String = "Vehicle",
    val vehicleColor: String = "#2563EB",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Trip(
    @DocumentId val id: String = "",
    val tripId: String = "",
    val title: String = "",
    val destinationName: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val originName: String = "",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val creatorId: String = "",
    val joinCode: String = "",
    val status: String = "active", // active, completed, cancelled
    val selectedRoutePolyline: String = "",
    val routeDistanceKm: Double = 0.0,
    val routeDurationMinutes: Double = 0.0,
    val participantIds: List<String> = emptyList(),
    val finalDistanceKm: Double = 0.0,
    val finalDurationSeconds: Long = 0L,
    val finalAverageSpeedKmh: Double = 0.0,
    val finalMaxSpeedKmh: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val participantCount: Int get() = participantIds.size
    val createdAtMillis: Long get() = createdAt?.toDate()?.time ?: System.currentTimeMillis()
}

@IgnoreExtraProperties
data class TripSummary(
    val tripId: String = "",
    val tripTitle: String = "",
    val originName: String = "",
    val destinationName: String = "",
    val totalDistanceKm: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val averageSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val participantCount: Int = 1,
    val endedAtMillis: Long = System.currentTimeMillis(),
    val polyline: String = "",
    val isCompletedByCreator: Boolean = false
)

@IgnoreExtraProperties
data class VehicleLocation(
    @DocumentId val id: String = "",
    val vehicleId: String = "",
    val tripId: String = "",
    val driverName: String = "",
    val vehicleName: String = "Vehicle",
    val vehicleColor: String = "#2563EB",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Double = 0.0,
    val heading: Double = 0.0,
    val batteryPercent: Double = 100.0,
    val status: String = "moving", // moving, stopped, offline
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RouteAlternative(
    val routeIndex: Int,
    val summary: String,
    val distanceKm: Double,
    val durationMinutes: Double,
    val polyline: String,
    val isRecommended: Boolean = false
)

data class PlaceSearchResult(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latitude: Double,
    val longitude: Double
)
