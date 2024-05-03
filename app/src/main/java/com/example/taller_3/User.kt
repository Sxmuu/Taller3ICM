package com.example.taller_3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class User(
    var uid:String,
    var name:String,
    var lastName:String,
    var email: String,
    var password: String,
    var displayName: String,
    var contactImageUrl: String?,
    var identificationNumber: String,
    var latitude: Double,
    var longitude: Double,
    var disponible: Boolean
) : Parcelable {
    constructor() : this("","","", "", "", "", "", "", 0.0, 0.0, true)
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "password" to password,
            "displayName" to displayName,
            "contactImageUrl" to contactImageUrl,
            "identificationNumber" to identificationNumber,
            "latitude" to latitude,
            "longitude" to longitude,
            "disponible" to disponible
        )
    }
}