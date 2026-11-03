package com.example

import com.example.data.model.TripSummary
import com.example.data.util.EtaCalculator
import com.example.service.TrackingForegroundService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtaAndTripSummaryTest {

    @Test
    fun testHaversineDistanceCalculation() {
        // Distance between Bangalore (12.9716, 77.5946) and Mysore (12.2958, 76.6394) is ~128-140 km
        val distanceKm = EtaCalculator.calculateDistanceKm(
            lat1 = 12.9716,
            lon1 = 77.5946,
            lat2 = 12.2958,
            lon2 = 76.6394
        )
        assertTrue("Straight-line distance should be ~125-145 km", distanceKm in 125.0..145.0)
    }

    @Test
    fun testEtaCalculatorBasic() {
        // 50 km remaining, moving at 50 km/h should take approximately 60 minutes
        val eta = EtaCalculator.calculateEta(
            currentLat = 12.9716,
            currentLng = 77.5946,
            destLat = 12.2958,
            destLng = 76.6394,
            originLat = 13.0,
            originLng = 77.6,
            routeTotalDistanceKm = 140.0,
            routeExpectedDurationMinutes = 150.0,
            currentSpeedKmh = 60.0
        )

        assertTrue("Remaining distance should be positive", eta.remainingDistanceKm > 0)
        assertTrue("Remaining minutes should be reasonable", eta.remainingMinutes > 30)
        assertFalse("Should not be near destination yet", eta.isNearDestination)
        assertTrue("ETA clock string should not be empty", eta.etaArrivalTimeString.isNotBlank())
        assertTrue("Route completion percent should be between 0 and 1", eta.routeCompletionPercent in 0.0f..1.0f)
    }

    @Test
    fun testEtaNearDestination() {
        // Less than 500 meters away
        val eta = EtaCalculator.calculateEta(
            currentLat = 12.9716,
            currentLng = 77.5946,
            destLat = 12.9720,
            destLng = 77.5950,
            originLat = 13.0,
            originLng = 77.6,
            routeTotalDistanceKm = 10.0,
            routeExpectedDurationMinutes = 20.0,
            currentSpeedKmh = 25.0
        )

        assertTrue("Should detect near destination", eta.isNearDestination)
        assertTrue("Remaining minutes should be small", eta.remainingMinutes <= 5)
    }

    @Test
    fun testEtaZeroSpeedFallback() {
        // When vehicle is stopped at a traffic signal (speed = 0)
        val eta = EtaCalculator.calculateEta(
            currentLat = 12.9716,
            currentLng = 77.5946,
            destLat = 12.5,
            destLng = 77.0,
            originLat = 13.0,
            originLng = 77.6,
            routeTotalDistanceKm = 80.0,
            routeExpectedDurationMinutes = 100.0,
            currentSpeedKmh = 0.0
        )

        // Should NOT produce infinite or zero minutes; fallback baseline speed prevents division by zero
        assertTrue("Remaining minutes should be positive and finite", eta.remainingMinutes > 0)
        assertFalse("ETA clock should be a valid time", eta.etaArrivalTimeString == "--:--")
    }

    @Test
    fun testTripSummaryMetrics() {
        val summary = TripSummary(
            tripId = "trip_test_123",
            tripTitle = "Bangalore to Goa Convoy",
            originName = "Bangalore",
            destinationName = "Goa",
            totalDistanceKm = 560.5,
            totalDurationSeconds = 36000L, // 10 hours
            averageSpeedKmh = 56.0,
            maxSpeedKmh = 88.5,
            participantCount = 4,
            endedAtMillis = System.currentTimeMillis(),
            polyline = "abcde",
            isCompletedByCreator = true
        )

        assertEquals("Bangalore to Goa Convoy", summary.tripTitle)
        assertEquals(560.5, summary.totalDistanceKm, 0.01)
        assertEquals(36000L, summary.totalDurationSeconds)
        assertEquals(56.0, summary.averageSpeedKmh, 0.01)
        assertEquals(88.5, summary.maxSpeedKmh, 0.01)
        assertEquals(4, summary.participantCount)
        assertTrue(summary.isCompletedByCreator)
    }

    @Test
    fun testLiveTripStatsAccumulation() {
        val initial = TrackingForegroundService.LiveTripStats(
            distanceKm = 0.0,
            elapsedSeconds = 0L,
            avgSpeedKmh = 0.0,
            maxSpeedKmh = 0.0
        )
        assertEquals(0.0, initial.distanceKm, 0.001)

        val updated = TrackingForegroundService.LiveTripStats(
            distanceKm = 15.4, // 15.4 km
            elapsedSeconds = 1200L,  // 20 min
            avgSpeedKmh = 46.2,
            maxSpeedKmh = 68.0
        )
        assertEquals(15.4, updated.distanceKm, 0.01)
        assertEquals(46.2, updated.avgSpeedKmh, 0.01)
        assertEquals(68.0, updated.maxSpeedKmh, 0.01)
    }
}
