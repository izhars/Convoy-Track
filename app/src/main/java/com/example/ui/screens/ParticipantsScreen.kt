package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleLocation
import com.example.ui.ConvoyViewModel
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyDangerRed
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.ConvoyWarningAmber

@Composable
fun ParticipantsScreen(
    viewModel: ConvoyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTrip by viewModel.activeTrip.collectAsState()
    val vehicles by viewModel.tripVehicles.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConvoyNavyDark)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Convoy Vehicles",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${vehicles.size} Active Drivers in Convoy",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (activeTrip != null) {
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my vehicle convoy trip to ${activeTrip!!.destinationName} on Convoy Track! Join Code: ${activeTrip!!.joinCode}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Convoy Code"))
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ConvoyPrimaryCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No convoy vehicles active right now. Start a trip or invite drivers!",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(vehicles) { vehicle ->
                    val isMe = vehicle.vehicleId == currentUser?.uid
                    ParticipantCard(vehicle = vehicle, isMe = isMe)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeTrip != null) {
            val isCreator = activeTrip!!.creatorId == currentUser?.uid

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.leaveCurrentTrip(context) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Leave Convoy", fontSize = 13.sp)
                }

                if (isCreator) {
                    Button(
                        onClick = { viewModel.completeCurrentTrip(context) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConvoyDangerRed)
                    ) {
                        Text(text = "End Trip", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantCard(
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isMe) ConvoyPrimaryCyan.copy(alpha = 0.6f) else Color(0x22FFFFFF),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(vehicleColor.copy(alpha = 0.2f))
                        .border(2.dp, vehicleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = vehicleColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMe) "${vehicle.driverName} (You)" else vehicle.driverName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusColor = when (vehicle.status) {
                            "moving" -> ConvoyAccentGreen
                            "stopped" -> ConvoyWarningAmber
                            else -> Color(0xFF94A3B8)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = vehicle.status.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = vehicle.vehicleName,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = ConvoyPrimaryCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${vehicle.speedKmh.toInt()} km/h", fontSize = 11.sp, color = ConvoyPrimaryCyan, fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${vehicle.heading.toInt()}°", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BatteryFull, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${vehicle.batteryPercent.toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}
