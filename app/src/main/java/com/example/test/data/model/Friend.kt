package com.example.test.data.model

import com.google.firebase.Timestamp

data class Friend(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val addedAt: Timestamp? = null
)