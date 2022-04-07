package com.example.cameraxtest

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView


class CommentAdapter (commentList: List<Comment>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private lateinit var targetUUID: String
    private var commentList = mutableListOf<Comment>() + commentList
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentAdapter.ViewHolder {
        // Creates inflater
        val inflater = LayoutInflater.from(parent.context)
        // Creates view by inflating the item's XML
        val contactView = inflater.inflate(R.layout.item_location, parent, false)
        // Return the ViewHolder containing the new view
        return ViewHolder(contactView)
    }

    // Populates data for each item
    override fun onBindViewHolder(viewHolder: CommentAdapter.ViewHolder, position: Int) {

        // Get the current poi based on position
        val comment: Comment = commentList[position]
        // Set item views based PoI data
        val tvName = viewHolder.nameTextView
        tvName.text = comment.userId
        val tvDesc = viewHolder.descriptionTextView
        tvDesc.text = comment.content

    }

    // Returns the number of items in the list
    override fun getItemCount(): Int {
        return commentList.size
    }



    /**
     * Updates recycler view with a list
     * @param list to send to recycler view
     */
    @SuppressLint("NotifyDataSetChanged")
    fun addList(list: List<Comment>) {
        commentList = list
        notifyDataSetChanged()
    }
}