package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.ui.ConvoyViewModel
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.ConvoyWarningAmber
import java.util.Locale

@Composable
fun TripHistoryScreen(
    viewModel: ConvoyViewModel,
    onTripSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.userTrips.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val completedTripSummary by viewModel.completedTripSummary.collectAsState()

    // Show Trip Summary Dashboard when a trip summary is active
    completedTripSummary?.let { summary ->
        TripSummaryDashboard(
            summary = summary,
            onDismiss = { viewModel.dismissTripSummary() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConvoyNavyDark)
            .padding(20.dp)
            .testTag("trip_history_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = ConvoyPrimaryCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Trip History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Your convoy journeys, routes & past summaries",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trips found. Create your first convoy trip to see history here!",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trips) { trip ->
                    val isCurrent = trip.tripId == activeTrip?.tripId
                    TripHistoryCard(
                        trip = trip,
                        isCurrent = isCurrent,
                        onClick = {
                            if (trip.status == "completed") {
                                viewModel.openSummaryForPastTrip(trip)
                            } else {
                                viewModel.selectTrip(trip)
                                onTripSelected()
                            }
                        },
                        onViewSummaryClick = {
                            viewModel.openSummaryForPastTrip(trip)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripHistoryCard(
    trip: Trip,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onViewSummaryClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF1E3A5F) else Color(0xFF131D31)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCurrent) ConvoyPrimaryCyan else Color(0x22FFFFFF),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag("trip_card_${trip.tripId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trip.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusColor = if (trip.status == "active") ConvoyAccentGreen else Color(0xFF94A3B8)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trip.status.uppercase(),
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${trip.originName} ➔ ${trip.destinationName}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: distance, time, avg speed if available
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val distanceDisplay = if (trip.finalDistanceKm > 0) {
                    String.format(Locale.US, "%.1f km", trip.finalDistanceKm)
                } else if (trip.routeDistanceKm > 0) {
                    "${trip.routeDistanceKm} km"
                } else null

                distanceDisplay?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = ConvoyPrimaryCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                if (trip.finalAverageSpeedKmh > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = ConvoyWarningAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${trip.finalAverageSpeedKmh.toInt()} km/h",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Text(
                    text = "${trip.participantIds.size} Drivers",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Summary button for completed trips
            if (trip.status == "completed") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onViewSummaryClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConvoyPrimaryCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_summary_${trip.tripId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Trip Summary Dashboard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
