package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Trip
import com.example.data.model.VehicleLocation
import org.json.JSONArray
import org.json.JSONObject

class MapBridge(
    private val onMapClick: (Double, Double) -> Unit,
    private val onVehicleClick: (String) -> Unit
) {
    @JavascriptInterface
    fun onMapClicked(lat: Double, lng: Double) {
        onMapClick(lat, lng)
    }

    @JavascriptInterface
    fun onVehicleClicked(vehicleId: String) {
        onVehicleClick(vehicleId)
    }

    @JavascriptInterface
    fun onMapReady() {
        Log.d("MapBridge", "Leaflet map initialized successfully")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    vehicles: List<VehicleLocation>,
    currentTrip: Trip?,
    selectedAlternativePolyline: String? = null,
    onMapClick: (Double, Double) -> Unit = { _, _ -> },
    onVehicleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMapLoaded by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("LeafletMapJS", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} line ${consoleMessage?.lineNumber()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isMapLoaded = true
                    view?.evaluateJavascript("if (typeof map !== 'undefined') { map.invalidateSize(); }", null)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    Log.e("LeafletMapView", "Error loading map: ${error?.description}")
                }
            }
            addJavascriptInterface(MapBridge(onMapClick, onVehicleClick), "AndroidBridge")
            loadUrl("file:///android_asset/leaflet_map.html")
        }
    }

    // Sync vehicles to map
    LaunchedEffect(vehicles, isMapLoaded) {
        if (!isMapLoaded) return@LaunchedEffect
        val jsonArray = JSONArray()
        vehicles.forEach { v ->
            val obj = JSONObject().apply {
                put("vehicleId", v.vehicleId)
                put("driverName", v.driverName)
                put("vehicleName", v.vehicleName)
                put("vehicleColor", v.vehicleColor)
                put("latitude", v.latitude)
                put("longitude", v.longitude)
                put("speedKmh", v.speedKmh)
                put("heading", v.heading)
                put("status", v.status)
            }
            jsonArray.put(obj)
        }
        val js = "if (typeof updateVehicles === 'function') { updateVehicles('${jsonArray.toString()}'); }"
        webView.evaluateJavascript(js, null)
    }

    // Sync trip endpoints and route
    LaunchedEffect(currentTrip, selectedAlternativePolyline, isMapLoaded) {
        if (!isMapLoaded) return@LaunchedEffect
        if (currentTrip != null) {
            val polyline = selectedAlternativePolyline?.ifBlank { currentTrip.selectedRoutePolyline }
                ?: currentTrip.selectedRoutePolyline

            val endpointsJs = "if (typeof setTripEndpoints === 'function') { setTripEndpoints(" +
                    "${currentTrip.originLat}, ${currentTrip.originLng}, '${currentTrip.originName.replace("'", "\\'")}', " +
                    "${currentTrip.destinationLat}, ${currentTrip.destinationLng}, '${currentTrip.destinationName.replace("'", "\\'")}'); }"
            webView.evaluateJavascript(endpointsJs, null)

            val routeJs = "if (typeof setTripRoute === 'function') { setTripRoute('$polyline', null); }"
            webView.evaluateJavascript(routeJs, null)

            val fitJs = "if (typeof fitAll === 'function') { fitAll(); }"
            webView.evaluateJavascript(fitJs, null)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                webView.evaluateJavascript("if (typeof map !== 'undefined') { map.invalidateSize(); }", null)
            }
    )
}
