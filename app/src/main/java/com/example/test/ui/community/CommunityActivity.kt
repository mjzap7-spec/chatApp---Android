package com.example.test.ui.community

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.ui.friend.FriendAdapter
import com.example.test.ui.chat.PrivateChatActivity
import com.example.test.R
import com.example.test.data.model.CommunityMessage
import com.example.test.data.model.Friend
import com.example.test.databinding.ActivityCommunityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import es.dmoral.toasty.Toasty

class CommunityActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityCommunityBinding

    private lateinit var adapter:
            CommunityAdapter

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private var messagesListener:
            ListenerRegistration? = null

    private var currentName = ""
    private var currentRole = "USER"

    private var replyingTo:
            CommunityMessage? = null

    private lateinit var storage: FirebaseStorage

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { fileUri: Uri? ->

            if (fileUri != null) {
                uploadChatFile(fileUri)
            }
        }

    private lateinit var friendAdapter: FriendAdapter
    private var friendsListener: ListenerRegistration? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityCommunityBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        binding.normalToolbar.visibility =
            View.VISIBLE

        binding.selectionToolbar.visibility =
            View.GONE

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.mainChatContent
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                systemBars.bottom
            )

            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        setupFriendList()
        listenForFriends()
        setupRecyclerView()

        if (auth.currentUser == null) {
            finish()
            return
        }

        setupButtons()
        loadCurrentUserProfile()
    }

    private fun setupRecyclerView() {
        val currentUid = auth.currentUser?.uid.orEmpty()
        adapter = CommunityAdapter(
            currentUserId = currentUid,
            onAvatarClick = { message ->
                showProfileDialog(message)
            },
            onSelectionChanged = { selectedMessages ->
                updateSelectionToolbar(selectedMessages)
            }
        )

        val layoutManager =
            LinearLayoutManager(this).apply {
                stackFromEnd = true
                reverseLayout = false
            }

        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = false

        binding.communityRecyclerView.layoutManager = layoutManager

        binding.communityRecyclerView.adapter = adapter

        binding.communityRecyclerView.itemAnimator = null
    }

    private fun setupFriendList() {
        friendAdapter =
            FriendAdapter { friend ->

                binding.communityDrawer.closeDrawer(
                    GravityCompat.END
                )

                val intent = Intent(
                    this,
                    PrivateChatActivity::class.java
                )

                intent.putExtra(
                    "OTHER_USER_ID",
                    friend.uid
                )

                intent.putExtra(
                    "OTHER_USER_NAME",
                    friend.name
                )

                intent.putExtra(
                    "OTHER_USER_EMAIL",
                    friend.email
                )

                startActivity(intent)
            }

        binding.friendRecyclerView.layoutManager =
            LinearLayoutManager(this)

        binding.friendRecyclerView.adapter =
            friendAdapter
    }

    private fun listenForFriends() {
        val currentUid = auth.currentUser?.uid ?: return
        friendsListener?.remove()
        friendsListener =
            firestore.collection("friends")
                .document(currentUid)
                .collection("userFriends")
                .orderBy("addedAt")
                .addSnapshotListener(
                    this
                ) { snapshot, exception ->

                    if (exception != null) {
                        showError(
                            exception.localizedMessage
                                ?: "Could not load friends"
                        )
                        return@addSnapshotListener
                    }

                    val friends =
                        snapshot
                            ?.documents
                            ?.mapNotNull { document ->

                                document.toObject(
                                    Friend::class.java
                                )?.copy(
                                    uid = document.id
                                )
                            }
                            .orEmpty()

                    friendAdapter.updateFriends(
                        friends
                    )
                }
    }

    private fun uploadChatFile(fileUri: Uri) {
        val firebaseUser =
            auth.currentUser ?: return

        val fileName =
            getFileName(fileUri)

        val mimeType =
            contentResolver.getType(fileUri)
                ?: "application/octet-stream"

        val fileSize = getFileSize(fileUri)

        val storagePath = "communityFiles/${firebaseUser.uid}/${System.currentTimeMillis()}_$fileName"

        val fileReference = storage.reference.child(storagePath)

        binding.btnAttach.isEnabled = false
        binding.btnSend.isEnabled = false

        fileReference
            .putFile(fileUri)
            .continueWithTask { uploadTask ->
                if (!uploadTask.isSuccessful) {
                    throw uploadTask.exception
                        ?: Exception("Upload failed")
                }
                fileReference.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                saveFileMessage(
                    fileName = fileName,
                    fileUrl = downloadUri.toString(),
                    mimeType = mimeType,
                    fileSize = fileSize
                )
                binding.btnAttach.isEnabled = true
                binding.btnSend.isEnabled = true
            }
            .addOnFailureListener { exception ->
                binding.btnAttach.isEnabled = true
                binding.btnSend.isEnabled = true
                showError(
                    exception.localizedMessage
                        ?: "Could not upload file"
                )
            }
    }

    private fun getFileName(uri: Uri): String {
        var result = "file_${System.currentTimeMillis()}"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            val nameIndex =
                cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

            if (
                nameIndex >= 0 &&
                cursor.moveToFirst()
            ) {
                result = cursor.getString(nameIndex)
            }
        }
        return result
    }

    private fun getFileSize(uri: Uri): Long {
        var result = 0L

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->

            val sizeIndex =
                cursor.getColumnIndex(
                    OpenableColumns.SIZE
                )

            if (
                sizeIndex >= 0 &&
                cursor.moveToFirst() &&
                !cursor.isNull(sizeIndex)
            ) {
                result = cursor.getLong(sizeIndex)
            }
        }

        return result
    }

    private fun saveFileMessage(
        fileName: String,
        fileUrl: String,
        mimeType: String,
        fileSize: Long
    ) {
        val firebaseUser = auth.currentUser ?: return

        val messageType =
            when {
                mimeType.startsWith("image/") -> "IMAGE"
                mimeType.startsWith("video/") -> "VIDEO"
                else -> "FILE"
            }

        val message =
            hashMapOf(
                "text" to "",
                "senderId" to firebaseUser.uid,
                "senderName" to currentName,
                "senderRole" to currentRole,
                "createdAt" to
                        FieldValue.serverTimestamp(),

                "messageType" to messageType,
                "fileName" to fileName,
                "fileUrl" to fileUrl,
                "fileType" to mimeType,
                "fileSize" to fileSize
            )

        firestore.collection("communityMessages")
            .add(message)
            .addOnFailureListener { exception ->
                showError(
                    exception.localizedMessage
                        ?: "Could not send file"
                )
            }
    }

    private fun showProfileDialog(
        message: CommunityMessage
    ) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(
            R.layout.dialog_chat_profile,
            null
        )
        val avatar =
            view.findViewById<TextView>(
                R.id.profileAvatar
            )
        val name =
            view.findViewById<TextView>(
                R.id.profileName
            )
        val email =
            view.findViewById<TextView>(
                R.id.profileEmail
            )
        val role =
            view.findViewById<TextView>(
                R.id.profileRole
            )
        val closeButton =
            view.findViewById<Button>(
                R.id.btnCloseProfile
            )
        avatar.text =
            message.senderName
                .firstOrNull()
                ?.uppercase()
                ?: "?"
        name.text =
            message.senderName.ifBlank {
                "Unknown user"
            }
        role.text =
            message.senderRole.uppercase()

        email.text = "Loading..."
        firestore.collection("users")
            .document(message.senderId)
            .get()
            .addOnSuccessListener { document ->
                email.text =
                    document.getString("email")
                        ?: "No email"
            }
            .addOnFailureListener {
                email.text = "No email"
            }
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(
            Color.TRANSPARENT.toDrawable()
        )
        dialog.show()
        dialog.window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(
                (
                        resources.displayMetrics.widthPixels * 0.85).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun updateSelectionToolbar(
        selectedMessages:
        List<CommunityMessage>
    ) {
        val selectionMode = selectedMessages.isNotEmpty()
        binding.normalToolbar.visibility =
            if (selectionMode) { View.GONE
            } else { View.VISIBLE }
        binding.selectionToolbar.visibility =
            if (selectionMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.txtSelectedCount.text = "${selectedMessages.size} selected"

        val allOwnedByCurrentUser =
            selectedMessages.all { message ->
                message.senderId == auth.currentUser?.uid
            }

        binding.btnDeleteSelected.visibility =
            if (
                selectionMode &&
                allOwnedByCurrentUser
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

//    private fun showMessageOptions(
//        message: CommunityMessage,
//        anchor: View
//    ) {
//        val popupMenu =
//            PopupMenu(this, anchor)
//
//        popupMenu.menu.add("Reply")
//        popupMenu.menu.add("Copy")
//
//        val isMyMessage =
//            message.senderId ==
//                    auth.currentUser?.uid
//
//        if (isMyMessage) {
//            popupMenu.menu.add("Edit")
//            popupMenu.menu.add("Delete")
//        }
//
//        popupMenu.setOnMenuItemClickListener { item ->
//
//            when (item.title.toString()) {
//                "Reply" -> {
//                    startReply(message)
//                }
//
//                "Copy" -> {
//                    copyMessage(message.text)
//                }
//
//                "Edit" -> {
//                    showEditMessageDialog(message)
//                }
//
//                "Delete" -> {
//                    confirmDeleteMessage(message)
//                }
//            }
//
//            true
//        }
//
//        popupMenu.show()
//    }

    private fun copyMessage(text: String) {
        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "Community message",
                text
            )

        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            this,
            "Message copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEditMessageDialog(
        message: CommunityMessage
    ) {
        val input = EditText(this)

        input.setText(message.text)
        input.setSelection(input.text.length)

        AlertDialog.Builder(this)
            .setTitle("Edit message")
            .setView(input)
            .setPositiveButton("Save") {
                    _,
                    _ ->

                val updatedText =
                    input.text
                        .toString()
                        .trim()

                if (updatedText.isNotEmpty()) {
                    updateMessage(
                        message.id,
                        updatedText
                    )
                }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun updateMessage(
        messageId: String,
        updatedText: String
    ) {
        firestore.collection("communityMessages")
            .document(messageId)
            .update(
                mapOf(
                    "text" to updatedText,
                    "edited" to true
                )
            )
    }

    private fun confirmDeleteMessage(
        message: CommunityMessage
    ) {
        AlertDialog.Builder(this)
            .setTitle("Delete message")
            .setMessage(
                "Do you want to delete this message?"
            )
            .setPositiveButton("Delete") {
                    _,
                    _ ->

                deleteMessage(message.id)
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun deleteMessage(
        messageId: String
    ) {
        firestore.collection("communityMessages")
            .document(messageId)
            .delete()
            .addOnFailureListener {
                showError("Could not delete message")
            }
    }

    private fun startReply(
        message: CommunityMessage
    ) {
        replyingTo = message

        binding.editMessage.hint =
            "Replying to ${message.senderName}"

        binding.editMessage.requestFocus()
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            when {
                binding.communityDrawer.isDrawerOpen(
                    GravityCompat.END
                ) -> {
                    binding.communityDrawer.closeDrawer(
                        GravityCompat.END
                    )
                }

                adapter.isSelectionMode() -> {
                    adapter.clearSelection()
                }

                else -> {
                    finish()
                }
            }
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnCloseSelection.setOnClickListener {
            adapter.clearSelection()
        }

        binding.btnCopySelected.setOnClickListener {
            copySelectedMessages()
        }

        binding.btnDeleteSelected.setOnClickListener {
            confirmDeleteSelectedMessages()
        }

        binding.btnAddFriend.setOnClickListener {
            showAddFriendDialog()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object :
                OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    when {
                        binding.communityDrawer.isDrawerOpen(
                            GravityCompat.END
                        ) -> {
                            binding.communityDrawer.closeDrawer(
                                GravityCompat.END
                            )
                        }

                        adapter.isSelectionMode() -> {
                            adapter.clearSelection()
                        }

                        else -> {
                            finish()
                        }
                    }
                }
            }
        )

        binding.btnFriends.setOnClickListener {
            binding.communityDrawer.openDrawer(
                GravityCompat.END
            )
        }
    }

    private fun showFriendsDialog() {
        val dialog = Dialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_friends,
            null
        )

        val recyclerView =
            view.findViewById<RecyclerView>(
                R.id.friendRecyclerView
            )

        val addFriendButton =
            view.findViewById<Button>(
                R.id.btnAddFriend
            )

        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = friendAdapter

        addFriendButton.setOnClickListener {
            dialog.dismiss()
            showAddFriendDialog()
        }

        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(
            Color.TRANSPARENT.toDrawable()
        )

        dialog.show()

        dialog.window?.setLayout(
            (
                    resources.displayMetrics
                        .widthPixels * 0.9
                    ).toInt(),
            WindowManager.LayoutParams
                .WRAP_CONTENT
        )
    }

    private fun showAddFriendDialog() {
        val input = EditText(this)

        input.hint = "Friend email"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(this)
            .setTitle("Add friend")
            .setMessage(
                "Enter the email of the user you want to add."
            )
            .setView(input)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val email =
                            input.text
                                .toString()
                                .trim()
                                .lowercase()

                        if (email.isEmpty()) {
                            input.error = "Please enter an email"
                            return@setOnClickListener
                        }

                        addFriendByEmail(
                            email = email,
                            dialog = this
                        )
                    }
                }
                show()
            }
    }

    private fun addFriendByEmail(
        email: String,
        dialog: AlertDialog
    ) {
        val currentUid = auth.currentUser?.uid ?: return

        val searchEmail = email.trim().lowercase()

        firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val document =
                    snapshot.documents.firstOrNull {
                        val savedEmail =
                            it.getString("email")
                                ?.trim()
                                ?.lowercase()

                        savedEmail == searchEmail
                    }

                if (document == null) {
                    showError(
                        "No user found with this email"
                    )
                    return@addOnSuccessListener
                }

                val friendUid = document.id

                if (friendUid == currentUid) {
                    showError(
                        "You cannot add yourself"
                    )
                    return@addOnSuccessListener
                }

                saveFriend(
                    friendUid = friendUid,
                    friendName =
                        document.getString("name")
                            ?: "Unknown user",
                    friendEmail =
                        document.getString("email")
                            ?: searchEmail,
                    dialog = dialog
                )
            }
            .addOnFailureListener {
                showError(
                    it.localizedMessage
                        ?: "Search failed"
                )
            }
    }


    private fun saveFriend(
        friendUid: String,
        friendName: String,
        friendEmail: String,
        dialog: AlertDialog
    ) {
        val currentUid =
            auth.currentUser?.uid

        if (currentUid.isNullOrBlank()) {
            showError("Please sign in again")
            return
        }

        val friendData =
            hashMapOf(
                "uid" to friendUid,
                "name" to friendName,
                "email" to friendEmail,
                "addedAt" to
                        FieldValue
                            .serverTimestamp()
            )

        firestore.collection("friends")
            .document(currentUid)
            .collection("userFriends")
            .document(friendUid)
            .set(friendData)
            .addOnSuccessListener {
                dialog.dismiss()

                showSuccess(
                    "$friendName added"
                )
            }
            .addOnFailureListener { exception ->

                Log.e(
                    "CommunityActivity",
                    "Could not save friend",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not add friend"
                )
            }
    }

    private fun copySelectedMessages() {
        val selected =
            adapter.getSelectedMessages()

        if (selected.isEmpty()) {
            return
        }

        val combinedText =
            selected.joinToString(
                separator = "\n"
            ) { message ->
                "${message.senderName}: ${message.text}"
            }

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData
                .newPlainText(
                    "Community messages",
                    combinedText
                )
        )

        adapter.clearSelection()

        Toast.makeText(
            this,
            "Messages copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun confirmDeleteSelectedMessages() {
        val selected =
            adapter.getSelectedMessages()

        if (selected.isEmpty()) {
            return
        }

        val currentUid =
            auth.currentUser?.uid

        val deletableMessages =
            selected.filter {
                it.senderId == currentUid
            }

        if (
            deletableMessages.size !=
            selected.size
        ) {
            showError(
                "You can delete only your own messages"
            )
            return
        }

        AlertDialog
            .Builder(this)
            .setTitle("Delete messages")
            .setMessage(
                "Delete ${selected.size} selected messages?"
            )
            .setPositiveButton("Delete") {
                    _,
                    _ ->

                deleteSelectedMessages(
                    deletableMessages
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun deleteSelectedMessages(
        messages: List<CommunityMessage>
    ) {
        val batch = firestore.batch()

        messages.forEach { message ->
            val reference =
                firestore.collection(
                    "communityMessages"
                )
                    .document(message.id)

            batch.delete(reference)
        }

        batch.commit()
            .addOnSuccessListener {
                adapter.clearSelection()

                Toast.makeText(
                    this,
                    "Messages deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->
                showError(
                    exception.localizedMessage
                        ?: "Could not delete messages"
                )
            }
    }

    private fun loadCurrentUserProfile() {
        val uid =
            auth.currentUser?.uid
                ?: return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                currentName =
                    document.getString("name")
                        ?: "Unknown user"

                currentRole =
                    document.getString("role")
                        ?: "USER"

                listenForMessages()
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "CommunityActivity",
                    "Could not load user profile",
                    exception
                )

                showError(
                    "Could not load your profile"
                )
            }
    }

    private fun sendMessage() {

        val replyMessage = replyingTo
        val firebaseUser =
            auth.currentUser

        if (firebaseUser == null) {
            showError("Please sign in again")
            return
        }

        val messageText =
            binding.editMessage.text
                .toString()
                .trim()

        if (messageText.isEmpty()) {
            return
        }

        binding.btnSend.isEnabled = false

        val message = hashMapOf(
            "text" to messageText,
            "senderId" to firebaseUser.uid,
            "senderName" to currentName,
            "senderRole" to currentRole,
            "createdAt" to FieldValue.serverTimestamp(),
            "edited" to false
        )

        firestore.collection(
            "communityMessages"
        )
            .add(message)
            .addOnSuccessListener {
                binding.editMessage.text.clear()
                binding.btnSend.isEnabled = true
                replyingTo = null
                binding.editMessage.hint = "Message"
            }
            .addOnFailureListener { exception ->
                binding.btnSend.isEnabled = true

                Log.e(
                    "CommunityActivity",
                    "Could not send message",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not send message"
                )
            }
    }

    private fun listenForMessages() {
        messagesListener?.remove()

        messagesListener =
            firestore.collection(
                "communityMessages"
            )
                .orderBy(
                    "createdAt",
                    Query.Direction.ASCENDING
                )
                .addSnapshotListener(
                    this
                ) { snapshot, exception ->

                    if (exception != null) {
                        Log.e(
                            "CommunityActivity",
                            "Could not load messages",
                            exception
                        )

                        showError(
                            exception.localizedMessage
                                ?: "Could not load messages"
                        )

                        return@addSnapshotListener
                    }

                    val messages =
                        snapshot
                            ?.documents
                            ?.mapNotNull { document ->
                                document
                                    .toObject(
                                        CommunityMessage::class.java
                                    )
                                    ?.copy(
                                        id = document.id
                                    )
                            }
                            .orEmpty()

                    adapter.updateMessages(messages)

                    if (messages.isNotEmpty()) {
                        binding.communityRecyclerView
                            .scrollToPosition(
                                messages.lastIndex
                            )
                    }
                }
    }


    private fun showSuccess(message: String) {
        Toasty.success(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun showError(message: String) {
        Toasty.error(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        messagesListener?.remove()
        super.onDestroy()
    }
}