package com.example.cameraxtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AddLocationActivity : AppCompatActivity() {
    //ImageView for the POI image
    private lateinit var addImage: ImageView
    val poiActivity = PoIActivity()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        addImage = findViewById(R.id.iv_add_image)
        addImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    //show popup to request runtime permission
                    requestPermissions(permissions, PoIActivity.IMAGE_PICK_CODE);
                } else {
                    //permission already granted
                    //poiActivity.pickImageFromGallery(addImage)
                }
            } else {
                //system OS is < Marshmallow
                //pickImageFromGallery()
                //pickImageFromGallery()
            }

        }
    }

}