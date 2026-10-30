package com.example.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ConvoyPrimaryCyan

@Composable
fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ConvoyPrimaryCyan,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF131D31),
    unfocusedContainerColor = Color(0xFF131D31)
)
