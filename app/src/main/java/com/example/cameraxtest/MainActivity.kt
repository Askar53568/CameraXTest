package com.example.cameraxtest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var logoutBtn: Button
    private lateinit var updatePass: Button

    //Real-time Database
    private lateinit var dbReference: DatabaseReference
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
    }

    private fun executeDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbReference = firebaseDatabase.reference
        dbReference = dbReference.child("qq")
        val myRef = firebaseDatabase.getReference("message")
        val userRef = firebaseDatabase.getReference("users")

        writeNewUser("newUser1", "Swansea Bay")




        myRef.setValue("Hello, World!")

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val map = dataSnapshot.value as Map<String, Any>?


                //val value = dataSnapshot.getValue<String>()
                Log.d(ContentValues.TAG, "Value is: ${map}")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun writeNewUser(username: String, favourite: String) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        val key = dbReference.child("users").push().key
        if (key == null) {
            Log.w(TAG, "Couldn't get push key for posts")
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
}