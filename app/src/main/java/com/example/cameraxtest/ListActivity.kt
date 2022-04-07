package com.example.cameraxtest



import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import java.util.*


//Refers to nav drawer toggle in the activity's action bar
private lateinit var toggle: ActionBarDrawerToggle
//List of all PoIs
private var fullPoiList = mutableListOf<PoI>()
//Mutable list to search PoIs
private var tempPoiList = mutableListOf<PoI>()
//RecyclerView adapter
private lateinit var rvAdapter: LocationAdapter
//This app's Firebase Authentication
private lateinit var auth: FirebaseAuth
//This app's Firebase Database & Database ref
private lateinit var firebaseDb: FirebaseDatabase
private lateinit var dbRef: DatabaseReference

private lateinit var toolbar: Toolbar
private lateinit var drawerLayout: DrawerLayout
private lateinit var navView: NavigationView
private lateinit var rvLocations: RecyclerView
//List of all favourite PoIs
private var favPoiList = mutableListOf<PoI>()
//Toggle for showing only favourites
private var favToggle: Boolean = false
private lateinit var btnFav: ImageView


class ListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //Calls parent constructor
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)
        toolbar = findViewById(R.id.toolbar_main)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        rvLocations = findViewById(R.id.rv_locations_list)
        btnFav = findViewById(R.id.btn_fav)

        //Sets the Tool Bar as a Support Action Bar
        setSupportActionBar(toolbar)
        //Adds support for back button in search and hamburger menu button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //Creates hamburger menu button, links it to nav drawer
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open_text, R.string.close_text)
        drawerLayout.addDrawerListener(toggle)
        //Toggle is ready
        toggle.syncState()

        auth = FirebaseAuth.getInstance()


        //Intent for the login activity
        val loginIntent = Intent(this, LoginActivity::class.java)

        //Handles clicks on nav drawer icons
        navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_item_map -> startActivity(Intent(this, MainActivity::class.java))
                R.id.item_sign_out -> {
                    auth.signOut()
                    startActivity(loginIntent)
                    finish()
                }
            }
            //If user signed out, redirect to login page
            if(auth.currentUser == null){
                startActivity(loginIntent)
                finish()
            }
            true
        }

        //Redirects user to login activity if not logged in
        if(auth.currentUser == null){
            startActivity(loginIntent)
            finish()
        }

        //Sets the RecyclerView's layoutManager to the created layout
        rvLocations.layoutManager = LinearLayoutManager(this@ListActivity)

        //Links Firebase db to the db's URL
        firebaseDb = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = firebaseDb.reference
        //Returns either the full list, or the filtered fav list


        //Toggles favourites when button is pressed
        btnFav.setOnClickListener {
            favToggle = !favToggle
            if (favToggle) {
                btnFav.setImageResource(R.drawable.ic_fav_filled_24)
            } else {
                btnFav.setImageResource(R.drawable.ic_fav_24)
            }
            updateFavs()
        }
        //Reference to the PoI sub-section of the db
        val poiRef: DatabaseReference = dbRef.child("POIs")
        //Runs at start and whenever database info changes
        poiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    //Wipe previous data
                    fullPoiList.clear()
                    for (poiSnapshot in dataSnapshot.children) {
                        //Lat and lng are not stored as a LatLng in the Firebase db
                        val lat = poiSnapshot.child("location/latitude/").value.toString().toDouble()
                        val lng = poiSnapshot.child("location/longitude/").value.toString().toDouble()
                        //Create a new PoIModel using db info
                        val poi = PoI(poiSnapshot.key!!, poiSnapshot.child("name").value as String, LatLng(lat, lng),poiSnapshot.child("description").value as String, false)
                        //Add poi to poi list
                        fullPoiList.add(poi)
                    }
                    updateFavs()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })

        rvAdapter = LocationAdapter(currentList())

    }

    //Returns either the full list, or the filtered fav list
    private fun currentList(): MutableList<PoI> {
        return if (favToggle) {
            favPoiList
        } else {
            fullPoiList
        }
    }

    fun updateFavs() {
        favPoiList.clear()
        firebaseDb = FirebaseDatabase.getInstance("https://map-login-57509-default-rtdb.europe-west1.firebasedatabase.app/")
        dbRef = firebaseDb.reference
        //Reference to the PoI sub-section of the db
        var favRef: DatabaseReference = dbRef.child("favourites/${auth.currentUser!!.uid}")

        //One time db listener
        favRef.get().addOnSuccessListener { it ->
            if (it != null) {
                for (favSnapshot in it.children) {
                    //Gets fav poi from the db, searches for it in list of pois
                    val poi: PoI? = fullPoiList.find { it.uuid == favSnapshot.key }
                    //Sets the fav param to true in the poi list
                    poi!!.fav = true
                    favPoiList.add(poi)
                    //Initialises adapter with new list
                    rvAdapter = LocationAdapter(currentList())
                    //Sends initialised adapter to recycler view
                    rvLocations.adapter = rvAdapter

                }
            }
        }.addOnFailureListener{
            Toast.makeText(this@ListActivity, "No favourites found", Toast.LENGTH_LONG).show()
        }
    }

    //Adds on click functionality to nav drawer items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)) {
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

                tempPoiList.clear()

                //Ignore case for searching
                val searchText = newText!!.lowercase(Locale.getDefault())
                if (searchText.isNotEmpty()) {
                    currentList().forEach {
                        //Return items matching entered text
                        if (it.name!!.lowercase(Locale.getDefault()).contains(searchText)) {
                            tempPoiList.add(it)
                        }
                    }
                } else {
                    //If it's empty, return all items
                    tempPoiList.addAll(currentList())
                }
                //Update the Recycler View
                rvAdapter.addList(tempPoiList)

                return false
            }

        })
        return super.onCreateOptionsMenu(menu)
    }
}