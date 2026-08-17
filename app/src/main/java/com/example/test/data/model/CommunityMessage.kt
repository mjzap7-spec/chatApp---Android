package com.example.test.data.model

import com.google.firebase.Timestamp

data class CommunityMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "USER",
    val createdAt: Timestamp? = null,

    val edited: Boolean = false,

    val replyToId: String? = null,
    val replyToName: String? = null,
    val replyToText: String? = null,

    val messageType: String = "TEXT",
    val fileName: String? = null,
    val fileUrl: String? = null,
    val fileType: String? = null,
    val fileSize: Long? = null
)