package com.example.cameraxtest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxtest.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    //Maps
    private lateinit var mMap: GoogleMap
    private lateinit var listView: ListView
    private lateinit var binding: ActivityMainBinding
    private lateinit var POIs : MutableList<PoI>

    private lateinit var auth: FirebaseAuth

    private lateinit var logoutBtn: Button
    private lateinit var updatePass: Button

    //Real-time Database
    private lateinit var dbReference: DatabaseReference
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseDatabase = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")

        executeMap()

        auth = FirebaseAuth.getInstance()

        if(auth.currentUser == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show()
        }

        logoutBtn = findViewById(R.id.logout_btn)
        updatePass = findViewById(R.id.update_pass_btn)

        logoutBtn.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        updatePass.setOnClickListener{
            val intent = Intent(this, UpdatePassword::class.java)
            startActivity(intent)
        }
        executeDatabase()
        writeNewPOI("Koktobe", LatLng(43.242013, 76.959379), "description")
        writeNewPOI("Swansea Uni", LatLng(51.619174, -3.880502), "uni")
    }
    private fun executeMap(){
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun executeDatabase() {
        dbReference = firebaseDatabase.reference
        dbReference = dbReference.child("users")
        val myRef = firebaseDatabase.getReference("message")
        val favourites = listOf("Swansea", "Neath")
        writeNewUserAuth(favourites)
    }

    private fun writeNewPOI(name: String, location : LatLng, description: String){
        dbReference = firebaseDatabase.reference
        val uuid = UUID.randomUUID().toString()
        val poI = PoI(uuid, name, location, description)
        val childUpdates = hashMapOf<String, Any>(
            "/POIs/$uuid" to poI
        )
        dbReference.updateChildren(childUpdates)

    }

    /*
    private fun writeNewUser(username: String, favourite: String) {
        val key = dbReference.child("users").push().key
        if (key == null) {
            Log.w(TAG, "Couldn't get push key for users")
            return
        }

        val user = UserInfo(username, favourite)
        val userValues = user.toMap()

        val childUpdates = hashMapOf<String, Any>(
            "/posts/$key" to userValues,
            "/user-posts/$username/$key" to userValues
        )

        dbReference.updateChildren(childUpdates)
    }

     */

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
        dbReference = firebaseDatabase.reference
        dbReference = dbReference.child("POIs")
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.key!!)
                val poi = PoI(
                    dataSnapshot.key!!,
                    dataSnapshot.child("name").getValue<String>()!!,
                    LatLng(
                        dataSnapshot.child("location/latitude").getValue<Double>()!!,
                        dataSnapshot.child("location/longitude").getValue<Double>()!!
                    ),
                    dataSnapshot.child("description").getValue<String>()!!
                )

                mMap.addMarker(MarkerOptions().position(poi.location).title(poi.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poi.location,14f))
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

                mMap.addMarker(MarkerOptions().position(poi.location).title(poi.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poi.location,14f))
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.key!!)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.key!!)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException())
                Toast.makeText(this@MainActivity, "Failed to load comments.",
                    Toast.LENGTH_SHORT).show()
            }
        }
        dbReference.addChildEventListener(childEventListener)

    }
}