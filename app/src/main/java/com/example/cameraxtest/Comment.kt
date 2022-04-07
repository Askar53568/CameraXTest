package com.example.cameraxtest

import com.google.firebase.database.DatabaseReference
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

@IgnoreExtraProperties
data class Comment(
    val userId: String = "",
    val content: String = ""

){
    @Exclude
    fun toMap(): Map<String, Any?>{
        return mapOf(
            "userId" to userId,
            "content" to content
        )
    }
}

