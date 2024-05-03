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
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.taller_3.databinding.ActivityPantalla2Binding
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
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


class Pantalla2 : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private lateinit var roadManager: RoadManager


    private var userLocationMarker: Marker? = null
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityPantalla2Binding

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest

    private var settingsOK = false

    private var pos: LatLng = LatLng(0.0, 0.0)

    private var localizaciones = JSONArray()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPantalla2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!

        roadManager = OSRMRoadManager(this, "ANDROID")
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)




        val JSONFromAsset = loadJSONFromAsset()?.let { JSONObject(it) }


       if (JSONFromAsset != null) {
            localizaciones = JSONFromAsset.getJSONArray("locationsArray")
        }



        myRef = database.getReference( "users/${auth.currentUser?.uid}")



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
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 15f))

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

        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Only attempt to change map style if mMap has been initialized
                if (this@Pantalla2::mMap.isInitialized) {
                    try {
                       // val styleId = if (event.values[0] < 5000) R.raw.dark else R.raw.light
                       // mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@Mapa, styleId))
                    } catch (e: Resources.NotFoundException) {
                        Log.e("MAP_STYLE", "Can't find style. Error: ", e)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
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


        setSupportActionBar(findViewById(R.id.my_toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)


    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.app_bar_switch)  // Encuentra el ítem del menú.
        val switchView = menuItem?.actionView as? Switch  // Obtiene el Switch definido en el actionLayout.
        switchView?.let {
            it.isChecked =  true // Modifica el estado del Switch.
            it.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "Cambiando disponibilidad...", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuLogOut -> {
                logout()
                true
            }
            R.id.listaDisponibles -> {
                showAvailableList()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {

        auth.signOut()

        val intent = Intent(this,MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
    }

    private fun toggleAvailability() {
     /*val updates = hashMapOf<String, Any>(
            "disponible" to newLatLng.latitude,
        )

        myRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("Location updated successfully!")
            } else {
                println("Failed to update location: ${task.exception?.message}")
            }
        }*/

    }

    private fun showAvailableList() {
        val intent = Intent(this,ListaDisponibles::class.java)
        startActivity(intent)
        Toast.makeText(this, "Mostrando lista de disponibles...", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.header_menu, menu)
        val switchItem = menu.findItem(R.id.app_bar_switch)
        val switchLayout = switchItem.actionView as RelativeLayout // Obtiene el RelativeLayout.
        val switchView = switchLayout.findViewById<Switch>(R.id.my_switch) // Encuentra el Switch dentro del RelativeLayout.

        // Obtener el usuario actual de Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            // Referencia a la base de datos para obtener el usuario
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            // Leer el valor de 'disponible' del usuario
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val disponible = dataSnapshot.child("disponible").getValue(Boolean::class.java) ?: false
                        // Establecer el estado del Switch basado en el valor de 'disponible'
                        switchView.isChecked = disponible
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("Firebase", "loadUser:onCancelled", databaseError.toException())
                }
            })
        } else {
            // Manejar caso donde no hay usuario autenticado
            switchView.isChecked = false // o deshabilitar el switch, dependiendo del caso de uso
        }

        // Listener para cambios en el Switch
        switchView.setOnCheckedChangeListener { _, isChecked ->
            // Opcional: Actualizar el valor en la base de datos cuando el Switch cambie
            currentUser?.let {
                FirebaseDatabase.getInstance().getReference("users").child(it.uid)
                    .child("disponible").setValue(isChecked)
            }

            Toast.makeText(this, "Disponibilidad ${if (isChecked) "encendida" else "apagada"}", Toast.LENGTH_SHORT).show()
        }

        return true
    }


    fun vectorToBitmap(context: Context, drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    class MyLocation(var fecha: Date, var latitud: Double, var
    longitud: Double) {
        fun toJSON(): JSONObject {
            val obj = JSONObject()
            try {
                obj.put("latitud", latitud)
                obj.put("longitud", longitud)
                obj.put("date", fecha)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return obj
        }
    }


    //Leer arichivo JSON
    private fun loadJSONFromAsset(): String? {
        val json: String?
        try {
            val inputStream = assets.open("locations.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charsets.UTF_8)
        } catch (ex: java.io.IOException) {
            ex.printStackTrace()
            Log.e("ERROR", "Error al leer archivo JSON")
            return null
        }
        return json
    }

    override fun onResume() {
        super.onResume()
        if (this::mMap.isInitialized) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        if (this::mMap.isInitialized) {
            sensorManager.unregisterListener(lightSensorListener)
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val mGeocoder = Geocoder(baseContext)
        var distancia = 0.0
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        sensorManager.registerListener(
            lightSensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        for (i in 0 until localizaciones.length()) {
            val item = localizaciones.getJSONObject(i)
            val latitud = item.optDouble("latitude", 0.0) // Usa 0.0 como valor predeterminado
            val longitud = item.optDouble("longitude", 0.0)
            val name = item.optString("name", "Unknown") // Usa "Unknown" como valor predeterminado

            val latLng = LatLng(latitud, longitud)

            Log.w("LOCATION", "Latitud: $latitud, Longitud: $longitud, Name: $name")
            mMap.addMarker(MarkerOptions().position(latLng).title(name))
        }


        mMap.setOnMapLongClickListener { latLng ->
            val address = geoCoderSearchLatLang(this, latLng)
            mMap.addMarker(MarkerOptions().position(latLng).title(address))
            distancia = distance(latLng.latitude, latLng.longitude, pos.latitude, pos.longitude)
            Toast.makeText(this, "Distancia: $distancia kms", Toast.LENGTH_SHORT).show()
            drawRouteCurrentlocationToTarget(pos, latLng)
        }
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

    fun drawRouteCurrentlocationToTarget(CurrentLocation: LatLng, Target: LatLng){

        val roadManager : RoadManager = OSRMRoadManager(this,"ANDROID")

        val start = GeoPoint(CurrentLocation.latitude, CurrentLocation.longitude)
        val end = GeoPoint(Target.latitude, Target.longitude)

        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(start)
        waypoints.add(end)

        val road = roadManager.getRoad(waypoints)

        val latLngRoute = road.mRouteHigh.map {LatLng(it.latitude, it.longitude)}

        val polylineoptions = PolylineOptions().addAll(latLngRoute).color(Color.RED).width(10f)
        mMap.addPolyline(polylineoptions)

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
}