package com.example.cameraxtest

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude

data class Favourite(
    val uuid: String = "",
    val name: String = "",
    val location: LatLng = LatLng(51.619174, -3.880502),
    val description: String = "",

){
    @Exclude
    fun toMap(): Map<String, Any?>{
        return mapOf(
            "uuid" to uuid,
            "name" to name,
            "location" to location,
            "description" to description
        )
    }
}