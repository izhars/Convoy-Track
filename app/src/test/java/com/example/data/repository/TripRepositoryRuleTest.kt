package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TripRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTrip_authenticatedUser_createsSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TripRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTrip(
                title = "Delhi to Agra Highway",
                originName = "Delhi",
                originLat = 28.7041,
                originLng = 77.1025,
                destinationName = "Agra",
                destLat = 27.1767,
                destLng = 78.0081,
                selectedRoutePolyline = "sample_polyline_123",
                routeDistanceKm = 210.0,
                routeDurationMinutes = 180.0
            )
        }

        assertTrue("Expected trip creation to succeed", result.isSuccess)
        val trip = result.getOrThrow()
        assertNotNull(trip.tripId)
        assertEquals("Delhi to Agra Highway", trip.title)
    }

    @Test
    fun observeUserTrips_authenticatedUser_returnsTrips() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TripRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTrip(
                title = "Mumbai to Pune Expressway",
                originName = "Mumbai",
                originLat = 19.0760,
                originLng = 72.8777,
                destinationName = "Pune",
                destLat = 18.5204,
                destLng = 73.8567,
                selectedRoutePolyline = "sample_pune_poly",
                routeDistanceKm = 150.0,
                routeDurationMinutes = 120.0
            )
        }
        val created = createResult.getOrThrow()

        val trips = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserTrips(uid).first { list -> list.any { it.tripId == created.tripId } }
        }
        assertTrue(trips.any { it.tripId == created.tripId })
    }

    @Test
    fun createTrip_withLongAddressAndPolyline_createsSuccessfully() = runBlocking {
        signInTestUser(BOB_EMAIL)
        val repository = TripRepository(firestore)

        val longDestination = "Goa State Museum, Old Secretariat, Adil Shah Palace, Dayanand Bandodkar Marg, Altinho, Panaji, Tiswadi, North Goa, Goa, 403001, India, Coastal Highway Sector 4"
        val longPolyline = "a".repeat(45000)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTrip(
                title = "Goa Coastal Roadtrip",
                originName = "Chhatrapati Shivaji Maharaj Terminus Area, Mumbai, Maharashtra, 400001, India",
                originLat = 18.9400,
                originLng = 72.8353,
                destinationName = longDestination,
                destLat = 15.4909,
                destLng = 73.8278,
                selectedRoutePolyline = longPolyline,
                routeDistanceKm = 580.0,
                routeDurationMinutes = 620.0
            )
        }

        assertTrue("Failed with: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val trip = result.getOrThrow()
        assertNotNull(trip.tripId)
        assertTrue(trip.destinationName.contains("Panaji"))
    }

    @Test
    fun observeUserTrips_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = TripRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.observeUserTrips("unauth_user").first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            caughtException = e
        }

        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_convoy@test.com"
        const val BOB_EMAIL = "bob_convoy@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
