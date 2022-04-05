package com.example.cameraxtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class AddLocationActivity : PoIActivity(){
    //Create a database reference
    private lateinit var dbReference: DatabaseReference
    //Connect to the database
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var passedLocation : DoubleArray
    private lateinit var location : LatLng

    //ImageView for the POI image
    private lateinit var addImage: ImageView

    private lateinit var nameEt: EditText
    private lateinit var descriptionEt: EditText

    //Firebase storage
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    private lateinit var saveButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        firebaseDatabase = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        var intent = getIntent()
        //Get the extra from the intent, which is a UUID of the POI
        passedLocation = intent.getDoubleArrayExtra("location")!!

        location = LatLng(passedLocation.get(0),passedLocation.get(1))

        nameEt = findViewById(R.id.et_name)
        descriptionEt = findViewById(R.id.et_description)
        saveButton = findViewById(R.id.btn_save)
        addImage = findViewById(R.id.iv_add_image)

        saveButton.setOnClickListener {
            var name: String = nameEt.text.toString()
            var description: String = descriptionEt.text.toString()

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description)) {
                Toast.makeText(
                    this@AddLocationActivity,
                    "Please fill all the fields",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                writeNewPOI(name,location,description)
            }
        }
        addImage.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                //show popup to request runtime permission
                requestPermissions(permissions, EditLocationActivity.IMAGE_PICK_CODE);
            } else {
                //permission already granted
                pickImageFromGallery()
            }
        }

    }
    private fun writeNewPOI(name: String, location : LatLng, description: String){
        val intentMainActivity = Intent(this, MainActivity::class.java)
        dbReference = firebaseDatabase.reference
        val uuid = UUID.randomUUID().toString()
        val poI = PoI(uuid, name, location, description)
        val childUpdates = hashMapOf<String, Any>(
            "/POIs/$uuid" to poI
        )
        dbReference.updateChildren(childUpdates).addOnSuccessListener {
            startActivity(intentMainActivity)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this@AddLocationActivity, "Failure to edit the POI", Toast.LENGTH_LONG)
                .show()
            startActivity(intentMainActivity)
            finish()
        }
    }
}