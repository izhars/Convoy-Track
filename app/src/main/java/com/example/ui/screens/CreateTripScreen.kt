package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Trip
import com.example.ui.ConvoyViewModel
import com.example.ui.components.LeafletMapView
import com.example.ui.components.inputColors
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.ConvoyWarningAmber
import kotlinx.coroutines.launch

enum class PickerMode {
    NONE,
    PICK_ORIGIN,
    PICK_DESTINATION
}

@SuppressLint("MissingPermission")
@Composable
fun CreateTripScreen(
    viewModel: ConvoyViewModel,
    onTripCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }

    // Origin state
    var originName by remember { mutableStateOf("Current GPS Location") }
    var originLat by remember { mutableDoubleStateOf(19.0760) }
    var originLng by remember { mutableDoubleStateOf(72.8777) }

    // Destination state
    var destinationName by remember { mutableStateOf("") }
    var destLat by remember { mutableDoubleStateOf(0.0) }
    var destLng by remember { mutableDoubleStateOf(0.0) }

    var activePickerMode by remember { mutableStateOf(PickerMode.NONE) }

    val calculatedRoutes by viewModel.calculatedRoutes.collectAsState()
    val selectedRouteIndex by viewModel.selectedRouteIndex.collectAsState()
    val isCalculatingRoutes by viewModel.isCalculatingRoutes.collectAsState()
    val isCreatingTrip by viewModel.isCreatingTrip.collectAsState()

    // Auto-detect device GPS location on launch
    LaunchedEffect(Unit) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                originLat = loc.latitude
                originLng = loc.longitude
                originName = "Current GPS Location"
            }
        } catch (e: Exception) {}
    }

    fun triggerRouteCalculation() {
        if (destLat != 0.0 && destLng != 0.0 && originLat != 0.0 && originLng != 0.0) {
            viewModel.calculateRoutes(originLat, originLng, destLat, destLng)
        }
    }

    // Modal Location Picker when choosing either Origin or Destination
    if (activePickerMode != PickerMode.NONE) {
        val isOrigin = activePickerMode == PickerMode.PICK_ORIGIN
        LocationPickerModal(
            isOrigin = isOrigin,
            initialName = if (isOrigin) originName else destinationName,
            initialLat = if (isOrigin) originLat else (if (destLat != 0.0) destLat else originLat + 0.02),
            initialLng = if (isOrigin) originLng else (if (destLng != 0.0) destLng else originLng + 0.02),
            viewModel = viewModel,
            onConfirm = { selectedName, selectedLat, selectedLng ->
                if (isOrigin) {
                    originName = selectedName
                    originLat = selectedLat
                    originLng = selectedLng
                } else {
                    destinationName = selectedName
                    destLat = selectedLat
                    destLng = selectedLng
                }
                activePickerMode = PickerMode.NONE
                triggerRouteCalculation()
            },
            onDismiss = { activePickerMode = PickerMode.NONE }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConvoyNavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Create Group Trip",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tap Origin or Destination to search places or pick on the interactive map.",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        // ================= ROUTE OVERVIEW MAP =================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ConvoyPrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "Trip Route Map",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (destLat != 0.0) "Route Connected (A ➔ B)" else "Set Destination to Preview Route",
                        fontSize = 11.sp,
                        color = if (destLat != 0.0) ConvoyAccentGreen else Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val previewTrip = remember(originLat, originLng, destLat, destLng, destinationName) {
                        Trip(
                            originLat = originLat,
                            originLng = originLng,
                            originName = originName,
                            destinationLat = if (destLat != 0.0) destLat else originLat,
                            destinationLng = if (destLng != 0.0) destLng else originLng,
                            destinationName = destinationName.ifBlank { "Destination" }
                        )
                    }

                    val selectedPolyline = calculatedRoutes.getOrNull(selectedRouteIndex)?.polyline

                    LeafletMapView(
                        vehicles = emptyList(),
                        currentTrip = previewTrip,
                        selectedAlternativePolyline = selectedPolyline,
                        onMapClick = { _, _ -> },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= ORIGIN CARD (TAP TO OPEN MAP & SEARCH) =================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .clickable { activePickerMode = PickerMode.PICK_ORIGIN }
                .testTag("origin_picker_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ConvoyPrimaryCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "A", color = ConvoyNavyDark, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(text = "ORIGIN POINT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConvoyPrimaryCyan)
                        Text(
                            text = originName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap to search or drop pin on map",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = ConvoyPrimaryCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Change", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConvoyPrimaryCyan)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ================= DESTINATION CARD (TAP TO OPEN MAP & SEARCH) =================
        val isDestSelected = destinationName.isNotBlank() && destLat != 0.0
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDestSelected) Color(0xFF131D31) else Color(0xFF1E2738)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (isDestSelected) ConvoyWarningAmber else ConvoyPrimaryCyan.copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { activePickerMode = PickerMode.PICK_DESTINATION }
                .testTag("destination_picker_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDestSelected) ConvoyWarningAmber else Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = if (isDestSelected) ConvoyNavyDark else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(text = "DESTINATION POINT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConvoyWarningAmber)
                        Text(
                            text = if (isDestSelected) destinationName else "Select Destination...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDestSelected) Color.White else Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isDestSelected) "Tap to change destination" else "Tap to search or pick on map",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDestSelected) Color(0xFF1E293B) else ConvoyPrimaryCyan)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDestSelected) Icons.Default.Edit else Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = if (isDestSelected) ConvoyWarningAmber else ConvoyNavyDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDestSelected) "Change" else "Select",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDestSelected) ConvoyWarningAmber else ConvoyNavyDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Trip Title
        Text(text = "Trip Title (Optional)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text(if (destinationName.isNotBlank()) "Trip to ${destinationName.take(25)}" else "e.g. Goa Convoy Roadtrip") },
            modifier = Modifier.fillMaxWidth().testTag("trip_title_input"),
            colors = inputColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Route Calculation Feedback
        if (isCalculatingRoutes) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ConvoyPrimaryCyan, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Calculating fastest route alternatives...", color = Color.White, fontSize = 13.sp)
            }
        }

        // ================= ROUTE ALTERNATIVES =================
        if (calculatedRoutes.isNotEmpty()) {
            Text(text = "Select Preferred Route", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            calculatedRoutes.forEachIndexed { index, route ->
                val isSelected = selectedRouteIndex == index
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF131D31)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.5.dp,
                            if (isSelected) ConvoyPrimaryCyan else Color(0x22FFFFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectRouteAlternative(index) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = route.summary,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (route.isRecommended) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ConvoyAccentGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "FASTEST", color = ConvoyAccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${route.distanceKm} km • Approx ${route.durationMinutes.toInt()} minutes",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) ConvoyPrimaryCyan else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create Trip Submit Button
        Button(
            onClick = {
                val finalTitle = title.ifBlank { "Trip to ${destinationName.take(25)}" }
                viewModel.createTrip(
                    title = finalTitle,
                    originName = originName,
                    originLat = originLat,
                    originLng = originLng,
                    destinationName = destinationName,
                    destLat = destLat,
                    destLng = destLng,
                    onSuccess = { onTripCreated() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_create_trip_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConvoyPrimaryBlue),
            enabled = !isCreatingTrip && destinationName.isNotBlank() && destLat != 0.0 && destLng != 0.0
        ) {
            if (isCreatingTrip) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Launching Convoy Trip...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Text(
                    text = "Launch Convoy Trip",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Full-screen / Modal Location Picker combining live search autocomplete and interactive map pin dropping
 */
@SuppressLint("MissingPermission")
@Composable
fun LocationPickerModal(
    isOrigin: Boolean,
    initialName: String,
    initialLat: Double,
    initialLng: Double,
    viewModel: ConvoyViewModel,
    onConfirm: (String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf(initialName.ifBlank { if (isOrigin) "Selected Origin" else "Selected Destination" }) }
    var selectedLat by remember { mutableDoubleStateOf(initialLat) }
    var selectedLng by remember { mutableDoubleStateOf(initialLng) }
    var isReverseGeocoding by remember { mutableStateOf(false) }

    val placeResults by viewModel.placeSearchResults.collectAsState()
    val isSearching by viewModel.isSearchingPlaces.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ConvoyNavyDark)
        ) {
            // Full screen Map View
            val dummyTrip = remember(selectedLat, selectedLng, isOrigin) {
                if (isOrigin) {
                    Trip(originLat = selectedLat, originLng = selectedLng, originName = selectedName)
                } else {
                    Trip(destinationLat = selectedLat, destinationLng = selectedLng, destinationName = selectedName)
                }
            }

            LeafletMapView(
                vehicles = emptyList(),
                currentTrip = dummyTrip,
                onMapClick = { clickedLat, clickedLng ->
                    selectedLat = clickedLat
                    selectedLng = clickedLng
                    isReverseGeocoding = true
                    coroutineScope.launch {
                        val resolvedName = viewModel.getPlaceNameForCoords(clickedLat, clickedLng)
                        selectedName = resolvedName
                        isReverseGeocoding = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Search Bar & Header
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = if (isOrigin) "Select Origin (A)" else "Select Destination (B)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOrigin) ConvoyPrimaryCyan else ConvoyWarningAmber
                                )
                                Text(
                                    text = "Search address or tap anywhere on map",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Input Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    viewModel.searchPlaces(it, debounceMs = 250L)
                                },
                                placeholder = { Text("Search city, station, hotel, street...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ConvoyPrimaryCyan)
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSearching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                                color = ConvoyPrimaryCyan,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                searchQuery = ""
                                                viewModel.searchPlaces("")
                                            }) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { viewModel.searchPlaces(searchQuery, debounceMs = 0L) }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("location_search_input"),
                                colors = inputColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.searchPlaces(searchQuery, debounceMs = 0L) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ConvoyPrimaryBlue),
                                modifier = Modifier.height(52.dp).testTag("trigger_search_button")
                            ) {
                                Text("Search", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick suggestion chips when not searching yet
                        if (searchQuery.isEmpty() && placeResults.isEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val popularPlaces = if (isOrigin) {
                                    listOf("Current GPS", "Goa", "Mumbai", "Delhi")
                                } else {
                                    listOf("Goa", "Panaji", "Airport", "Mumbai", "Delhi")
                                }

                                popularPlaces.forEach { place ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable {
                                                if (place == "Current GPS") {
                                                    try {
                                                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                                        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                                            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                                        if (loc != null) {
                                                            selectedLat = loc.latitude
                                                            selectedLng = loc.longitude
                                                            selectedName = "Current GPS Location"
                                                        }
                                                    } catch (e: Exception) {}
                                                } else {
                                                    searchQuery = place
                                                    viewModel.searchPlaces(place, debounceMs = 0L)
                                                }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = place,
                                            fontSize = 11.sp,
                                            color = if (place == "Current GPS") ConvoyAccentGreen else ConvoyPrimaryCyan,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // If user is picking origin, show quick GPS button
                        if (isOrigin && searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        try {
                                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                            if (loc != null) {
                                                selectedLat = loc.latitude
                                                selectedLng = loc.longitude
                                                selectedName = "Current GPS Location"
                                            }
                                        } catch (e: Exception) {}
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = ConvoyAccentGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Use Current GPS Location", color = ConvoyAccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Search Results Dropdown List
                if (placeResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ConvoyPrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(6.dp)
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            placeResults.forEach { place ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedName = place.primaryText
                                            selectedLat = place.latitude
                                            selectedLng = place.longitude
                                            searchQuery = place.primaryText
                                            viewModel.searchPlaces("")
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = ConvoyPrimaryCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = place.primaryText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (place.secondaryText.isNotBlank()) {
                                            Text(text = place.secondaryText, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (searchQuery.trim().length >= 2 && !isSearching) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0F172A)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PinDrop, contentDescription = null, tint = ConvoyWarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No direct name match for '$searchQuery'. Tap anywhere on the map to place pin!",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Bottom Confirmation Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isOrigin) ConvoyPrimaryCyan else ConvoyWarningAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOrigin) "A" else "B",
                                color = ConvoyNavyDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOrigin) "SELECTED ORIGIN" else "SELECTED DESTINATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOrigin) ConvoyPrimaryCyan else ConvoyWarningAmber
                            )
                            if (isReverseGeocoding) {
                                Text(text = "Resolving address...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            } else {
                                Text(
                                    text = selectedName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${String.format("%.4f", selectedLat)}, ${String.format("%.4f", selectedLng)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onConfirm(selectedName, selectedLat, selectedLng) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOrigin) ConvoyPrimaryBlue else ConvoyAccentGreen
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOrigin) "Confirm Origin Location" else "Confirm Destination Location",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
