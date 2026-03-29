package dev.jovanni0.itec19.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import dev.jovanni0.itec19.stores.DrawingStore

data class PosterLocation(
    val posterId: String,
    val lat: Double,
    val lng: Double
)

val posterLocations = listOf(
    PosterLocation("afis1",  45.7537, 21.2257),
    PosterLocation("afis2",  45.7579, 21.2290),
    PosterLocation("afis3",  45.7661, 21.2291),
    PosterLocation("afis4",  45.7476, 21.2312),
    PosterLocation("afis5",  45.7495, 21.2325),
    PosterLocation("afis6",  45.7505, 21.2244),
    PosterLocation("afis7",  45.7575, 21.2340),
    PosterLocation("afis8",  45.7503, 21.2096),
    PosterLocation("afis9",  45.7408, 21.2427),
    PosterLocation("afis10", 45.7550, 21.2300),
)

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locationGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.7537, 21.2257), 15f)
    }

    LaunchedEffect(locationGranted) {
        if (locationGranted) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(it.latitude, it.longitude), 17f
                        )
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("MAP_ERROR", "Permission error", e)
            }
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationGranted),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = locationGranted,
                zoomControlsEnabled = false
            )
        ) {
            posterLocations.forEach { poster ->
                val dominantTeam = DrawingStore.dominance[poster.posterId]
                val hue = when (dominantTeam?.name) {
                    "RED"   -> BitmapDescriptorFactory.HUE_RED
                    "GREEN" -> BitmapDescriptorFactory.HUE_GREEN
                    "BLUE"  -> BitmapDescriptorFactory.HUE_BLUE
                    else    -> BitmapDescriptorFactory.HUE_ORANGE
                }

                Marker(
                    state = rememberMarkerState(position = LatLng(poster.lat, poster.lng)),
                    title = poster.posterId,
                    icon = BitmapDescriptorFactory.defaultMarker(hue),
                    onInfoWindowClick = {
                        val intent = Intent(context, dev.jovanni0.itec19.PosterDetailActivity::class.java).apply {
                            putExtra("poster_name", poster.posterId)
                            putExtra("read_only", true)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}