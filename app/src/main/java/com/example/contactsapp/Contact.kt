package com.example.contactsapp

data class Contact(
    val id: Int,
    var nom: String,
    var prenom: String,
    var telephone: String,
    var email: String,
    var photoUri: String? = null
)