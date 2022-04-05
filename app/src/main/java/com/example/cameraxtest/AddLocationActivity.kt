package com.example.cameraxtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
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

class AddLocationActivity : PoIActivity() {
    //ImageView for the POI image
    private lateinit var addImage: ImageView

    private lateinit var nameEt: EditText
    private lateinit var descriptionEt: EditText

    private lateinit var saveButton: Button

    //Create a database reference
    private lateinit var dbReference: DatabaseReference

    //Connect to the database
    private lateinit var firebaseDatabase: FirebaseDatabase

    //Firebase storage
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var location: LatLng
    private lateinit var passedLocation: DoubleArray
    private lateinit var targetUUID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        addImage = findViewById(R.id.iv_add_image)
        //connect to the database stored at the URL
        firebaseDatabase =
            FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        //Get intent passed from the MainActivity.onMarkerClick
        var intent = intent
        targetUUID = UUID.randomUUID().toString()
        //Get the extra from the intent, which is a UUID of the POI
        passedLocation = intent.getDoubleArrayExtra("location")!!

        location = LatLng(passedLocation.get(0), passedLocation.get(1))

        nameEt = findViewById(R.id.et_name)
        descriptionEt = findViewById(R.id.et_description)
        saveButton = findViewById(R.id.btn_save)

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
                writeNewPOI(name, location, description)
            }
        }
        displayImage(addImage)
        addImage.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                //show popup to request runtime permission
                requestPermissions(permissions, IMAGE_PICK_CODE);
            } else {
                //permission already granted
                pickImageFromGallery()
            }
        }


    }

    companion object {
        //image pick code
        const val IMAGE_PICK_CODE = 1000
    }

    private fun writeNewPOI(name: String, location : LatLng, description: String){
        val intentMainActivity = Intent(this, MainActivity::class.java)
        dbReference = firebaseDatabase.reference
        val poI = PoI(targetUUID, name, location, description)
        val childUpdates = hashMapOf<String, Any>(
            "/POIs/$targetUUID" to poI
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