package com.example.cameraxtest

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*


class CommentActivity : AppCompatActivity() {
    //Refers to nav drawer toggle in the activity's action bar
    private lateinit var toggle: ActionBarDrawerToggle

    //List of all PoIs
    private var fullCommentList = mutableListOf<Comment>()

    //Mutable list to search PoIs
    private var tempCommentList = mutableListOf<Comment>()

    //RecyclerView adapter
    private lateinit var rvAdapter: CommentAdapter

    //This app's Firebase Authentication
    private lateinit var auth: FirebaseAuth

    //This app's Firebase Database & Database ref
    private lateinit var firebaseDb: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference

    private lateinit var toolbar: Toolbar
    private lateinit var saveButton: Button

    private lateinit var commentEt: EditText


    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var rvComments: RecyclerView
    private lateinit var addComment: FloatingActionButton
    private lateinit var userId: String
    private lateinit var targetUUID: String
    private lateinit var commentsReference: DatabaseReference
    private lateinit var favBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        toolbar = findViewById(R.id.toolbar_main)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        rvComments = findViewById(R.id.rv_locations_list)
        addComment = findViewById(R.id.fabAddLocation)
        favBtn = findViewById(R.id.btn_fav)

        favBtn.visibility = View.GONE

        var intent = getIntent()
        //Get the extra from the intent, which is a UUID of the POI
        if (intent.getStringExtra("uuid") != null) {
            targetUUID = intent.getStringExtra("uuid")!!
        } else {
            //Put whatever
        }

        //Sets the Tool Bar as a Support Action Bar
        setSupportActionBar(toolbar)
        //Adds support for back button in search and hamburger menu button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //Creates hamburger menu button, links it to nav drawer
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open_text, R.string.close_text)
        drawerLayout.addDrawerListener(toggle)
        //Toggle is ready
        toggle.syncState()

        //Instance of firebase authentication
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid

        firebaseDb =
            FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = firebaseDb.reference

        //Intent for the login activity
        val loginIntent = Intent(this, LoginActivity::class.java)


        //Handles clicks on nav drawer icons
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_map -> startActivity(Intent(this, MainActivity::class.java))
                R.id.item_sign_out -> {
                    auth.signOut()
                    startActivity(loginIntent)
                    finish()
                }
            }
            //If user signed out, redirect to login page
            if (auth.currentUser == null) {
                startActivity(loginIntent)
                finish()
            }
            true
        }

        //Redirects user to login activity if not logged in
        if (auth.currentUser == null) {
            startActivity(loginIntent)
            finish()
        } else {
            addComment.visibility = View.VISIBLE
        }

        //Sets the RecyclerView's layoutManager to the created layout
        rvComments.layoutManager = LinearLayoutManager(this@CommentActivity)

        firebaseDb =
            FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = firebaseDb.reference

        addComment.setOnClickListener {
            val addCommentActivity = Intent(this, AddCommentActivity::class.java)
            addCommentActivity.putExtra("uuid", targetUUID)
            startActivity(addCommentActivity)
        }
        //Reference to the PoI sub-section of the db
        val commentRef: DatabaseReference = dbRef.child("comments/$targetUUID")

        commentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    //Wipe previous data
                    fullCommentList.clear()
                    for (commentSnapshot in dataSnapshot.children) {
                        //Create a new PoIModel using db info
                        val comment = Comment(
                            commentSnapshot.child("userId").value as String,
                            commentSnapshot.child("content").value as String
                        )
                        //Add poi to poi list
                        fullCommentList.add(comment)
                    }
                    rvAdapter = CommentAdapter(fullCommentList)

                    rvComments.adapter = rvAdapter

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
        //In case Firebase database fails, load empty list into rv
        rvAdapter = CommentAdapter(fullCommentList)
        tempCommentList.addAll(fullCommentList)
    }

    //Adds on click functionality to nav drawer items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        //Inflates the menu search XML
        menuInflater.inflate(R.menu.menu_search, menu)
        val item = menu?.findItem(R.id.search_action)
        val searchView = item?.actionView as SearchView
        //Handle the click event for every item in the list
        //When any text is changed
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            //Submit does nothing, as we update whenever the text changes
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                tempCommentList.clear()

                //Ignore case for searching
                val searchText = newText!!.lowercase(Locale.getDefault())
                if (searchText.isNotEmpty()) {
                    fullCommentList.forEach {
                        //Return items matching entered text
                        if (it.content!!.lowercase(Locale.getDefault()).contains(searchText)) {
                            tempCommentList.add(it)
                        }
                    }
                } else {
                    //If it's empty, return all items
                    tempCommentList.addAll(fullCommentList)
                }
                //Update the Recycler View
                rvAdapter.addList(tempCommentList)

                return false
            }

        })
        return super.onCreateOptionsMenu(menu)
    }
}