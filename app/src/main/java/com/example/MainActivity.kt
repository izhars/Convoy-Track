package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ConvoyViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateTripScreen
import com.example.ui.screens.JoinTripScreen
import com.example.ui.screens.LiveMapScreen
import com.example.ui.screens.ParticipantsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripHistoryScreen
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.ConvoySurfaceDark
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        ConvoyAppContent()
    }
}

enum class ConvoyScreen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIVE_MAP("Map", Icons.Default.Navigation),
    CREATE_TRIP("Create", Icons.Default.AddLocationAlt),
    JOIN_TRIP("Join", Icons.Default.GroupAdd),
    PARTICIPANTS("Vehicles", Icons.Default.DirectionsCar),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun ConvoyAppContent(
    viewModel: ConvoyViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ConvoyScreen.LIVE_MAP) }
    val uiMessage by viewModel.uiMessage.collectAsState()

    // Request necessary runtime permissions (Location & Notifications)
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions result handled gracefully */ }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    LaunchedEffect(uiMessage) {
        if (!uiMessage.isNullOrBlank()) {
            Toast.makeText(context, uiMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    // Android predictive back handling
    if (currentScreen != ConvoyScreen.LIVE_MAP) {
        BackHandler {
            currentScreen = ConvoyScreen.LIVE_MAP
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                ConvoyScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ConvoyNavyDark,
                            selectedTextColor = ConvoyPrimaryCyan,
                            indicatorColor = ConvoyPrimaryCyan,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = ConvoyNavyDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ConvoyScreen.LIVE_MAP -> {
                    LiveMapScreen(
                        viewModel = viewModel,
                        onCreateTripClick = { currentScreen = ConvoyScreen.CREATE_TRIP },
                        onJoinTripClick = { currentScreen = ConvoyScreen.JOIN_TRIP }
                    )
                }
                ConvoyScreen.CREATE_TRIP -> {
                    CreateTripScreen(
                        viewModel = viewModel,
                        onTripCreated = { currentScreen = ConvoyScreen.LIVE_MAP }
                    )
                }
                ConvoyScreen.JOIN_TRIP -> {
                    JoinTripScreen(
                        viewModel = viewModel,
                        onTripJoined = { currentScreen = ConvoyScreen.LIVE_MAP }
                    )
                }
                ConvoyScreen.PARTICIPANTS -> {
                    ParticipantsScreen(
                        viewModel = viewModel
                    )
                }
                ConvoyScreen.HISTORY -> {
                    TripHistoryScreen(
                        viewModel = viewModel,
                        onTripSelected = { currentScreen = ConvoyScreen.LIVE_MAP }
                    )
                }
                ConvoyScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
