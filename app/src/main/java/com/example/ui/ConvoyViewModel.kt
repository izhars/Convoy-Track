package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PlaceSearchResult
import com.example.data.model.RouteAlternative
import com.example.data.model.Trip
import com.example.data.model.TripSummary
import com.example.data.model.UserProfile
import com.example.data.model.VehicleLocation
import com.example.data.repository.RoutingRepository
import com.example.data.repository.TripRepository
import com.example.data.repository.UserRepository
import com.example.data.util.EtaCalculator
import com.example.data.util.RealTimeEta
import com.example.service.TrackingForegroundService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConvoyViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepository = TripRepository(application)
    private val userRepository = UserRepository(application)
    private val routingRepository = RoutingRepository()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _userTrips = MutableStateFlow<List<Trip>>(emptyList())
    val userTrips = _userTrips.asStateFlow()

    private val _activeTrip = MutableStateFlow<Trip?>(null)
    val activeTrip = _activeTrip.asStateFlow()

    private val _tripVehicles = MutableStateFlow<List<VehicleLocation>>(emptyList())
    val tripVehicles = _tripVehicles.asStateFlow()

    private val _placeSearchResults = MutableStateFlow<List<PlaceSearchResult>>(emptyList())
    val placeSearchResults = _placeSearchResults.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces = _isSearchingPlaces.asStateFlow()

    private val _calculatedRoutes = MutableStateFlow<List<RouteAlternative>>(emptyList())
    val calculatedRoutes = _calculatedRoutes.asStateFlow()

    private val _selectedRouteIndex = MutableStateFlow(0)
    val selectedRouteIndex = _selectedRouteIndex.asStateFlow()

    private val _isCalculatingRoutes = MutableStateFlow(false)
    val isCalculatingRoutes = _isCalculatingRoutes.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    private val _isCreatingTrip = MutableStateFlow(false)
    val isCreatingTrip = _isCreatingTrip.asStateFlow()

    val isTrackingServiceRunning: StateFlow<Boolean> = TrackingForegroundService.isServiceRunning
    val currentSpeedKmh: StateFlow<Double> = TrackingForegroundService.currentSpeedKmh

    private val _completedTripSummary = MutableStateFlow<TripSummary?>(null)
    val completedTripSummary = _completedTripSummary.asStateFlow()

    // Real-Time ETA calculator flow: recalculates live as user moves or GPS updates
    val realTimeEta: StateFlow<RealTimeEta> = combine(
        TrackingForegroundService.currentLocation,
        TrackingForegroundService.currentSpeedKmh,
        _activeTrip,
        _tripVehicles
    ) { loc, speed, trip, vehicles ->
        if (trip == null) {
            RealTimeEta(0.0, 0, "--:--", 0f, 0.0, 0.0, false)
        } else {
            val myUid = _currentUser.value?.uid
            val myVehicle = vehicles.firstOrNull { it.vehicleId == myUid }
            val curLat = loc?.latitude ?: myVehicle?.latitude ?: trip.originLat
            val curLng = loc?.longitude ?: myVehicle?.longitude ?: trip.originLng
            val curSpeed = if (speed > 0) speed else (myVehicle?.speedKmh ?: 0.0)

            EtaCalculator.calculateEta(
                currentLat = curLat,
                currentLng = curLng,
                destLat = trip.destinationLat,
                destLng = trip.destinationLng,
                originLat = trip.originLat,
                originLng = trip.originLng,
                routeTotalDistanceKm = trip.routeDistanceKm,
                routeExpectedDurationMinutes = trip.routeDurationMinutes,
                currentSpeedKmh = curSpeed
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RealTimeEta(0.0, 0, "--:--", 0f, 0.0, 0.0, false)
    )

    private var activeTripJob: Job? = null
    private var vehiclesJob: Job? = null

    init {
        Firebase.auth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            if (auth.currentUser != null) {
                loadUserData(auth.currentUser!!.uid)
            } else {
                _userProfile.value = null
                _userTrips.value = emptyList()
                _activeTrip.value = null
                _tripVehicles.value = emptyList()
            }
        }
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            loadUserData(uid)
        }
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            userRepository.observeUserProfile(uid)
                .catch { /* handled in repo */ }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }

        viewModelScope.launch {
            tripRepository.observeUserTrips(uid)
                .catch { /* handled in repo */ }
                .collect { trips ->
                    _userTrips.value = trips
                    if (_activeTrip.value == null) {
                        val active = trips.firstOrNull { it.status == "active" }
                        if (active != null) {
                            selectTrip(active)
                        }
                    } else if (_activeTrip.value?.status != "active") {
                        clearActiveTrip()
                    }
                }
        }
    }

    fun selectTrip(trip: Trip) {
        if (trip.status != "active") {
            _activeTrip.value = trip
            activeTripJob?.cancel()
            activeTripJob = null
            vehiclesJob?.cancel()
            vehiclesJob = null
            _tripVehicles.value = emptyList()
            return
        }

        _activeTrip.value = trip
        activeTripJob?.cancel()
        vehiclesJob?.cancel()

        activeTripJob = viewModelScope.launch {
            tripRepository.observeTrip(trip.tripId)
                .catch { /* handled */ }
                .collect { updated ->
                    if (updated == null || updated.status != "active") {
                        clearActiveTrip()
                    } else {
                        _activeTrip.value = updated
                    }
                }
        }

        vehiclesJob = viewModelScope.launch {
            tripRepository.observeVehicles(trip.tripId)
                .catch { /* handled */ }
                .collect { vehicles ->
                    _tripVehicles.value = vehicles
                }
        }
    }

    fun clearActiveTrip() {
        activeTripJob?.cancel()
        activeTripJob = null
        vehiclesJob?.cancel()
        vehiclesJob = null
        _activeTrip.value = null
        _tripVehicles.value = emptyList()
    }

    private var searchJob: Job? = null

    fun searchPlaces(query: String, debounceMs: Long = 250L) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            searchJob?.cancel()
            _placeSearchResults.value = emptyList()
            _isSearchingPlaces.value = false
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            _isSearchingPlaces.value = true
            try {
                val results = routingRepository.searchPlaces(trimmed)
                _placeSearchResults.value = results
            } catch (e: Exception) {
                _placeSearchResults.value = emptyList()
            } finally {
                _isSearchingPlaces.value = false
            }
        }
    }

    fun calculateRoutes(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        viewModelScope.launch {
            _isCalculatingRoutes.value = true
            val routes = routingRepository.calculateRoutes(originLat, originLng, destLat, destLng)
            _calculatedRoutes.value = routes
            _selectedRouteIndex.value = 0
            _isCalculatingRoutes.value = false
        }
    }

    suspend fun getPlaceNameForCoords(lat: Double, lng: Double): String {
        return routingRepository.reverseGeocode(lat, lng)
    }

    fun selectRouteAlternative(index: Int) {
        _selectedRouteIndex.value = index
    }

    fun createTrip(
        title: String,
        originName: String,
        originLat: Double,
        originLng: Double,
        destinationName: String,
        destLat: Double,
        destLng: Double,
        onSuccess: (Trip) -> Unit
    ) {
        val routes = _calculatedRoutes.value
        val selected = routes.getOrNull(_selectedRouteIndex.value) ?: routes.firstOrNull()
        val polyline = selected?.polyline ?: ""
        val distanceKm = selected?.distanceKm ?: 0.0
        val durationMin = selected?.durationMinutes ?: 0.0

        val vName = _userProfile.value?.vehicleName ?: "Vehicle"
        val vColor = _userProfile.value?.vehicleColor ?: "#2563EB"

        viewModelScope.launch {
            _isCreatingTrip.value = true
            try {
                val result = tripRepository.createTrip(
                    title = title,
                    originName = originName,
                    originLat = originLat,
                    originLng = originLng,
                    destinationName = destinationName,
                    destLat = destLat,
                    destLng = destLng,
                    selectedRoutePolyline = polyline,
                    routeDistanceKm = distanceKm,
                    routeDurationMinutes = durationMin,
                    driverVehicleName = vName,
                    driverVehicleColor = vColor
                )

                result.onSuccess { trip ->
                    _uiMessage.value = "Trip created! Join code: ${trip.joinCode}"
                    selectTrip(trip)
                    onSuccess(trip)
                }.onFailure { e ->
                    _uiMessage.value = "Failed to create trip: ${e.message}"
                }
            } finally {
                _isCreatingTrip.value = false
            }
        }
    }

    fun joinTrip(joinCode: String, onSuccess: (Trip) -> Unit) {
        val vName = _userProfile.value?.vehicleName ?: "Vehicle"
        val vColor = _userProfile.value?.vehicleColor ?: "#10B981"

        viewModelScope.launch {
            val result = tripRepository.joinTripByCode(
                joinCode = joinCode,
                vehicleName = vName,
                vehicleColor = vColor
            )
            result.onSuccess { trip ->
                _uiMessage.value = "Joined trip: ${trip.title}!"
                selectTrip(trip)
                onSuccess(trip)
            }.onFailure { e ->
                _uiMessage.value = e.message ?: "Failed to join trip"
            }
        }
    }

    fun startTracking(context: Context, trip: Trip) {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START
            putExtra(TrackingForegroundService.EXTRA_TRIP_ID, trip.tripId)
            putExtra(TrackingForegroundService.EXTRA_DRIVER_NAME, _userProfile.value?.displayName ?: "Driver")
            putExtra(TrackingForegroundService.EXTRA_VEHICLE_NAME, _userProfile.value?.vehicleName ?: "Vehicle")
            putExtra(TrackingForegroundService.EXTRA_VEHICLE_COLOR, _userProfile.value?.vehicleColor ?: "#2563EB")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiMessage.value = "Live GPS tracking started"
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        context.startService(intent)
        _uiMessage.value = "Live tracking stopped"
    }

    fun leaveCurrentTrip(context: Context) {
        val trip = _activeTrip.value ?: return
        val stats = TrackingForegroundService.tripStats.value

        // Calculate summary metrics
        val durationSec = if (stats.elapsedSeconds > 10) {
            stats.elapsedSeconds
        } else {
            (trip.routeDurationMinutes * 60).toLong().coerceAtLeast(60L)
        }
        val distanceKm = if (stats.distanceKm > 0.05) {
            stats.distanceKm
        } else {
            trip.routeDistanceKm
        }
        val avgSpeed = if (stats.avgSpeedKmh > 1.0) {
            stats.avgSpeedKmh
        } else if (durationSec > 0) {
            distanceKm / (durationSec / 3600.0)
        } else {
            0.0
        }
        val maxSpeed = if (stats.maxSpeedKmh > 1.0) stats.maxSpeedKmh else (avgSpeed * 1.25)

        val summary = TripSummary(
            tripId = trip.tripId,
            tripTitle = trip.title,
            originName = trip.originName,
            destinationName = trip.destinationName,
            totalDistanceKm = distanceKm,
            totalDurationSeconds = durationSec,
            averageSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            participantCount = _tripVehicles.value.size.coerceAtLeast(1),
            endedAtMillis = System.currentTimeMillis(),
            polyline = trip.selectedRoutePolyline,
            isCompletedByCreator = (trip.creatorId == _currentUser.value?.uid)
        )

        stopTracking(context)
        clearActiveTrip()
        _completedTripSummary.value = summary

        viewModelScope.launch {
            val result = tripRepository.leaveTrip(
                tripId = trip.tripId,
                finalDistanceKm = distanceKm,
                finalDurationSeconds = durationSec,
                finalAverageSpeedKmh = avgSpeed,
                finalMaxSpeedKmh = maxSpeed
            )
            result.onSuccess {
                _uiMessage.value = "Left convoy successfully"
            }.onFailure { e ->
                _uiMessage.value = "Failed to leave convoy: ${e.message}"
            }
        }
    }

    fun completeCurrentTrip(context: Context) {
        val trip = _activeTrip.value ?: return
        val stats = TrackingForegroundService.tripStats.value

        val durationSec = if (stats.elapsedSeconds > 10) {
            stats.elapsedSeconds
        } else {
            (trip.routeDurationMinutes * 60).toLong().coerceAtLeast(60L)
        }
        val distanceKm = if (stats.distanceKm > 0.05) {
            stats.distanceKm
        } else {
            trip.routeDistanceKm
        }
        val avgSpeed = if (stats.avgSpeedKmh > 1.0) {
            stats.avgSpeedKmh
        } else if (durationSec > 0) {
            distanceKm / (durationSec / 3600.0)
        } else {
            0.0
        }
        val maxSpeed = if (stats.maxSpeedKmh > 1.0) stats.maxSpeedKmh else (avgSpeed * 1.25)

        val summary = TripSummary(
            tripId = trip.tripId,
            tripTitle = trip.title,
            originName = trip.originName,
            destinationName = trip.destinationName,
            totalDistanceKm = distanceKm,
            totalDurationSeconds = durationSec,
            averageSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            participantCount = _tripVehicles.value.size.coerceAtLeast(1),
            endedAtMillis = System.currentTimeMillis(),
            polyline = trip.selectedRoutePolyline,
            isCompletedByCreator = true
        )

        stopTracking(context)
        clearActiveTrip()
        _completedTripSummary.value = summary

        viewModelScope.launch {
            val result = tripRepository.completeTrip(
                tripId = trip.tripId,
                finalDistanceKm = distanceKm,
                finalDurationSeconds = durationSec,
                finalAverageSpeedKmh = avgSpeed,
                finalMaxSpeedKmh = maxSpeed
            )
            result.onSuccess {
                _uiMessage.value = "Trip ended successfully!"
            }.onFailure { e ->
                _uiMessage.value = "Failed to end trip: ${e.message}"
            }
        }
    }

    fun dismissTripSummary() {
        _completedTripSummary.value = null
    }

    fun showTripSummary(summary: TripSummary) {
        _completedTripSummary.value = summary
    }

    fun openSummaryForPastTrip(trip: Trip) {
        val durationSec = if (trip.finalDurationSeconds > 0) {
            trip.finalDurationSeconds
        } else {
            (trip.routeDurationMinutes * 60).toLong().coerceAtLeast(60L)
        }
        val distanceKm = if (trip.finalDistanceKm > 0) trip.finalDistanceKm else trip.routeDistanceKm
        val avgSpeed = if (trip.finalAverageSpeedKmh > 0) {
            trip.finalAverageSpeedKmh
        } else if (durationSec > 0) {
            distanceKm / (durationSec / 3600.0)
        } else {
            0.0
        }
        val maxSpeed = if (trip.finalMaxSpeedKmh > 0) trip.finalMaxSpeedKmh else (avgSpeed * 1.25)

        _completedTripSummary.value = TripSummary(
            tripId = trip.tripId,
            tripTitle = trip.title,
            originName = trip.originName,
            destinationName = trip.destinationName,
            totalDistanceKm = distanceKm,
            totalDurationSeconds = durationSec,
            averageSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            participantCount = trip.participantCount.coerceAtLeast(1),
            endedAtMillis = trip.createdAtMillis,
            polyline = trip.selectedRoutePolyline,
            isCompletedByCreator = true
        )
    }

    fun saveProfile(displayName: String, vehicleName: String, vehicleColor: String) {
        viewModelScope.launch {
            val result = userRepository.saveUserProfile(displayName, vehicleName, vehicleColor)
            result.onSuccess {
                _userProfile.value = it
                _uiMessage.value = "Profile and vehicle saved!"
            }.onFailure { e ->
                _uiMessage.value = "Save failed: ${e.message}"
            }
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}
