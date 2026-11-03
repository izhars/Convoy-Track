package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TripSummary
import com.example.ui.theme.ConvoyAccentGreen
import com.example.ui.theme.ConvoyNavyDark
import com.example.ui.theme.ConvoyPrimaryBlue
import com.example.ui.theme.ConvoyPrimaryCyan
import com.example.ui.theme.ConvoySurfaceCard
import com.example.ui.theme.ConvoySurfaceDark
import com.example.ui.theme.ConvoyWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripSummaryDashboard(
    summary: TripSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(ConvoyNavyDark)
                .testTag("trip_summary_dashboard"),
            color = ConvoyNavyDark
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar with Close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trip Summary",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_summary")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Summary",
                                tint = Color.White
                            )
                        }
                    }

                    // Hero Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ConvoySurfaceDark)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ConvoyPrimaryCyan.copy(alpha = 0.15f),
                                            ConvoySurfaceDark
                                        )
                                    )
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ConvoyAccentGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = ConvoyAccentGreen,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Text(
                                    text = if (summary.isCompletedByCreator) "Convoy Trip Completed!" else "Left Convoy Trip",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = summary.tripTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ConvoyPrimaryCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                                    .format(Date(summary.endedAtMillis))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Route Points Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ConvoySurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ConvoyAccentGreen)
                                )
                                Text(
                                    text = summary.originName.ifBlank { "Origin" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(14.dp)
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ConvoyPrimaryCyan)
                                )
                                Text(
                                    text = summary.destinationName.ifBlank { "Destination" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 3 Key Stats: Distance, Duration, Average Speed
                    Text(
                        text = "CORE PERFORMANCE METRICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                    )

                    // 1. Total Distance Traveled
                    MetricStatCard(
                        icon = Icons.Default.Route,
                        iconTint = ConvoyPrimaryCyan,
                        title = "Total Distance Traveled",
                        value = formatDistance(summary.totalDistanceKm),
                        subtext = "Route covered by convoy",
                        testTag = "stat_total_distance"
                    )

                    // 2. Total Time in Convoy
                    MetricStatCard(
                        icon = Icons.Default.Timer,
                        iconTint = ConvoyAccentGreen,
                        title = "Total Time in Convoy",
                        value = formatDuration(summary.totalDurationSeconds),
                        subtext = "Active convoy duration",
                        testTag = "stat_total_time"
                    )

                    // 3. Average Speed
                    MetricStatCard(
                        icon = Icons.Default.Speed,
                        iconTint = ConvoyWarningAmber,
                        title = "Average Speed",
                        value = "${String.format(Locale.US, "%.1f", summary.averageSpeedKmh)} km/h",
                        subtext = "Fleet cruising pace",
                        testTag = "stat_avg_speed"
                    )

                    // Secondary Stats Row (Peak Speed & Vehicles)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryStatCard(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = "Peak Speed",
                            value = "${String.format(Locale.US, "%.1f", summary.maxSpeedKmh)} km/h",
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryStatCard(
                            icon = Icons.Default.Group,
                            title = "Convoy Size",
                            value = "${summary.participantCount} ${if (summary.participantCount == 1) "Vehicle" else "Vehicles"}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareText = buildString {
                                    append("🚗 Convoy Trip Summary: ${summary.tripTitle}\n")
                                    append("📍 ${summary.originName} ➔ ${summary.destinationName}\n\n")
                                    append("📏 Distance: ${formatDistance(summary.totalDistanceKm)}\n")
                                    append("⏱️ Duration: ${formatDuration(summary.totalDurationSeconds)}\n")
                                    append("⚡ Avg Speed: ${String.format(Locale.US, "%.1f", summary.averageSpeedKmh)} km/h\n")
                                    append("🚀 Max Speed: ${String.format(Locale.US, "%.1f", summary.maxSpeedKmh)} km/h\n")
                                    append("👥 Convoy Members: ${summary.participantCount} vehicles\n\n")
                                    append("Tracked live with Convoy!")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Trip Summary"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_share_summary"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConvoyPrimaryCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Stats")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_done_summary"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConvoyPrimaryBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Done")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtext: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ConvoySurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconTint
                )
            }
        }
    }
}

@Composable
private fun SecondaryStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ConvoySurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ConvoyPrimaryCyan,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
    }
}

private fun formatDistance(distanceKm: Double): String {
    return if (distanceKm < 1.0) {
        val meters = (distanceKm * 1000).toInt()
        "$meters m"
    } else {
        String.format(Locale.US, "%.1f km", distanceKm)
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}
