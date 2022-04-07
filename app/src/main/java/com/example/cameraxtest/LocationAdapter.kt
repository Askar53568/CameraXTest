package com.example.cameraxtest

import android.annotation.SuppressLint
import com.example.cameraxtest.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView


class LocationAdapter (poiList: List<PoI>) : RecyclerView.Adapter<LocationAdapter.ViewHolder>() {

    private lateinit var targetUUID: String
    private var poiList = mutableListOf<PoI>() + poiList
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference



    //Provides access for the views in each list object
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Initializing variables in each view
        val nameTextView: TextView = itemView.findViewById<TextView>(R.id.tvName)
        val descriptionTextView: TextView = itemView.findViewById<TextView>(R.id.tvDescription)
        val image: CircleImageView = itemView.findViewById(R.id.iv_location_image)
    }

    // Constructor inflates the layout from XML, returns the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationAdapter.ViewHolder {
        // Creates inflater
        val inflater = LayoutInflater.from(parent.context)
        // Creates view by inflating the item's XML
        val contactView = inflater.inflate(R.layout.item_location, parent, false)
        // Return the ViewHolder containing the new view
        return ViewHolder(contactView)
    }

    // Populates data for each item
    override fun onBindViewHolder(viewHolder: LocationAdapter.ViewHolder, position: Int) {

        // Get the current poi based on position
        val poi: PoI = poiList[position]
        // Set item views based PoI data
        val tvName = viewHolder.nameTextView
        tvName.text = poi.name
        val tvDesc = viewHolder.descriptionTextView
        tvDesc.text = poi.description
        //uuid
        targetUUID = poi.uuid
        //Item image
        storage = FirebaseStorage.getInstance("gs://map-login-57509.appspot.com")
        storageReference = storage.reference
        val image = viewHolder.image
        displayImage(image)

    }

    // Returns the number of items in the list
    override fun getItemCount(): Int {
        return poiList.size
    }

    //Download image from the storage and display it in the image view
    protected fun displayImage(imageView: ImageView) {
        //Get the reference to the image
        val imagePOIref: StorageReference = storageReference.child("images/" + this.targetUUID)
        imagePOIref.downloadUrl.addOnSuccessListener { imageView.load(it) }
    }



    /**
     * Updates recycler view with a list
     * @param list to send to recycler view
     */
    @SuppressLint("NotifyDataSetChanged")
    fun addList(list: List<PoI>) {
        poiList = list
        notifyDataSetChanged()
    }
}