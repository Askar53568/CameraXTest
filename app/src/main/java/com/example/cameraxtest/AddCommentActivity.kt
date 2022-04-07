package com.example.cameraxtest

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*


class AddCommentActivity: AppCompatActivity() {
    private lateinit var commentListRef: DatabaseReference
    private lateinit var toolbar: Toolbar
    private lateinit var userId: String
    private lateinit var saveButton: Button
    private lateinit var commentEt: EditText
    private lateinit var commentsReference: DatabaseReference
    private lateinit var targetUUID: String
    //This app's Firebase Authentication
    private lateinit var auth: FirebaseAuth
    //This app's Firebase Database & Database ref
    private lateinit var firebaseDb: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference
    private var commentList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_comment)
        var intent = getIntent()
        targetUUID = intent.getStringExtra("uuid")!!
        toolbar = findViewById(R.id.toolbar)
        commentEt = findViewById(R.id.et_comment)
        //Sets the Tool Bar as a Support Action Bar
        setSupportActionBar(toolbar)
        //Adds support for back button in search and hamburger menu button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        saveButton = findViewById(R.id.btn_save)

        //Instance of firebase authentication
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid

        firebaseDb = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = firebaseDb.reference
        commentListRef = dbRef.child("comments")

        //Intent for the login activity
        val loginIntent = Intent(this, LoginActivity::class.java)


        //Redirects user to login activity if not logged in
        if(auth.currentUser == null){
            startActivity(loginIntent)
            finish()
        }

        firebaseDb = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = firebaseDb.reference

        saveButton.setOnClickListener {
            var content: String = commentEt.text.toString()

            if (TextUtils.isEmpty(content)) {
                Toast.makeText(
                    this@AddCommentActivity,
                    "Please fill all the fields",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                addComment(content)
            }
        }

    }


    private fun addComment(content: String){
        val intentCommentActivity = Intent(this, CommentActivity::class.java)
        intentCommentActivity.putExtra("uuid", targetUUID)
        commentsReference = firebaseDb.reference
        val uuid = UUID.randomUUID().toString()
        val comment = Comment(userId,content)
        val childUpdates = hashMapOf<String, Any>(
            "/comments/$targetUUID/$uuid" to comment
        )
        commentsReference.updateChildren(childUpdates).addOnSuccessListener {
            startActivity(intentCommentActivity)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this@AddCommentActivity, "Failure", Toast.LENGTH_LONG)
                .show()
            startActivity(intentCommentActivity)
            finish()
        }
    }
}