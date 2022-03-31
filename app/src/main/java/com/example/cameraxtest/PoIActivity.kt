package com.example.cameraxtest

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*

class PoIActivity : AppCompatActivity() {
    //Create a database reference
    private lateinit var dbReference: DatabaseReference

    //Connect to the database
    private lateinit var firebaseDatabase: FirebaseDatabase

    //Textview for the details of the POI
    private lateinit var detailsTv: TextView

    //ImageView for the POI image
    private lateinit var imagePOI: ImageView

    //Camera and Gallery buttons
    private lateinit var galleryButton: Button
    private lateinit var cameraButton: Button
    //Image URI
    //private var imageURI: Uri? = null
    //Firebase storage
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var targetUUID: String

    private lateinit var storagePOIimageref: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi)
        //connect to the database stored at the URL
        firebaseDatabase =
            FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        //Get intent passed from the MainActivity.onMarkerClick
        var intent = getIntent()
        //Get the extra from the intent, which is a UUID of the POI
        targetUUID = intent.getStringExtra("uuid")!!
        //Instantiate storage
        storage = FirebaseStorage.getInstance("gs://map-login-57509.appspot.com")
        storageReference = storage.reference

        storagePOIimageref = storage.getReferenceFromUrl("gs://map-login-57509.appspot.com/images" + targetUUID)
        //Connect the TextView
        detailsTv = findViewById(R.id.details)
        //Connect to the ImageView
        imagePOI = findViewById(R.id.image)
        //Get details of the POI and display them
        envokePOIListener(targetUUID)
        Glide.with(this@PoIActivity)
            .load(storagePOIimageref)
            .into(imagePOI)
        //Set the listener for the imageview to be able to change the image
        imagePOI.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    //show popup to request runtime permission
                    requestPermissions(permissions, IMAGE_PICK_CODE);
                } else {
                    //permission already granted
                    pickImageFromGallery()
                }
            } else {
                //system OS is < Marshmallow
                //pickImageFromGallery()
                pickImageFromGallery()
            }

        }

    }

    private fun envokePOIListener(uuid: String) {
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
                Toast.makeText(this@PoIActivity, "Failed to load description", Toast.LENGTH_LONG)
                    .show()
            }
        }
        //Attach the value listener to the database reference
        dbReference.addValueEventListener(POIListener)


    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000

        //Permission code
        private val PERMISSION_CODE = 1001
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size >0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }
                else{
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            var imageURI = data?.data
            imagePOI.setImageURI(imageURI)
            //imagePOI.setImageURI(data?.data)
            imageURI?.let { uploadImage(it) }
        }
    }

    private fun uploadImage(imageUri : Uri){
        var progressBar : ProgressBar
        // Create a reference to "mountains.jpg"
        val imagePOIref: StorageReference = storageReference.child("images/"+targetUUID)

        var uploadTask = imagePOIref.putFile(imageUri)

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener {
            Toast.makeText(this@PoIActivity, "Failed to upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { taskSnapshot ->
            Toast.makeText(this@PoIActivity, "Succesful upload", Toast.LENGTH_LONG).show()
        }.addOnProgressListener{ taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            Toast.makeText(this@PoIActivity, "Upload is $progress% done", Toast.LENGTH_LONG).show()
        }.addOnPausedListener {
            Toast.makeText(this@PoIActivity, "Upload is paused", Toast.LENGTH_LONG).show()
        }

    }
}
