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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
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
