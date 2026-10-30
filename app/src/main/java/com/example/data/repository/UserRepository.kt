package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
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

class UserRepository(private val db: FirebaseFirestore) {

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

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.READ, "users/$userId")
                throw e
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(
        displayName: String,
        vehicleName: String,
        vehicleColor: String
    ): Result<UserProfile> {
        val uid = requireUserId()
        val email = auth.currentUser?.email ?: ""
        val userDoc = db.collection("users").document(uid)

        return try {
            val existing = userDoc.get().await()
            val safeDisplayName = displayName.trim().take(80).ifBlank { "Driver" }
            val safeVehicleName = vehicleName.trim().take(80).ifBlank { "Vehicle" }
            val safeVehicleColor = vehicleColor.trim().take(24).ifBlank { "#2563EB" }
            val safeEmail = email.take(100)

            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to safeDisplayName,
                "email" to safeEmail,
                "vehicleName" to safeVehicleName,
                "vehicleColor" to safeVehicleColor,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                userDoc.set(payload).await()
            } else {
                userDoc.update(payload).await()
            }

            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName,
                    email = email,
                    vehicleName = vehicleName,
                    vehicleColor = vehicleColor
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }
}
