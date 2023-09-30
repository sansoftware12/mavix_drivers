package com.example.mavix_drivers

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var driversReference: DatabaseReference
    private var driverMarkers: HashMap<String, Marker> = HashMap()
    private var nearestUserMarker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        firebaseDatabase = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }


    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getCurrentLocation() {
        if (isLocationPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.lastLocation
                .addOnCompleteListener(this, OnCompleteListener<Location> { task ->
                    if (task.isSuccessful && task.result != null) {
                        val location = task.result
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val locationData = hashMapOf(
                                "latitude" to latitude,
                                "longitude" to longitude
                            )

                            val databaseReference = firebaseDatabase.reference.child("driver_locations")
                            databaseReference.child(userId).setValue(locationData)
                                .addOnSuccessListener {
                                    showToast("Location data sent to Firebase")
                                }
                                .addOnFailureListener { e ->
                                    showToast("Error sending location data: ${e.message}")
                                }
                        }

                        val markerOptions = MarkerOptions()
                            .position(LatLng(latitude, longitude))
                            .title("My Location")
                        mMap.addMarker(markerOptions)
                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 12f)
                        mMap.moveCamera(cameraUpdate)

                        // Display nearest user marker
                        displayNearestUserMarker(location)
                    } else {
                        showToast("Error getting location")
                    }
                })
        }
    }

    private fun displayNearestUserMarker(driverLocation: Location) {
        val userLocationsReference = firebaseDatabase.reference.child("user_locations")
        userLocationsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var nearestUser: Marker? = null
                var nearestUserDistance = Float.MAX_VALUE

                for (userSnapshot in dataSnapshot.children) {
                    val latitude = userSnapshot.child("latitude").value as? Double
                    val longitude = userSnapshot.child("longitude").value as? Double
                    val userId = userSnapshot.key // Get the user's ID

                    if (latitude != null && longitude != null && userId != null) {
                        val userLocation = Location("")
                        userLocation.latitude = latitude
                        userLocation.longitude = longitude

                        val distance = driverLocation.distanceTo(userLocation)
                        if (distance < nearestUserDistance) {
                            nearestUserDistance = distance

                            if (nearestUser != null) {
                                nearestUser.remove()
                            }
                            nearestUser = mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(latitude, longitude))
                                    .title("Nearest User")
                                    .snippet("Distance: ${String.format("%.2f", distance)} meters")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                            )

                            // Pass the user ID and the distance to retrieve user information and update snippet
                            retrieveUserInformationAndUpdateSnippet(nearestUser, distance, userId)
                        }
                    }
                }

                // Check if nearestUser is not null before calling retrieveUserInformationAndUpdateSnippet
                if (nearestUser != null) {
                    // Implement the function to handle the marker and other details
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Error retrieving user locations")
            }
        })
    }

    private fun retrieveUserInformationAndUpdateSnippet(marker: Marker?, distance: Float, userId: String) {
        // Check if the marker is not null before proceeding
        if (marker != null) {
            val usersReference = firebaseDatabase.reference.child("users").child(userId)
            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val email = userSnapshot.child("email").value as? String
                    val phone = userSnapshot.child("phone").value as? String

                    if (email != null && phone != null) {
                        // Call showUserLocation with the correct parameters
                        showUserLocation(marker, distance, email, phone)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    showToast("Error retrieving user information")
                }
            })
        }
    }

    private fun showUserLocation(marker: Marker, distance: Float, email: String, phone: String) {
        // Use the provided marker and update its snippet
        val snippet = "Distance: ${String.format("%.2f", distance)} meters\nEmail: $email\nPhone: $phone"
        marker.snippet = snippet

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                showToast("Location permission denied")
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true

        // Check if location permission is granted
        if (isLocationPermissionGranted()) {
            // Enable the "My Location" layer on the map
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling ActivityCompat#requestPermissions here to request the missing permissions
                return
            }
            mMap.isMyLocationEnabled = true

            // Call getCurrentLocation() to get the driver's current location
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove user markers from the map
        for (marker in driverMarkers.values) {
            marker.remove()
        }
        driverMarkers.clear()
    }
}