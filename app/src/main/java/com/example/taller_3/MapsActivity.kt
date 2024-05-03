package com.example.taller_3

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.StrictMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.taller_3.databinding.ActivityMapsBinding
import com.example.taller_3.databinding.ActivityPantalla2Binding
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    //private lateinit var roadManager: RoadManager


    private var userLocationMarker: Marker? = null
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest


    private var observedUserMarker: Marker? = null

    private lateinit var seeingUser: User

    private var settingsOK = false

    private var pos: LatLng = LatLng(0.0, 0.0)


    private val getLocationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        Log.i("LOCATION",
            "Result from settings: ${result.resultCode}")
        if(result.resultCode == RESULT_OK){
            settingsOK = true
            startLocationUpdates()
        }else {

        }
    }

    private val database = FirebaseDatabase.getInstance()

    private lateinit var myRef: DatabaseReference
    private lateinit var auth: FirebaseAuth


    private lateinit var databaseRef : DatabaseReference

    private lateinit var routePolyline: Polyline




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        myRef = database.getReference( "users/${auth.currentUser?.uid}")

        seeingUser = intent.getParcelableExtra<User>("User")!!

        Log.e("LOCATION", "User: ${seeingUser.toMap()}")


        databaseRef = FirebaseDatabase.getInstance().getReference("users/${seeingUser.uid}")



        //var seeingUserDistance = distance(seeingUser.latitude, seeingUser.longitude, pos.latitude, pos.longitude)


        mLocationRequest = createLocationRequest()
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.locations.lastOrNull()?.let { location ->
                    pos = LatLng(location.latitude, location.longitude)}
                for (location in locationResult.locations) {
                    if (location != null) {

                        val newLatLng = LatLng(location.latitude, location.longitude)
                        val minDistanceChange = 30.0 // meters, change this value based on your requirements


                        if (newLatLng.latitude != 0.0 && newLatLng.longitude != 0.0) {
                            if (userLocationMarker == null || distance(userLocationMarker!!.position.latitude, userLocationMarker!!.position.longitude, newLatLng.latitude, newLatLng.longitude)*1000 > minDistanceChange) {
                                if (userLocationMarker == null) {



                                    userLocationMarker = mMap.addMarker(MarkerOptions().position(newLatLng).title("Ubicación Actual").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

                                } else {
                                    userLocationMarker!!.setPosition(newLatLng)



                                }
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(observedUserMarker!!.position, 15f))


                                drawStraightLineBetweenPoints(newLatLng, observedUserMarker!!.position)


                                val updates = hashMapOf<String, Any>(
                                    "latitude" to newLatLng.latitude,
                                    "longitude" to newLatLng.longitude
                                )

                                myRef.updateChildren(updates).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        println("Location updated successfully!")
                                    } else {
                                        println("Failed to update location: ${task.exception?.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }



        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )
            return
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()

        checkLocationSettings()

        mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            Log.i("LOCATION",
                "onSuccess location")
            if (location != null) {
                Log.i("LOCATION", "Longitud: " + location.longitude)
                Log.i("LOCATION", "Latitud: " + location.latitude)
            }
        }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java)
                val longitude = snapshot.child("longitude").getValue(Double::class.java)
                if (latitude != null && longitude != null) {
                    updateMapMarker(latitude, longitude)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("DB_ERROR", "Failed to read value.", error.toException())
            }
        })
    }


    override fun onMapReady(googleMap: GoogleMap) {
        var distancia = 0.0
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
    }



    private fun createLocationRequest(): LocationRequest =
        // New builder
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            Log.i("LOCATION", "GPS is ON")

            settingsOK = true
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if ((e as ApiException).statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                val resolvable = e as ResolvableApiException
                val isr = IntentSenderRequest.Builder(resolvable.resolution).build()
                getLocationSettings.launch(isr)
            }
        }
    }

    fun geoCoderSearchLatLang(context: Context, latLng: LatLng): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // Get a single address
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    return address.getAddressLine(0) ?: "Unknown Location" // Primary address line
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Log the exception
        }
        return "Unknown Location" // Fallback in case of error or no address found
    }

    fun drawStraightLineBetweenPoints(CurrentLocation: LatLng, Target: LatLng){
        val polylineOptions = PolylineOptions()
            .add(CurrentLocation)
            .add(Target)
            .color(Color.RED)
            .width(10f)
        mMap.addPolyline(polylineOptions)
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val RADIUS_OF_EARTH_KM = 6371
        val result = RADIUS_OF_EARTH_KM * c
        return (result * 100.0).roundToInt() / 100.0
    }



    private fun updateMapMarker(latitude: Double, longitude: Double) {

        val latLng = LatLng(seeingUser.latitude, seeingUser.longitude)
        val seeingUserDistance = distance(latitude, longitude, pos.latitude, pos.longitude)

        val newUserLocation = LatLng(latitude, longitude)
        if (observedUserMarker == null) {
            observedUserMarker = mMap.addMarker(MarkerOptions().position(latLng).title("${seeingUser.name} ${seeingUser.lastName}").snippet("Distancia: $seeingUserDistance kms"))
        } else {
            observedUserMarker!!.position = newUserLocation
            observedUserMarker!!.title = "${seeingUser.name} ${seeingUser.lastName}"
            observedUserMarker!!.snippet = "Distancia: $seeingUserDistance kms"
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newUserLocation, 15f))
    }
}