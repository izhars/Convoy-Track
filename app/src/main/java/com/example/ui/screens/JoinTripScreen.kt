package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConvoyViewModel
import com.example.ui.components.inputColors
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.VehicleAmber
import com.example.ui.theme.VehicleBlue
import com.example.ui.theme.VehicleGreen
import com.example.ui.theme.VehiclePurple
import com.example.ui.theme.VehicleRed
import com.example.ui.theme.VehicleTeal

@Composable
fun JoinTripScreen(
    viewModel: ConvoyViewModel,
    onTripJoined: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var joinCode by remember { mutableStateOf("") }
    val userProfile by viewModel.userProfile.collectAsState()

    var vehicleName by remember { mutableStateOf(userProfile?.vehicleName ?: "Honda City") }
    var selectedColorHex by remember { mutableStateOf(userProfile?.vehicleColor ?: "#10B981") }

    val vehicleColors = listOf(
        "#E53935", "#1E88E5", "#10B981", "#FB8C00", "#8E24AA", "#00897B", "#00D2FF"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConvoyNavyDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0xFF131D31)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = null,
                tint = ConvoyPrimaryCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Join Convoy Trip",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Enter the 6-character code shared by your trip host to sync live location.",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        // Code Input Field
        OutlinedTextField(
            value = joinCode,
            onValueChange = {
                if (it.length <= 8) {
                    joinCode = it.uppercase()
                }
            },
            placeholder = { Text("e.g. CONV88", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            trailingIcon = {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrBlank()) {
                        joinCode = clip.trim().uppercase()
                    }
                }) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = ConvoyPrimaryCyan)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("join_code_input"),
            colors = inputColors(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Vehicle Info Section
        Text(
            text = "Your Vehicle in the Convoy",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = vehicleName,
            onValueChange = { vehicleName = it },
            label = { Text("Vehicle Model / Name") },
            placeholder = { Text("e.g. Black Fortuner / Red Thar") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vehicle_name_input"),
            colors = inputColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Map Marker Color",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            vehicleColors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (isSelected) Color.White else Color.Transparent,
                            CircleShape
                        )
                        .clickable { selectedColorHex = hex }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                viewModel.joinTrip(joinCode) {
                    onTripJoined()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_join_trip_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConvoyAccentGreen),
            enabled = joinCode.trim().length >= 4
        ) {
            Text(
                text = "Join Convoy Now",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
