package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Trip
import com.example.data.model.VehicleLocation
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TripRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            FirebaseApp.getInstance(),
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentUserName(): String {
        return auth.currentUser?.displayName?.ifBlank { "Driver" } ?: "Driver"
    }

    suspend fun createTrip(
        title: String,
        originName: String,
        originLat: Double,
        originLng: Double,
        destinationName: String,
        destLat: Double,
        destLng: Double,
        selectedRoutePolyline: String,
        routeDistanceKm: Double,
        routeDurationMinutes: Double,
        driverVehicleName: String = "Vehicle",
        driverVehicleColor: String = "#2563EB"
    ): Result<Trip> {
        val uid = requireUserId()
        val tripId = "trip_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val joinCode = generateJoinCode()

        val safeTitle = title.trim().take(180).ifBlank { "Convoy Trip" }
        val safeOriginName = originName.trim().take(500).ifBlank { "Current Location" }
        val safeDestinationName = destinationName.trim().take(500).ifBlank { "Destination" }
        val safePolyline = selectedRoutePolyline.take(200000)
        val safeDistance = if (routeDistanceKm.isNaN() || routeDistanceKm < 0.0) 0.0 else routeDistanceKm
        val safeDuration = if (routeDurationMinutes.isNaN() || routeDurationMinutes < 0.0) 0.0 else routeDurationMinutes
        val safeDriverName = currentUserName().trim().take(80).ifBlank { "Driver" }
        val safeVehicleName = driverVehicleName.trim().take(80).ifBlank { "Vehicle" }
        val safeVehicleColor = driverVehicleColor.trim().take(24).ifBlank { "#2563EB" }

        val tripData = hashMapOf(
            "tripId" to tripId,
            "title" to safeTitle,
            "originName" to safeOriginName,
            "originLat" to originLat,
            "originLng" to originLng,
            "destinationName" to safeDestinationName,
            "destinationLat" to destLat,
            "destinationLng" to destLng,
            "creatorId" to uid,
            "joinCode" to joinCode,
            "status" to "active",
            "selectedRoutePolyline" to safePolyline,
            "routeDistanceKm" to safeDistance,
            "routeDurationMinutes" to safeDuration,
            "participantIds" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val initialVehicleData = hashMapOf(
            "vehicleId" to uid,
            "tripId" to tripId,
            "driverName" to safeDriverName,
            "vehicleName" to safeVehicleName,
            "vehicleColor" to safeVehicleColor,
            "latitude" to originLat,
            "longitude" to originLng,
            "speedKmh" to 0.0,
            "heading" to 0.0,
            "batteryPercent" to 100.0,
            "status" to "moving",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val tripRef = db.collection("trips").document(tripId)
            tripRef.set(tripData).await()

            // Register creator's initial vehicle location in trip subcollection
            try {
                tripRef.collection("vehicles").document(uid).set(initialVehicleData).await()
            } catch (ve: Exception) {
                android.util.Log.w("TripRepo", "Initial vehicle subcollection write: ${ve.message}")
            }

            val createdTrip = Trip(
                id = tripId,
                tripId = tripId,
                title = safeTitle,
                originName = safeOriginName,
                originLat = originLat,
                originLng = originLng,
                destinationName = safeDestinationName,
                destinationLat = destLat,
                destinationLng = destLng,
                creatorId = uid,
                joinCode = joinCode,
                status = "active",
                selectedRoutePolyline = safePolyline,
                routeDistanceKm = safeDistance,
                routeDurationMinutes = safeDuration,
                participantIds = listOf(uid)
            )
            Result.success(createdTrip)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "trips/$tripId")
            Result.failure(e)
        }
    }

    fun observeUserTrips(userId: String): Flow<List<Trip>> = flow {
        val query = db.collection("trips")
            .whereArrayContains("participantIds", userId)

        query.snapshots()
            .map { snapshot -> snapshot.toObjects(Trip::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "trips")
                throw e
            }
            .collect { emit(it) }
    }

    fun observeTrip(tripId: String): Flow<Trip?> = flow {
        db.collection("trips").document(tripId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(Trip::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.READ, "trips/$tripId")
                throw e
            }
            .collect { emit(it) }
    }

    fun observeVehicles(tripId: String): Flow<List<VehicleLocation>> = flow {
        db.collection("trips").document(tripId).collection("vehicles")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(VehicleLocation::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "trips/$tripId/vehicles")
                throw e
            }
            .collect { emit(it) }
    }

    suspend fun joinTripByCode(
        joinCode: String,
        vehicleName: String = "Vehicle",
        vehicleColor: String = "#10B981"
    ): Result<Trip> {
        val uid = requireUserId()
        val cleanCode = joinCode.trim().uppercase()

        return try {
            val querySnapshot = db.collection("trips")
                .whereEqualTo("joinCode", cleanCode)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(IllegalArgumentException("No active trip found with code: $cleanCode"))
            }

            val doc = querySnapshot.documents[0]
            val trip = doc.toObject(Trip::class.java)
                ?: return Result.failure(IllegalStateException("Failed to parse trip document"))

            val currentParticipants = trip.participantIds.toMutableList()
            if (!currentParticipants.contains(uid)) {
                currentParticipants.add(uid)
                doc.reference.update(
                    mapOf(
                        "participantIds" to currentParticipants,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Register/update vehicle in the trip's vehicles subcollection
            val vehicleData = hashMapOf(
                "vehicleId" to uid,
                "tripId" to trip.tripId,
                "driverName" to currentUserName().trim().take(80).ifBlank { "Driver" },
                "vehicleName" to vehicleName.trim().take(80).ifBlank { "Vehicle" },
                "vehicleColor" to vehicleColor.trim().take(24).ifBlank { "#10B981" },
                "latitude" to trip.originLat,
                "longitude" to trip.originLng,
                "speedKmh" to 0.0,
                "heading" to 0.0,
                "batteryPercent" to 100.0,
                "status" to "moving",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                doc.reference.collection("vehicles").document(uid).set(vehicleData).await()
            } catch (ve: Exception) {
                android.util.Log.w("TripRepo", "Join subcollection vehicle write: ${ve.message}")
            }

            Result.success(trip.copy(participantIds = currentParticipants))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "trips/joinCode=$joinCode")
            Result.failure(e)
        }
    }

    suspend fun updateVehicleLocation(
        tripId: String,
        location: VehicleLocation
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val payload = hashMapOf(
                "vehicleId" to uid,
                "tripId" to tripId,
                "driverName" to location.driverName.ifBlank { currentUserName() }.trim().take(80).ifBlank { "Driver" },
                "vehicleName" to location.vehicleName.trim().take(80).ifBlank { "Vehicle" },
                "vehicleColor" to location.vehicleColor.trim().take(24).ifBlank { "#2563EB" },
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "speedKmh" to location.speedKmh.coerceAtLeast(0.0),
                "heading" to location.heading.coerceIn(0.0, 360.0),
                "batteryPercent" to location.batteryPercent.coerceIn(0.0, 100.0),
                "status" to location.status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("trips").document(tripId)
                .collection("vehicles").document(uid)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId/vehicles/$uid")
            Result.failure(e)
        }
    }

    suspend fun leaveTrip(tripId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val tripRef = db.collection("trips").document(tripId)
            val snapshot = tripRef.get().await()
            val trip = snapshot.toObject(Trip::class.java)

            if (trip != null) {
                val updatedList = trip.participantIds.filter { it != uid }
                tripRef.update(
                    mapOf(
                        "participantIds" to updatedList,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Mark vehicle as offline
                tripRef.collection("vehicles").document(uid).update(
                    mapOf(
                        "status" to "offline",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId/leave")
            Result.failure(e)
        }
    }

    suspend fun completeTrip(tripId: String): Result<Unit> {
        return try {
            db.collection("trips").document(tripId).update(
                mapOf(
                    "status" to "completed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId/complete")
            Result.failure(e)
        }
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
