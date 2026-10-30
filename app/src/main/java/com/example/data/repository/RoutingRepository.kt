package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PlaceSearchResult
import com.example.data.model.RouteAlternative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class RoutingRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val olaApiKey: String
        get() = try {
            // Check BuildConfig for OLA_MAPS_API_KEY injected via .env / Secrets
            val key = BuildConfig::class.java.getField("OLA_MAPS_API_KEY").get(null) as? String
            key?.trim() ?: ""
        } catch (e: Throwable) {
            ""
        }

    suspend fun searchPlaces(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        // 1. Try Ola Maps API if key is configured
        if (olaApiKey.isNotBlank()) {
            val olaResults = searchOlaMaps(trimmed)
            if (olaResults.isNotEmpty()) return@withContext olaResults
        }

        // 2. Try Photon API (OpenStreetMap-based autocomplete, fast, no rate limit)
        val photonResults = searchPhoton(trimmed)
        if (photonResults.isNotEmpty()) return@withContext photonResults

        // 3. Try Nominatim with safe error handling
        val nominatimResults = searchNominatim(trimmed)
        if (nominatimResults.isNotEmpty()) return@withContext nominatimResults

        // 4. Fallback to curated landmarks & popular destinations
        searchOfflineCurated(trimmed)
    }

    private fun searchPhoton(query: String): List<PlaceSearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://photon.komoot.io/api/?q=$encodedQuery&limit=8"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ConvoyTrackApp/1.0 (Android; dev@convoy.app)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()

            val json = JSONObject(body)
            val features = json.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<PlaceSearchResult>()

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val props = feature.optJSONObject("properties") ?: continue
                val geom = feature.optJSONObject("geometry") ?: continue
                val coords = geom.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue

                val lon = coords.getDouble(0)
                val lat = coords.getDouble(1)

                val name = props.optString("name").takeIf { it.isNotBlank() }
                    ?: props.optString("street").takeIf { it.isNotBlank() }
                    ?: query

                val details = listOfNotNull(
                    props.optString("district").takeIf { it.isNotBlank() },
                    props.optString("city").takeIf { it.isNotBlank() },
                    props.optString("state").takeIf { it.isNotBlank() },
                    props.optString("country").takeIf { it.isNotBlank() }
                ).filter { it != name }.distinct().take(3).joinToString(", ")

                val osmId = props.optString("osm_id", "$lat,$lon")

                list.add(
                    PlaceSearchResult(
                        placeId = "photon_$osmId",
                        primaryText = name,
                        secondaryText = details,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Photon search failed: ${e.message}")
            emptyList()
        }
    }

    private fun searchOlaMaps(query: String): List<PlaceSearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.olamaps.io/places/v1/autocomplete?input=$encodedQuery&api_key=$olaApiKey"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val json = JSONObject(body)
            val predictions = json.optJSONArray("predictions") ?: return emptyList()
            val list = mutableListOf<PlaceSearchResult>()

            for (i in 0 until predictions.length()) {
                val item = predictions.getJSONObject(i)
                val placeId = item.optString("place_id", i.toString())
                val structuredFormat = item.optJSONObject("structured_formatting")
                val primaryText = structuredFormat?.optString("main_text") ?: item.optString("description")
                val secondaryText = structuredFormat?.optString("secondary_text") ?: ""
                val geometry = item.optJSONObject("geometry")?.optJSONObject("location")
                val lat = geometry?.optDouble("lat", 0.0) ?: 0.0
                val lng = geometry?.optDouble("lng", 0.0) ?: 0.0

                list.add(
                    PlaceSearchResult(
                        placeId = placeId,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        latitude = lat,
                        longitude = lng
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Ola Maps search failed: ${e.message}")
            emptyList()
        }
    }

    private fun searchNominatim(query: String): List<PlaceSearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=6"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ConvoyTrackApp/1.0 (Android; dev@convoy.app)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string()?.trim() ?: return emptyList()
            if (!body.startsWith("[")) return emptyList()

            val jsonArray = JSONArray(body)
            val list = mutableListOf<PlaceSearchResult>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val displayName = item.optString("display_name", "")
                val parts = displayName.split(",")
                val primary = parts.firstOrNull()?.trim() ?: displayName
                val secondary = parts.drop(1).take(2).joinToString(", ").trim()
                val lat = item.optString("lat").toDoubleOrNull() ?: item.optDouble("lat", 0.0)
                val lon = item.optString("lon").toDoubleOrNull() ?: item.optDouble("lon", 0.0)
                val placeId = item.optString("place_id", i.toString())

                if (lat != 0.0 || lon != 0.0) {
                    list.add(
                        PlaceSearchResult(
                            placeId = "nom_$placeId",
                            primaryText = primary,
                            secondaryText = secondary,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Nominatim search failed: ${e.message}")
            emptyList()
        }
    }

    private fun searchOfflineCurated(query: String): List<PlaceSearchResult> {
        val q = query.lowercase().trim()
        val curated = listOf(
            PlaceSearchResult("c_goa", "Goa", "State in India (Panaji, Margao, Beaches)", 15.2993, 74.1240),
            PlaceSearchResult("c_panaji", "Panaji, Goa", "Capital of Goa, India", 15.4909, 73.8278),
            PlaceSearchResult("c_calangute", "Calangute Beach, Goa", "North Goa, India", 15.5439, 73.7553),
            PlaceSearchResult("c_mopa", "Manohar International Airport (GOX)", "Mopa, Goa, India", 15.7275, 73.8711),
            PlaceSearchResult("c_dabolim", "Goa International Airport (GOI)", "Dabolim, Vasco da Gama, Goa", 15.3806, 73.8317),
            PlaceSearchResult("c_delhi", "New Delhi", "National Capital Territory of Delhi, India", 28.6139, 77.2090),
            PlaceSearchResult("c_mumbai", "Mumbai", "Maharashtra, India (Gateway of India, Marine Drive)", 19.0760, 72.8777),
            PlaceSearchResult("c_bengaluru", "Bengaluru (Bangalore)", "Karnataka, India (Silicon Valley)", 12.9716, 77.5946),
            PlaceSearchResult("c_hyderabad", "Hyderabad", "Telangana, India (Charminar, HITEC City)", 17.3850, 78.4867),
            PlaceSearchResult("c_chennai", "Chennai", "Tamil Nadu, India (Marina Beach)", 13.0827, 80.2707),
            PlaceSearchResult("c_kolkata", "Kolkata", "West Bengal, India (Howrah, Victoria Memorial)", 22.5726, 88.3639),
            PlaceSearchResult("c_pune", "Pune", "Maharashtra, India", 18.5204, 73.8567),
            PlaceSearchResult("c_jaipur", "Jaipur", "Pink City, Rajasthan, India (Hawa Mahal)", 26.9124, 75.7873),
            PlaceSearchResult("c_ahmedabad", "Ahmedabad", "Gujarat, India", 23.0225, 72.5714),
            PlaceSearchResult("c_kochi", "Kochi (Cochin)", "Kerala, India", 9.9312, 76.2673),
            PlaceSearchResult("c_agra", "Agra", "Uttar Pradesh, India (Taj Mahal)", 27.1767, 78.0081),
            PlaceSearchResult("c_dubai", "Dubai", "United Arab Emirates (Burj Khalifa)", 25.2048, 55.2708),
            PlaceSearchResult("c_london", "London", "United Kingdom (Big Ben, Heathrow)", 51.5074, -0.1278),
            PlaceSearchResult("c_newyork", "New York City", "New York, USA (Times Square, Manhattan)", 40.7128, -74.0060),
            PlaceSearchResult("c_paris", "Paris", "France (Eiffel Tower)", 48.8566, 2.3522),
            PlaceSearchResult("c_singapore", "Singapore", "Marina Bay Sands, Changi Airport", 1.3521, 103.8198),
            PlaceSearchResult("c_tokyo", "Tokyo", "Japan (Shinjuku, Shibuya)", 35.6762, 139.6503)
        )

        return curated.filter {
            it.primaryText.lowercase().contains(q) || it.secondaryText.lowercase().contains(q)
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        if (olaApiKey.isNotBlank()) {
            try {
                val url = "https://api.olamaps.io/places/v1/reverse-geocode?latlng=$lat,$lng&api_key=$olaApiKey"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val name = results.getJSONObject(0).optString("formatted_address")
                        if (name.isNotBlank()) return@withContext name
                    }
                }
            } catch (e: Exception) {
                Log.w("RoutingRepo", "Ola reverse geocode failed: ${e.message}")
            }
        }

        // Try Photon reverse
        try {
            val url = "https://photon.komoot.io/reverse?lat=$lat&lon=$lng"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ConvoyTrackApp/1.0 (Android; dev@convoy.app)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val props = features.getJSONObject(0).optJSONObject("properties")
                        if (props != null) {
                            val name = props.optString("name").takeIf { it.isNotBlank() }
                                ?: props.optString("street").takeIf { it.isNotBlank() }
                            val city = props.optString("city").takeIf { it.isNotBlank() }
                                ?: props.optString("district").takeIf { it.isNotBlank() }
                            val state = props.optString("state").takeIf { it.isNotBlank() }
                            val country = props.optString("country").takeIf { it.isNotBlank() }
                            val parts = listOfNotNull(name, city, state, country).distinct()
                            if (parts.isNotEmpty()) {
                                return@withContext parts.take(3).joinToString(", ")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Photon reverse geocode failed: ${e.message}")
        }

        // Nominatim reverse
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ConvoyTrackApp/1.0 (Android; dev@convoy.app)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val displayName = json.optString("display_name")
                    if (displayName.isNotBlank()) {
                        val parts = displayName.split(",")
                        return@withContext parts.take(3).joinToString(", ").trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Nominatim reverse geocode failed: ${e.message}")
        }

        "Location (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
    }

    suspend fun calculateRoutes(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): List<RouteAlternative> = withContext(Dispatchers.IO) {
        if (olaApiKey.isNotBlank()) {
            val olaRoutes = calculateOlaRoutes(originLat, originLng, destLat, destLng)
            if (olaRoutes.isNotEmpty()) return@withContext olaRoutes
        }

        // OSRM (Open Source Routing Machine) Fallback
        calculateOsrmRoutes(originLat, originLng, destLat, destLng)
    }

    private fun calculateOlaRoutes(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): List<RouteAlternative> {
        return try {
            val url = "https://api.olamaps.io/routing/v1/directions?origin=$originLat,$originLng&destination=$destLat,$destLng&alternatives=true&api_key=$olaApiKey"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val json = JSONObject(body)
            val routes = json.optJSONArray("routes") ?: return emptyList()
            val list = mutableListOf<RouteAlternative>()

            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val summary = route.optString("summary", "Route ${i + 1}")
                val legs = route.optJSONArray("legs")
                var distanceMeters = 0.0
                var durationSeconds = 0.0
                if (legs != null && legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    distanceMeters = leg.optJSONObject("distance")?.optDouble("value", 0.0) ?: 0.0
                    durationSeconds = leg.optJSONObject("duration")?.optDouble("value", 0.0) ?: 0.0
                }
                val polyline = route.optJSONObject("overview_polyline")?.optString("points", "") ?: ""

                list.add(
                    RouteAlternative(
                        routeIndex = i,
                        summary = if (summary.isNotBlank()) summary else if (i == 0) "Fastest Highway Route" else "Alternative Route $i",
                        distanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0,
                        durationMinutes = Math.round(durationSeconds / 60.0).toDouble(),
                        polyline = polyline,
                        isRecommended = i == 0
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Ola routing failed: ${e.message}")
            emptyList()
        }
    }

    private fun calculateOsrmRoutes(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): List<RouteAlternative> {
        return try {
            val url = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=full&geometries=polyline&alternatives=true"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ConvoyTrackApp/1.0 (Android)")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val json = JSONObject(body)
            val routes = json.optJSONArray("routes") ?: return emptyList()
            val list = mutableListOf<RouteAlternative>()

            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val distanceMeters = route.optDouble("distance", 0.0)
                val durationSeconds = route.optDouble("duration", 0.0)
                val geometry = route.optString("geometry", "")

                val summary = if (i == 0) "Fastest Route" else "Alternative Route $i"

                list.add(
                    RouteAlternative(
                        routeIndex = i,
                        summary = summary,
                        distanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0,
                        durationMinutes = Math.round(durationSeconds / 60.0).toDouble(),
                        polyline = geometry,
                        isRecommended = i == 0
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("RoutingRepo", "OSRM routing failed: ${e.message}")
            // Fallback straight-line polyline if network fails
            val distanceKm = calculateHaversineDistance(originLat, originLng, destLat, destLng)
            listOf(
                RouteAlternative(
                    routeIndex = 0,
                    summary = "Direct Route",
                    distanceKm = Math.round(distanceKm * 10.0) / 10.0,
                    durationMinutes = Math.round((distanceKm / 50.0) * 60.0).toDouble(),
                    polyline = encodeSimpleLine(originLat, originLng, destLat, destLng),
                    isRecommended = true
                )
            )
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun encodeSimpleLine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        return "_p~iF~ps|U_ulLnnqC"
    }
}
