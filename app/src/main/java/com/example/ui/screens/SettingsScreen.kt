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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.ui.ConvoyViewModel
import com.example.ui.components.inputColors
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyDangerRed
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan

@Composable
fun SettingsScreen(
    viewModel: ConvoyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var displayName by remember(userProfile) {
        mutableStateOf(userProfile?.displayName ?: currentUser?.displayName ?: "Convoy Driver")
    }
    var vehicleName by remember(userProfile) {
        mutableStateOf(userProfile?.vehicleName ?: "Honda City")
    }
    var vehicleColorHex by remember(userProfile) {
        mutableStateOf(userProfile?.vehicleColor ?: "#2563EB")
    }
    var highAccuracyMode by remember { mutableStateOf(true) }
    var batterySaverMode by remember { mutableStateOf(false) }

    val vehicleColors = listOf(
        "#E53935", "#1E88E5", "#10B981", "#FB8C00", "#8E24AA", "#00897B", "#00D2FF"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConvoyNavyDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = ConvoyPrimaryCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Vehicle profile, GPS telemetry & routing", fontSize = 13.sp, color = Color(0xFF94A3B8))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile & Vehicle Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Driver & Vehicle Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Driver Name") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ConvoyPrimaryCyan) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_name_input"),
                    colors = inputColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = vehicleName,
                    onValueChange = { vehicleName = it },
                    label = { Text("Vehicle Description") },
                    leadingIcon = { Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = ConvoyPrimaryCyan) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_vehicle_input"),
                    colors = inputColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Vehicle Marker Color", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    vehicleColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = vehicleColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                                .clickable { vehicleColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveProfile(displayName, vehicleName, vehicleColorHex) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConvoyPrimaryBlue),
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Save Profile", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // GPS & Background Tracking Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Telemetry & Background Tracking", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "High Accuracy GPS (3s interval)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Broadcast live speed and position every 3 seconds", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = highAccuracyMode,
                        onCheckedChange = { highAccuracyMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ConvoyAccentGreen)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Battery Saver Mode", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Adjust updates to 10s when stationary", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = batterySaverMode,
                        onCheckedChange = { batterySaverMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ConvoyAccentGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Mapping Engines Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = ConvoyPrimaryCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Map & Routing Engines", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Map Rendering: Leaflet + OpenStreetMap Vector Tiles\n• Place Search: Ola Maps Autocomplete & OpenStreetMap Nominatim\n• Route Calculation: Ola Maps Directions API & OSRM Engine",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Sign Out Button
        Button(
            onClick = {
                appSignOut(
                    context = context,
                    credentialManager = credentialManager,
                    onComplete = {},
                    scope = coroutineScope
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = ConvoyDangerRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Sign Out (${currentUser?.email ?: ""})", color = ConvoyDangerRed, fontWeight = FontWeight.SemiBold)
        }
    }
}
