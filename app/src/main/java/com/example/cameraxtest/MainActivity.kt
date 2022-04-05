package com.example.cameraxtest

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.cameraxtest.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    //Maps
    private lateinit var mMap: GoogleMap
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null
    private var locationUpdate: Location? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentMarker: Marker

    private lateinit var auth: FirebaseAuth

    private lateinit var logoutBtn: Button
    private lateinit var updatePass: Button

    //Real-time Database
    private lateinit var dbReference: DatabaseReference
    private lateinit var firebaseDatabase: FirebaseDatabase

    //Continous location update
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseDatabase =
            FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest!!.setInterval(20 * 1000)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult === null) {
                    return
                } else {
                    locationUpdate = locationResult.lastLocation
                }
            }
        }
        //Remove last location update
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback as LocationCallback)
        executeMap()

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show()
        }

        logoutBtn = findViewById(R.id.logout_btn)
        updatePass = findViewById(R.id.update_pass_btn)

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        updatePass.setOnClickListener {
            val intent = Intent(this, UpdatePassword::class.java)
            startActivity(intent)
        }
        executeDatabase()
        //writeNewPOI("Park of The First president", LatLng(43.18559019595884, 76.88532945554309), "just a park")
    }

    private fun executeMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED && (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
        }
        val task = fusedLocationProviderClient?.lastLocation
        task?.addOnSuccessListener { location ->
            if (location != null) {
                this.currentLocation = location
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executeMap()
            }
        }
    }

    private fun executeDatabase() {
        dbReference = firebaseDatabase.reference
        dbReference = dbReference.child("users")
        val favourites = listOf("Swansea", "Neath")
        writeNewUserAuth(favourites)
    }

    private fun writeNewUserAuth(favourites: List<String>) {
        val user = FirebaseAuth.getInstance().currentUser!!
        val uId = user.uid

        val childUpdates = hashMapOf<String, Any>(
            "$uId/username" to user.email!!,
            "$uId/favourites" to favourites
        )

        dbReference.updateChildren(childUpdates)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //Zoom in on the current location
        //Get the reference to the node of the realtime database where the poi details are stored
        dbReference = firebaseDatabase.reference
        dbReference = dbReference.child("POIs")
        val latlng = LatLng(currentLocation?.latitude!!, currentLocation?.longitude!!)
        drawMarker(latlng)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentMarker.position, 14f))

        //Display the pois in the realtime database on the map
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val poi = PoI(
                    dataSnapshot.key!!,
                    dataSnapshot.child("name").getValue<String>()!!,
                    LatLng(
                        dataSnapshot.child("location/latitude").getValue<Double>()!!,
                        dataSnapshot.child("location/longitude").getValue<Double>()!!
                    ),
                    dataSnapshot.child("description").getValue<String>()!!
                )
                val newPoi: MarkerOptions =
                    MarkerOptions().position(poi.location).title(poi.name).icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
                mMap.addMarker(newPoi).tag = poi.uuid

                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poi.location,14f))
                // A new comment has been added, add it to the displayed list
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildChanged: ${dataSnapshot.key}")

                val children = dataSnapshot.children
                val poi = PoI(
                    dataSnapshot.key!!,
                    dataSnapshot.child("name").getValue<String>()!!,
                    LatLng(
                        dataSnapshot.child("location/latitude").getValue<Double>()!!,
                        dataSnapshot.child("location/longitude").getValue<Double>()!!
                    ),
                    dataSnapshot.child("description").getValue<String>()!!
                )
                var newPoi: MarkerOptions =
                    MarkerOptions().position(poi.location).title(poi.name).icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
                mMap.addMarker(newPoi).setTag(poi.uuid)

//                mMap.addMarker(MarkerOptions().position(poi.location).title(poi.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poi.location, 14f))
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.key!!)
                Toast.makeText(this@MainActivity, "POI removed", Toast.LENGTH_LONG).show()
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.key!!)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Cancelled", databaseError.toException())
            }
        }
        mMap.setOnPoiClickListener(object : GoogleMap.OnPoiClickListener {
            override fun onPoiClick(poi: PointOfInterest) {

            }
        })
        mMap.setOnMapClickListener { position ->
            if (currentMarker != null) {
                currentMarker.remove()
            }
            val newPosition = LatLng(position.latitude, position.longitude)
            drawMarker(newPosition)
        }

        mMap.setOnMapLongClickListener(object : GoogleMap.OnMapLongClickListener {
            override fun onMapLongClick(marker: LatLng) {
                if (currentMarker != null) {
                    currentMarker.remove()
                }
                val newPosition = LatLng(marker.latitude, marker.longitude)
                drawMarker(newPosition)
                var passedLocation: DoubleArray = DoubleArray(2)
                passedLocation.set(0, marker.latitude)
                passedLocation.set(1, marker.longitude)
                val intent = Intent(this@MainActivity, AddLocationActivity::class.java)
                intent.putExtra("location", passedLocation)
                startActivity(intent)
                finish()
            }
        })
        dbReference.addChildEventListener(childEventListener)

        mMap.setOnMarkerClickListener(this)

    }

    private fun drawMarker(position: LatLng) {
        var marker: MarkerOptions = MarkerOptions().position(position).title("current location")
        //Add marker to the map
        currentMarker = mMap.addMarker(marker)
        currentMarker?.showInfoWindow()
    }


    override fun onMarkerClick(marker: Marker): Boolean {
        if (marker.tag == null) {
            var passedLocation: DoubleArray = DoubleArray(2)
            passedLocation.set(0, marker.position.latitude)
            passedLocation.set(1, marker.position.longitude)
            val intent = Intent(this, AddLocationActivity::class.java)
            intent.putExtra("location", passedLocation)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, PoIActivity::class.java)
            intent.putExtra("uuid", marker.tag.toString())
            startActivity(intent)
            finish()
            return true;
        }
        return true
    }


}