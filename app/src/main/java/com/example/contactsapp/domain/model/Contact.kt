package com.example.contactsapp.domain.model

import android.net.Uri

data class Contact (
    val name: String,
    val phoneNumber: String,
    val phoneType: String, // mobile - home - work
    val photoUri: Uri?
)