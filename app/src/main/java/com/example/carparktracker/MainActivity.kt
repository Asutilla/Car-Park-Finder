package com.example.carparktracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carparktracker.ui.theme.CarParkTrackerTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var carLocationStore: CarLocationStore

    private var hasLocationPermission by mutableStateOf(false)
    private var showRationaleDialog by mutableStateOf(false)
    private var showPermissionSettingsDialog by mutableStateOf(false)
    private var carLocation by mutableStateOf<LatLng?>(null)
    private var currentLocation by mutableStateOf<LatLng?>(null)

    /** Keeps a tap on "I'm parked here" from being dropped while the permission dialog is up. */
    private var parkAfterPermissionGrant = false
    private var isPermissionRequestInFlight = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionRequestInFlight = false
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted

        if (granted) {
            fetchCurrentLocation(onLocationReceived = { location -> currentLocation = location })
            if (parkAfterPermissionGrant) {
                parkAfterPermissionGrant = false
                updateParkedLocation()
            }
            return@registerForActivityResult
        }

        parkAfterPermissionGrant = false
        if (isPermissionPermanentlyDenied()) {
            // The SDK will no longer show the permission dialog, so explain and link to Settings.
            showPermissionSettingsDialog = true
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_denied_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        carLocationStore = CarLocationStore(
            SharedPreferencesKeyValueStore(getPreferences(MODE_PRIVATE))
        )

        // Restore the parked car so the pin is back after the app is killed and reopened.
        carLocation = carLocationStore.restore()?.toLatLng()

        requestLocationAccessIfNeeded()

        setContent {
            CarParkTrackerTheme {
                if (showRationaleDialog) {
                    AlertDialog(
                        onDismissRequest = { showRationaleDialog = false },
                        title = { Text(text = stringResource(id = R.string.location_permission_title)) },
                        text = { Text(text = stringResource(id = R.string.location_permission_rationale)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showRationaleDialog = false
                                    requestLocationPermissions()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.continue_label))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showRationaleDialog = false }
                            ) {
                                Text(text = stringResource(id = R.string.cancel_label))
                            }
                        }
                    )
                }

                if (showPermissionSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionSettingsDialog = false },
                        title = { Text(text = stringResource(id = R.string.location_settings_title)) },
                        text = { Text(text = stringResource(id = R.string.location_settings_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showPermissionSettingsDialog = false
                                    openAppSettings()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.open_settings_label))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showPermissionSettingsDialog = false }
                            ) {
                                Text(text = stringResource(id = R.string.cancel_label))
                            }
                        }
                    )
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        hasPermission = hasLocationPermission,
                        carLocation = carLocation,
                        currentLocation = currentLocation,
                        onParkHereClicked = {
                            if (hasLocationPermission()) {
                                updateParkedLocation()
                            } else {
                                parkAfterPermissionGrant = true
                                requestLocationAccessIfNeeded()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::fusedLocationClient.isInitialized) return

        // The user may have granted or revoked the permission in system settings.
        val granted = hasLocationPermission()
        val permissionJustGained = granted && !hasLocationPermission
        hasLocationPermission = granted

        if (permissionJustGained && currentLocation == null) {
            fetchCurrentLocation(onLocationReceived = { location -> currentLocation = location })
        }
    }

    /**
     * Requests location access the way the SDK expects: ask outright the first
     * time, show the rationale dialog after a denial, and point at app settings
     * once the permission can no longer be requested.
     */
    private fun requestLocationAccessIfNeeded() {
        if (hasLocationPermission()) {
            hasLocationPermission = true
            fetchCurrentLocation(onLocationReceived = { location -> currentLocation = location })
            return
        }

        hasLocationPermission = false
        when {
            shouldShowRationale() -> showRationaleDialog = true
            !carLocationStore.hasRequestedPermission -> requestLocationPermissions()
            else -> showPermissionSettingsDialog = true
        }
    }

    private fun shouldShowRationale(): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    /** True once the SDK refuses to show a permission dialog any more ("don't ask again"). */
    private fun isPermissionPermanentlyDenied(): Boolean = !shouldShowRationale()

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermissions() {
        if (isPermissionRequestInFlight) return
        isPermissionRequestInFlight = true
        carLocationStore.hasRequestedPermission = true
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(
        onLocationReceived: (LatLng) -> Unit,
        onLocationUnavailable: () -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            onLocationUnavailable()
            return
        }

        // Ask for a fresh fix first so the pin is the spot the user is standing on,
        // then fall back to the last known location when the device has no fix yet.
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationReceived(LatLng(location.latitude, location.longitude))
            } else {
                fetchLastKnownLocation(onLocationReceived, onLocationUnavailable)
            }
        }.addOnFailureListener {
            fetchLastKnownLocation(onLocationReceived, onLocationUnavailable)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocation(
        onLocationReceived: (LatLng) -> Unit,
        onLocationUnavailable: () -> Unit
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(LatLng(location.latitude, location.longitude))
                } else {
                    onLocationUnavailable()
                }
            }
            .addOnFailureListener { onLocationUnavailable() }
    }

    private fun updateParkedLocation() {
        fetchCurrentLocation(
            onLocationReceived = { latLng ->
                carLocation = latLng
                carLocationStore.save(CarCoordinates(latLng.latitude, latLng.longitude))
                Toast.makeText(
                    this,
                    getString(R.string.car_parked_toast),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onLocationUnavailable = {
                Toast.makeText(
                    this,
                    getString(R.string.location_unavailable_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

private fun CarCoordinates.toLatLng(): LatLng = LatLng(latitude, longitude)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    carLocation: LatLng?,
    currentLocation: LatLng?,
    onParkHereClicked: () -> Unit
) {
    val knownLocation = carLocation ?: currentLocation
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            knownLocation ?: LatLng(0.0, 0.0),
            if (knownLocation != null) DEFAULT_ZOOM else WORLD_ZOOM
        )
    }

    // One hoisted state for the pin. rememberMarkerState's key argument is only the
    // save/restore registry key, so it does not recreate the state when the parked
    // location changes; the position has to be updated explicitly instead.
    val carMarkerState = rememberMarkerState(position = carLocation ?: LatLng(0.0, 0.0))

    // Move the pin and follow it when there is one, otherwise centre on the user.
    LaunchedEffect(carLocation) {
        carLocation?.let {
            carMarkerState.position = it
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM))
        }
    }
    LaunchedEffect(currentLocation) {
        val current = currentLocation
        if (carLocation == null && current != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(current, DEFAULT_ZOOM))
        }
    }

    val parkedCarTitle = stringResource(id = R.string.parked_car_title)

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = hasPermission)
        ) {
            // The icon must be built inside the map content: BitmapDescriptorFactory
            // is only usable after the MapView has initialised the Maps SDK.
            val carIcon = rememberCarIcon()

            carLocation?.let {
                Marker(
                    state = carMarkerState,
                    title = parkedCarTitle,
                    icon = carIcon
                )
            }
        }

        Button(
            onClick = onParkHereClicked,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(0.8f)
        ) {
            Text(text = stringResource(id = R.string.im_parked_here))
        }
    }
}

@Composable
private fun rememberCarIcon(): BitmapDescriptor {
    val context = LocalContext.current
    return remember(context) {
        bitmapDescriptorFromVector(context, R.drawable.ic_car)
            ?: BitmapDescriptorFactory.defaultMarker()
    }
}

private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    drawable.setBounds(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private const val DEFAULT_ZOOM = 16f
private const val WORLD_ZOOM = 2f
