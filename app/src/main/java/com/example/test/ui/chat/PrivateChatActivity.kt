package com.example.test.ui.chat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.databinding.ActivityPrivateChatBinding
import com.example.test.data.model.PrivateMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import es.dmoral.toasty.Toasty

class PrivateChatActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityPrivateChatBinding

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private lateinit var adapter:
            PrivateMessageAdapter

    private var messageListener:
            ListenerRegistration? = null

    private var currentUid = ""
    private var otherUserId = ""
    private var otherUserName = ""
    private var otherUserEmail = ""
    private var chatId = ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        binding =
            ActivityPrivateChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        currentUid = auth.currentUser?.uid.orEmpty()

        otherUserId = intent.getStringExtra("OTHER_USER_ID").orEmpty()

        otherUserName =
            intent.getStringExtra("OTHER_USER_NAME").orEmpty()

        otherUserEmail = intent.getStringExtra("OTHER_USER_EMAIL").orEmpty()

        if (
            currentUid.isBlank() ||
            otherUserId.isBlank()
        ) {
            finish()
            return
        }

        chatId =
            createChatId(currentUid, otherUserId)
        setupHeader()
        setupRecyclerView()
        setupButtons()
        listenForMessages()
    }

    private fun setupHeader() {
        binding.txtOtherName.text =
            otherUserName.ifBlank {
                "Private chat"
            }

        binding.txtOtherAvatar.text =
            otherUserName
                .firstOrNull()
                ?.uppercase()
                ?: "?"
    }

    private fun setupRecyclerView() {
        adapter =
            PrivateMessageAdapter(
                currentUid = currentUid,
                otherUserName =
                    otherUserName
            )

        binding.privateMessageRecyclerView
            .layoutManager =
            LinearLayoutManager(this).apply {
                stackFromEnd = true
            }

        binding.privateMessageRecyclerView
            .adapter = adapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val text =
            binding.editMessage.text
                .toString()
                .trim()

        if (text.isEmpty()) {
            return
        }

        binding.btnSend.isEnabled = false

        val chatReference =
            firestore.collection(
                "privateChats"
            )
                .document(chatId)

        val messageReference =
            chatReference
                .collection("messages")
                .document()

        val batch = firestore.batch()

        val chatData =
            hashMapOf<String, Any>(
                "participantIds" to
                        listOf(
                            currentUid,
                            otherUserId
                        ),
                "lastMessage" to text,
                "lastMessageSenderId" to
                        currentUid,
                "lastMessageAt" to
                        FieldValue.serverTimestamp()
            )

        val messageData =
            hashMapOf<String, Any>(
                "text" to text,
                "senderId" to currentUid,
                "receiverId" to otherUserId,
                "createdAt" to
                        FieldValue.serverTimestamp(),
                "edited" to false
            )

        batch.set(
            chatReference,
            chatData
        )

        batch.set(
            messageReference,
            messageData
        )

        batch.commit()
            .addOnSuccessListener {
                binding.editMessage
                    .text
                    .clear()

                binding.btnSend.isEnabled =
                    true
            }
            .addOnFailureListener {
                    exception ->

                binding.btnSend.isEnabled =
                    true

                showError(
                    exception.localizedMessage
                        ?: "Could not send message"
                )
            }
    }

    private fun listenForMessages() {
        messageListener?.remove()

        messageListener =
            firestore.collection(
                "privateChats"
            )
                .document(chatId)
                .collection("messages")
                .orderBy(
                    "createdAt",
                    Query.Direction.ASCENDING
                )
                .addSnapshotListener(
                    this
                ) {
                        snapshot,
                        exception ->

                    if (exception != null) {
                        Log.e(
                            "PrivateChat",
                            "Listener failed",
                            exception
                        )

                        showError(
                            "Could not load messages"
                        )

                        return@addSnapshotListener
                    }

                    val messages =
                        snapshot
                            ?.documents
                            ?.mapNotNull {
                                    document ->

                                document.toObject(
                                    PrivateMessage::class.java
                                )?.copy(
                                    id = document.id
                                )
                            }
                            .orEmpty()

                    adapter.updateMessages(
                        messages
                    )

                    if (messages.isNotEmpty()) {
                        binding
                            .privateMessageRecyclerView
                            .scrollToPosition( messages.lastIndex )
                    }
                }
    }

    private fun createChatId(
        firstUid: String,
        secondUid: String
    ): String {
        return if (firstUid < secondUid) {
            "${firstUid}_${secondUid}"
        } else {
            "${secondUid}_${firstUid}"
        }
    }

    private fun showError(
        message: String
    ) {
        Toasty.error(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        messageListener?.remove()
        super.onDestroy()
    }
}