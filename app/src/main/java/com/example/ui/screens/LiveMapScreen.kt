package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.data.model.VehicleLocation
import com.example.ui.ConvoyViewModel
import com.example.ui.components.LeafletMapView
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyDangerRed
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.ConvoySurfaceDark
import com.example.ui.theme.ConvoyWarningAmber

@Composable
fun LiveMapScreen(
    viewModel: ConvoyViewModel,
    onCreateTripClick: () -> Unit,
    onJoinTripClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTrip by viewModel.activeTrip.collectAsState()
    val tripVehicles by viewModel.tripVehicles.collectAsState()
    val isTrackingActive by viewModel.isTrackingServiceRunning.collectAsState()
    val currentSpeed by viewModel.currentSpeedKmh.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeTrip == null) {
            // Empty State: No active trip
            NoActiveTripView(
                onCreateTripClick = onCreateTripClick,
                onJoinTripClick = onJoinTripClick
            )
        } else {
            // Active Trip Map View
            LeafletMapView(
                vehicles = tripVehicles,
                currentTrip = activeTrip,
                onVehicleClick = { vId -> selectedVehicleId = vId },
                modifier = Modifier.fillMaxSize()
            )

            // Top Floating Trip Header
            TopTripHeader(
                trip = activeTrip!!,
                vehicleCount = tripVehicles.size,
                context = context,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            )

            // Bottom HUD / Live Convoy Carousel
            BottomConvoyHUD(
                activeTrip = activeTrip!!,
                vehicles = tripVehicles,
                myUserId = currentUser?.uid ?: "",
                currentSpeed = currentSpeed,
                isTrackingActive = isTrackingActive,
                onToggleTracking = {
                    if (isTrackingActive) {
                        viewModel.stopTracking(context)
                    } else {
                        viewModel.startTracking(context, activeTrip!!)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun TopTripHeader(
    trip: Trip,
    vehicleCount: Int,
    context: Context,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xDD0F172A)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "To: ${trip.destinationName}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Join Code Pill with Copy Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Convoy Join Code", trip.joinCode))
                            Toast.makeText(context, "Code ${trip.joinCode} copied!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = trip.joinCode,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ConvoyPrimaryCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = ConvoyPrimaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trip stats chips row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    label = "$vehicleCount ${if (vehicleCount == 1) "Car" else "Cars"} Moving",
                    color = ConvoyAccentGreen
                )
                if (trip.routeDistanceKm > 0) {
                    StatChip(
                        label = "${trip.routeDistanceKm} km",
                        color = ConvoyPrimaryBlue
                    )
                }
                if (trip.routeDurationMinutes > 0) {
                    StatChip(
                        label = "${trip.routeDurationMinutes.toInt()} min ETA",
                        color = ConvoyWarningAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun BottomConvoyHUD(
    activeTrip: Trip,
    vehicles: List<VehicleLocation>,
    myUserId: String,
    currentSpeed: Double,
    isTrackingActive: Boolean,
    onToggleTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Vehicles Horizontal List
        if (vehicles.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vehicles) { vehicle ->
                    val isMe = vehicle.vehicleId == myUserId
                    VehicleHUDCard(vehicle = vehicle, isMe = isMe)
                }
            }
        }

        // Live Tracking Action Bar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Speed HUD
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTrackingActive) ConvoyAccentGreen.copy(alpha = 0.15f)
                                else Color(0xFF1E293B)
                            )
                            .border(
                                2.dp,
                                if (isTrackingActive) ConvoyAccentGreen else Color(0xFF334155),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${currentSpeed.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isTrackingActive) ConvoyAccentGreen else Color.White
                            )
                            Text(
                                text = "km/h",
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTrackingActive) "Live GPS Active" else "GPS Standby",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isTrackingActive) "Syncing every 3s" else "Tap start to share location",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Start/Stop Tracking Button
                Button(
                    onClick = onToggleTracking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTrackingActive) ConvoyDangerRed else ConvoyAccentGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("toggle_tracking_button")
                ) {
                    Icon(
                        imageVector = if (isTrackingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTrackingActive) "Stop" else "Start Live",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleHUDCard(
    vehicle: VehicleLocation,
    isMe: Boolean
) {
    val vehicleColor = try {
        Color(android.graphics.Color.parseColor(vehicle.vehicleColor))
    } catch (e: Exception) {
        ConvoyPrimaryBlue
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xDD1E293B)
        ),
        modifier = Modifier
            .width(150.dp)
            .border(
                1.5.dp,
                if (isMe) ConvoyPrimaryCyan else Color(0x22FFFFFF),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Vehicle dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(vehicleColor)
                )
                // Status dot
                val statusColor = when (vehicle.status) {
                    "moving" -> ConvoyAccentGreen
                    "stopped" -> ConvoyWarningAmber
                    else -> Color(0xFF94A3B8)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isMe) "${vehicle.driverName} (You)" else vehicle.driverName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = vehicle.vehicleName,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${vehicle.speedKmh.toInt()} km/h",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = ConvoyPrimaryCyan
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${vehicle.batteryPercent.toInt()}%",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoActiveTripView(
    onCreateTripClick: () -> Unit,
    onJoinTripClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConvoyNavyDark)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF131D31)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = ConvoyPrimaryCyan,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Active Convoy Trip",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White
        )

        Text(
            text = "Create a new trip or join your friends' convoy using a 6-character code to view live vehicle positions.",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            lineHeight = 20.sp
        )

        Button(
            onClick = onCreateTripClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("create_trip_nav_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConvoyPrimaryBlue)
        ) {
            Text(text = "Create New Trip", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onJoinTripClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("join_trip_nav_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
        ) {
            Text(text = "Join Trip with Code", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
