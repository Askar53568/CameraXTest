package com.example.cameraxtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import coil.load
import java.util.*


open class PoIActivity : AppCompatActivity() {
    //Create a database reference
    private lateinit var dbReference: DatabaseReference

    //Connect to the database
    private lateinit var firebaseDatabase: FirebaseDatabase

    //Textview for the details of the POI
    private lateinit var detailsTv: TextView

    //Textview for the details of the POI
    private lateinit var nameTv: TextView

    //Edit Button
    private lateinit var editButton: Button
    private lateinit var removeButton: Button

    //ImageView for the POI image
    private lateinit var imagePOI: ImageView

    //Camera and Gallery buttons
    private lateinit var galleryButton: Button
    private lateinit var cameraButton: Button

    //Firebase storage
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var targetUUID: String




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi)
        //connect to the database stored at the URL
        firebaseDatabase =
            FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        //Get intent passed from the MainActivity.onMarkerClick
        var intent = getIntent()
        //Get the extra from the intent, which is a UUID of the POI
        if (intent.getStringExtra("uuid") != null) {
            targetUUID = intent.getStringExtra("uuid")!!
        } else {
            targetUUID = UUID.randomUUID().toString()
        }
        pickImageView(findViewById(R.id.image))
        //targetUUID = intent.getStringExtra("uuid")!!
        //Instantiate storage
        storage = FirebaseStorage.getInstance("gs://map-login-57509.appspot.com")
        storageReference = storage.reference
        //Connect the details TextView
        detailsTv = findViewById(R.id.details)
        //Connect to the name TextView
        nameTv = findViewById(R.id.name)
        //Connnect to the edit button
        editButton = findViewById(R.id.edit_button)
        removeButton = findViewById(R.id.remove_button)
        //Connect to the ImageView
//        imagePOI = findViewById(R.id.image)
        //Get details of the POI and display them
        envokePOIListener(targetUUID)
        displayImage(imagePOI)
        //Set the listener for the imageview to be able to change the image
        imagePOI.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
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

        editButton.setOnClickListener {
            val intent = Intent(this, EditLocationActivity::class.java)
            intent.putExtra("uuid", targetUUID)
            startActivity(intent)
            finish()
        }

        removeButton.setOnClickListener{
            removePOI(targetUUID)
        }

    }



    private fun envokePOIListener(uuid: String) {
        //Actual database reference
        dbReference = firebaseDatabase.reference
        //Point at a specific node of the json tree to access the description of the specific POI of UUID ${uuid}
        dbReference = dbReference.child("POIs/${uuid}/description")
        val nameReference = firebaseDatabase.reference.child("POIs/${uuid}/name")
        //Create value listener to display the POI description data
        val DescriptionListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val description = dataSnapshot.getValue<String>()!!
                    val name = dataSnapshot.getValue<String>()!!

                    //Set the description text as the textview
                    detailsTv.text = description
                    //Set the name for the POI
                    nameTv.text = name
                } else {
                    return
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@PoIActivity, "Failed to load description", Toast.LENGTH_LONG)
                    .show()
            }
        }
        val NameListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val name = dataSnapshot.getValue<String>()!!
                    //Set the name for the POI
                    nameTv.text = name
                } else {
                    return
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@PoIActivity, "Failed to load name", Toast.LENGTH_LONG)
                    .show()
            }
        }
        //Attach the value listener to the database reference
        dbReference.addValueEventListener(DescriptionListener)
        nameReference.addValueEventListener(NameListener)


    }

    companion object {
        //image pick code
        val IMAGE_PICK_CODE = 1000

        //Permission code
        val PERMISSION_CODE = 1001
    }
    protected fun pickImageView(imageView: ImageView){
        imagePOI = imageView
    }
    public open fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        resultLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private var resultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            var imageURI = data?.data
            this.imagePOI.setImageURI(imageURI)
            imageURI?.let {
                uploadImage(it)
            }
        }
    }


    //Download image from the storage and display it in the image view
    protected fun displayImage(imageView: ImageView) {
        //Get the reference to the image
        val imagePOIref: StorageReference = storageReference.child("images/" + this.targetUUID)
        imagePOIref.downloadUrl.addOnSuccessListener { imageView.load(it) }.addOnFailureListener {
            Toast.makeText(this, "Error downloading image", Toast.LENGTH_SHORT)
        }
    }

    private fun uploadImage(imageUri: Uri) {
        //Set the reference to the image
        val imagePOIref: StorageReference = storageReference.child("images/" + this.targetUUID)
        //Upload image
        var uploadTask = imagePOIref.putFile(imageUri)

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener {
            Toast.makeText(this@PoIActivity, "Failed to upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { taskSnapshot ->
            Toast.makeText(this@PoIActivity, "Succesful upload", Toast.LENGTH_LONG).show()
        }.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            Toast.makeText(this@PoIActivity, "Upload is $progress% done", Toast.LENGTH_LONG).show()
        }.addOnPausedListener {
            Toast.makeText(this@PoIActivity, "Upload is paused", Toast.LENGTH_LONG).show()
        }

    }

    private fun removePOI(uuid: String){
        val intentMainActivity = Intent(this, MainActivity::class.java)
        val imagePOIref: StorageReference = storageReference.child("images/" + targetUUID)
        dbReference = firebaseDatabase.reference
        dbReference = dbReference.child("/POIs/$uuid")
        dbReference.removeValue()
        imagePOIref.delete()
        startActivity(intentMainActivity)
        finish()
    }

}
