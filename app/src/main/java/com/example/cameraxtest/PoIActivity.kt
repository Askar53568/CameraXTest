package com.example.cameraxtest

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxtest.databinding.ActivityMainBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue

class PoIActivity : AppCompatActivity() {
    //Create a database reference
    private lateinit var dbReference: DatabaseReference
    //Connect to the database
    private lateinit var firebaseDatabase: FirebaseDatabase
    //Textview for the details of the POI
    private lateinit var detailsTv : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi)
        //connect to the database stored at the URL
        firebaseDatabase = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        //Get intent passed from the MainActivity.onMarkerClick
        var intent = getIntent()
        //Get the extra from the intent, which is a UUID of the POI
        var targetUUID = intent.getStringExtra("uuid")!!

        //Connect the TextView
        detailsTv = findViewById(R.id.details)

        envokePOIListener(targetUUID)

    }
    private fun envokePOIListener(uuid: String){
        //Actual database reference
        dbReference = firebaseDatabase.reference
        //Point at a specific node of the json tree to access the description of the specific POI of UUID ${uuid}
        dbReference = dbReference.child("POIs/${uuid}/description")
        //Create value listener to display the POI description data
        val POIListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val description = dataSnapshot.getValue<String>()!!
                //Set the description text as the textview
                detailsTv.setText(description)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@PoIActivity, "Failed to load description", Toast.LENGTH_LONG).show()
            }
        }
        //Attach the value listener to the database reference
        dbReference.addValueEventListener(POIListener)


    }
}