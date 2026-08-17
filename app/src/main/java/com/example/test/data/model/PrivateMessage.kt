package com.example.test.data.model

import com.google.firebase.Timestamp

data class PrivateMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val createdAt: Timestamp? = null,
    val edited: Boolean = false
)