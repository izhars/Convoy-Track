package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.ConvoyDatabase
import com.example.data.local.LocationCacheEntity
import com.example.data.model.VehicleLocation
import com.example.data.repository.TripRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackingForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: ConvoyDatabase
    private lateinit var tripRepository: TripRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private var activeTripId: String = ""
    private var driverName: String = "Driver"
    private var vehicleName: String = "Vehicle"
    private var vehicleColor: String = "#2563EB"
    private var isPaused: Boolean = false

    companion object {
        const val CHANNEL_ID = "convoy_live_tracking"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.convoy.START_TRACKING"
        const val ACTION_PAUSE = "com.example.convoy.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.example.convoy.RESUME_TRACKING"
        const val ACTION_STOP = "com.example.convoy.STOP_TRACKING"

        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_DRIVER_NAME = "extra_driver_name"
        const val EXTRA_VEHICLE_NAME = "extra_vehicle_name"
        const val EXTRA_VEHICLE_COLOR = "extra_vehicle_color"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation = _currentLocation.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0.0)
        val currentSpeedKmh = _currentSpeedKmh.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = ConvoyDatabase.getInstance(this)
        tripRepository = TripRepository(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Convoy::TrackingWakeLock")

        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                activeTripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: ""
                driverName = intent.getStringExtra(EXTRA_DRIVER_NAME) ?: "Driver"
                vehicleName = intent.getStringExtra(EXTRA_VEHICLE_NAME) ?: "Vehicle"
                vehicleColor = intent.getStringExtra(EXTRA_VEHICLE_COLOR) ?: "#2563EB"
                isPaused = false
                startTracking()
            }
            ACTION_PAUSE -> {
                isPaused = true
                updateNotification("Tracking paused", "Live location broadcast paused")
            }
            ACTION_RESUME -> {
                isPaused = false
                updateNotification("Live Tracking Active", "Broadcasting GPS to convoy")
            }
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isPaused) return
                val location = result.lastLocation ?: return

                _currentLocation.value = location
                val speedKmh = (location.speed * 3.6).toDouble()
                _currentSpeedKmh.value = speedKmh

                val batteryLevel = getBatteryLevel()

                updateNotification(
                    "Live Tracking Active: ${speedKmh.toInt()} km/h",
                    "Convoy Trip: $activeTripId • Battery: $batteryLevel%"
                )

                serviceScope.launch {
                    processLocationUpdate(location, speedKmh, batteryLevel)
                }
            }
        }
    }

    private suspend fun processLocationUpdate(location: Location, speedKmh: Double, batteryLevel: Double) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val status = if (speedKmh > 3.0) "moving" else "stopped"

        // 1. Cache in Room database
        val cacheEntity = LocationCacheEntity(
            tripId = activeTripId,
            vehicleId = uid,
            driverName = driverName,
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmh = speedKmh,
            heading = location.bearing.toDouble(),
            batteryPercent = batteryLevel,
            status = status,
            isSynced = false
        )
        val rowId = db.locationCacheDao().insertLocation(cacheEntity)

        // 2. Synchronize to Firestore
        if (activeTripId.isNotBlank()) {
            val vehicleLoc = VehicleLocation(
                id = uid,
                vehicleId = uid,
                tripId = activeTripId,
                driverName = driverName,
                vehicleName = vehicleName,
                vehicleColor = vehicleColor,
                latitude = location.latitude,
                longitude = location.longitude,
                speedKmh = speedKmh,
                heading = location.bearing.toDouble(),
                batteryPercent = batteryLevel,
                status = status
            )

            val result = tripRepository.updateVehicleLocation(activeTripId, vehicleLoc)
            if (result.isSuccess) {
                db.locationCacheDao().markAsSynced(listOf(rowId))
                flushPendingLocations(uid)
            }
        }
    }

    private suspend fun flushPendingLocations(uid: String) {
        try {
            val pending = db.locationCacheDao().getPendingLocations()
            if (pending.isNotEmpty()) {
                val syncedIds = mutableListOf<Long>()
                for (item in pending) {
                    val loc = VehicleLocation(
                        id = uid,
                        vehicleId = uid,
                        tripId = item.tripId,
                        driverName = item.driverName,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        speedKmh = item.speedKmh,
                        heading = item.heading,
                        batteryPercent = item.batteryPercent,
                        status = item.status
                    )
                    val res = tripRepository.updateVehicleLocation(item.tripId, loc)
                    if (res.isSuccess) {
                        syncedIds.add(item.id)
                    }
                }
                if (syncedIds.isNotEmpty()) {
                    db.locationCacheDao().markAsSynced(syncedIds)
                }
            }
        } catch (e: Exception) {
            Log.w("TrackingService", "Flush pending locations failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        _isServiceRunning.value = true
        wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours max wakeLock
        startForeground(NOTIFICATION_ID, buildNotification("Starting Convoy Tracking...", "Acquiring GPS fix"))

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("TrackingService", "Failed to request location updates", e)
        }
    }

    private fun stopTracking() {
        _isServiceRunning.value = false
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e("TrackingService", "Failed to remove location updates", e)
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun getBatteryLevel(): Double {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) {
            Math.round((level.toDouble() / scale.toDouble()) * 100.0 * 10.0) / 10.0
        } else {
            100.0
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Convoy Live Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for active group vehicle tracking"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                0,
                if (isPaused) "Resume" else "Pause",
                pausePendingIntent
            )
            .addAction(0, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
