package com.example.cameraxtest

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.cameraxtest.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var favReference: DatabaseReference

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

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    //Continous location update
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    private lateinit var toolbarMain: androidx.appcompat.widget.Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var btnFav: ImageView
    private var favToggle: Boolean = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbarMain = findViewById(R.id.toolbarMain)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)

        //Sets the Tool Bar as a Support Action Bar
        setSupportActionBar(toolbarMain)
        //Adds support for back button in search and hamburger menu button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //Creates hamburger menu button, links it to nav drawer
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open_text, R.string.close_text)
        drawerLayout.addDrawerListener(toggle)
        //Toggle is ready
        toggle.syncState()
        btnFav = findViewById(R.id.btn_fav)


        firebaseDatabase =FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
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

        //Handles clicks on nav drawer icons
        navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_item_main -> startActivity(Intent(this, ListActivity::class.java))
                R.id.item_sign_out -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            //If user signed out, redirect to login page
            if(auth.currentUser == null){
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            true
        }

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show()
        }

        btnFav.setOnClickListener {
            favToggle = !favToggle
            if (favToggle) {
                btnFav.setImageResource(R.drawable.ic_fav_filled_24)
                displayFavourites()
            } else {
                btnFav.setImageResource(R.drawable.ic_fav_24)
                displayAllPOIs()
            }
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
    }

    private fun displayFavourites() {
        mMap.clear()
        val latlng = LatLng(currentLocation?.latitude!!, currentLocation?.longitude!!)
        drawMarker(latlng)
        favReference.addChildEventListener(setFavouritesListener())
    }

    //Adds on click functionality to nav drawer items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
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
        favReference = firebaseDatabase.reference.child("favourites/${auth.currentUser!!.uid!!}")
        val latlng = LatLng(currentLocation?.latitude!!, currentLocation?.longitude!!)
        drawMarker(latlng)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentMarker.position, 14f))

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
                val uuid = UUID.randomUUID().toString()
                intent.putExtra("uuid",uuid)
                startActivity(intent)
                finish()
            }
        })
        displayAllPOIs()

        mMap.setOnMarkerClickListener(this)



    }
    private fun setFavouritesListener(): ChildEventListener{
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val fav = Favourite(
                    dataSnapshot.key!!,
                    dataSnapshot.child("name").getValue<String>()!!,
                    LatLng(
                        dataSnapshot.child("location/latitude").getValue<Double>()!!,
                        dataSnapshot.child("location/longitude").getValue<Double>()!!
                    ),
                    dataSnapshot.child("description").getValue<String>()!!
                )
                val newFavourite: MarkerOptions =
                    MarkerOptions().position(fav.location).title(fav.name).icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_ORANGE
                        )
                    )
                mMap.addMarker(newFavourite).tag = fav.uuid

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildChanged: ${dataSnapshot.key}")

                val fav = Favourite(
                    dataSnapshot.key!!,
                    dataSnapshot.child("name").getValue<String>()!!,
                    LatLng(
                        dataSnapshot.child("location/latitude").getValue<Double>()!!,
                        dataSnapshot.child("location/longitude").getValue<Double>()!!
                    ),
                    dataSnapshot.child("description").getValue<String>()!!
                )
                val newFavourite: MarkerOptions =
                    MarkerOptions().position(fav.location).title(fav.name).icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_ORANGE
                        )
                    )
                mMap.addMarker(newFavourite).tag = fav.uuid
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.key!!)
                Toast.makeText(this@MainActivity, "Favourite POI removed", Toast.LENGTH_LONG).show()
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.key!!)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Cancelled", databaseError.toException())
            }
        }
        return childEventListener
    }

    private fun displayAllPOIs(){
        mMap.clear()
        val latlng = LatLng(currentLocation?.latitude!!, currentLocation?.longitude!!)
        drawMarker(latlng)
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

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildChanged: ${dataSnapshot.key}")

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
        dbReference.addChildEventListener(childEventListener)
    }

    private fun drawMarker(position: LatLng) {
        var marker: MarkerOptions = MarkerOptions().position(position).title("current location")
        //Add marker to the map
        currentMarker = mMap.addMarker(marker)
        currentMarker?.showInfoWindow()
    }



    override fun onMarkerClick(marker: Marker): Boolean {
        var passedLocation: DoubleArray = DoubleArray(2)
        passedLocation.set(0, marker.position.latitude)
        passedLocation.set(1, marker.position.longitude)
        if (marker.tag == null) {
            val intent = Intent(this, AddLocationActivity::class.java)
            intent.putExtra("location", passedLocation)
            val uuid = UUID.randomUUID().toString()
            intent.putExtra("uuid",uuid)
            startActivity(intent)
        } else {
            val intent = Intent(this, PoIActivity::class.java)
            intent.putExtra("uuid", marker.tag.toString())
            intent.putExtra("location", passedLocation)
            startActivity(intent)
            return true;
        }
        return true
    }


}