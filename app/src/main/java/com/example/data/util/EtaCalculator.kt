package com.example.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RealTimeEta(
    val remainingDistanceKm: Double,
    val remainingMinutes: Int,
    val etaTimeString: String, // e.g. "11:24 AM"
    val progressPercent: Float, // 0.0f .. 1.0f
    val currentSpeedKmh: Double,
    val effectiveSpeedKmh: Double,
    val isArrived: Boolean
) {
    val etaArrivalTimeString: String get() = etaTimeString
    val routeCompletionPercent: Float get() = progressPercent
    val isNearDestination: Boolean get() = isArrived || remainingDistanceKm < 0.5
}

object EtaCalculator {

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        haversineDistanceKm(lat1, lon1, lat2, lon2)

    fun calculateEta(
        currentLat: Double,
        currentLng: Double,
        destLat: Double,
        destLng: Double,
        originLat: Double,
        originLng: Double,
        routeTotalDistanceKm: Double,
        routeExpectedDurationMinutes: Double,
        currentSpeedKmh: Double
    ): RealTimeEta {
        if (destLat == 0.0 && destLng == 0.0) {
            return RealTimeEta(
                remainingDistanceKm = 0.0,
                remainingMinutes = 0,
                etaTimeString = "--:--",
                progressPercent = 0f,
                currentSpeedKmh = currentSpeedKmh,
                effectiveSpeedKmh = 0.0,
                isArrived = false
            )
        }

        val directRemainingKm = haversineDistanceKm(currentLat, currentLng, destLat, destLng)
        val directTotalKm = haversineDistanceKm(originLat, originLng, destLat, destLng)

        // Circuity factor converts straight line distance to approximate road network distance
        val circuityFactor = if (directTotalKm > 0.5 && routeTotalDistanceKm > directTotalKm) {
            (routeTotalDistanceKm / directTotalKm).coerceIn(1.05, 1.45)
        } else {
            1.2
        }

        val estimatedRoadRemainingKm = if (routeTotalDistanceKm > 0) {
            (directRemainingKm * circuityFactor).coerceAtMost(routeTotalDistanceKm * 1.1)
        } else {
            directRemainingKm * circuityFactor
        }

        // If within 150 meters, consider arrived
        val isArrived = directRemainingKm < 0.15

        // Effective speed calculation:
        // Blend current instantaneous speed with typical route speed to avoid wild fluctuations at traffic stops
        val baselineSpeedKmh = if (routeExpectedDurationMinutes > 0 && routeTotalDistanceKm > 0) {
            (routeTotalDistanceKm / (routeExpectedDurationMinutes / 60.0)).coerceIn(35.0, 90.0)
        } else {
            50.0
        }

        val effectiveSpeedKmh = when {
            isArrived -> 0.0
            currentSpeedKmh >= 15.0 -> {
                // Moving smoothly: blend current speed with baseline
                (currentSpeedKmh * 0.65 + baselineSpeedKmh * 0.35).coerceIn(20.0, 120.0)
            }
            currentSpeedKmh in 3.0..15.0 -> {
                // Slow traffic
                (currentSpeedKmh * 0.5 + baselineSpeedKmh * 0.5).coerceIn(15.0, 60.0)
            }
            else -> {
                // Stationary at red light/stop: use baseline road speed
                baselineSpeedKmh
            }
        }

        val remainingMinutes = if (isArrived) {
            0
        } else {
            ((estimatedRoadRemainingKm / effectiveSpeedKmh) * 60.0).roundToInt().coerceAtLeast(1)
        }

        // Compute arrival clock time
        val etaMillis = System.currentTimeMillis() + (remainingMinutes * 60 * 1000L)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val etaTimeString = if (isArrived) "Arrived" else timeFormat.format(Date(etaMillis))

        // Progress calculation
        val totalDistance = if (routeTotalDistanceKm > 0) routeTotalDistanceKm else (directTotalKm * circuityFactor).coerceAtLeast(1.0)
        val progress = if (isArrived) 1.0f else (1.0 - (estimatedRoadRemainingKm / totalDistance)).toFloat().coerceIn(0f, 1f)

        return RealTimeEta(
            remainingDistanceKm = Math.round(estimatedRoadRemainingKm * 10.0) / 10.0,
            remainingMinutes = remainingMinutes,
            etaTimeString = etaTimeString,
            progressPercent = progress,
            currentSpeedKmh = currentSpeedKmh,
            effectiveSpeedKmh = Math.round(effectiveSpeedKmh * 10.0) / 10.0,
            isArrived = isArrived
        )
    }

    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return 0.0
        if (lat2 == 0.0 && lon2 == 0.0) return 0.0
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
