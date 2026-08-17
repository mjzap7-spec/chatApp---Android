package com.example.test.ui.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.R
import com.example.test.databinding.ActivityUserListBinding
import com.example.test.data.model.User
import com.example.test.ui.auth.SignInActivity
import com.example.test.ui.chat.PrivateChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import es.dmoral.toasty.Toasty

class UserListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserListBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private var usersListener: ListenerRegistration? = null
    private val allUsers = mutableListOf<User>()
    private var currentUid = ""
    private var currentName = ""
    private var currentEmail = ""
    private var currentRole = "USER"

    private var currentSearch = ""
    private var selectedRoleFilter = "ALL"

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        binding =
            ActivityUserListBinding.inflate(
                layoutInflater
            )
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            openSignInScreen()
            return
        }
        currentUid = firebaseUser.uid
        currentEmail = firebaseUser.email.orEmpty()
        setupSearch()
        setupCardFilters()
        setupDeleteSelectedButton()
        setupBackNavigation()
        loadCurrentUserProfile()
    }
    private fun loadCurrentUserProfile() {
        firestore.collection("users")
            .document(currentUid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    showError("Your profile was not found")
                    signOut()
                    return@addOnSuccessListener
                }
                currentName = document.getString("name").orEmpty()
                currentEmail = document.getString("email") ?: currentEmail
                currentRole = document.getString("role") ?: "USER"
                setupAccountButton()
                setupRecyclerView()
                listenForUsers()
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Could not load current profile",
                    exception
                )
                showError(
                    exception.localizedMessage
                        ?: "Could not load your profile"
                )
            }
    }

    private fun setupRecyclerView() {
        binding.userRecyclerView.layoutManager =
            LinearLayoutManager(this)

        userAdapter = UserAdapter(
            currentUid = currentUid,
            currentRole = currentRole,

            onEditUserClick = {
                    selectedUser: User ->

                showUserEditDialog(
                    selectedUser
                )
            },

            onSaveName = {
                    user: User,
                    newName: String ->

                updateUserName(
                    user = user,
                    newName = newName
                )
            },

            onDeleteUser = {
                    user: User ->

                confirmDeleteUserFromRow(
                    user
                )
            },

            onSelectionChanged = {
                    selectedCount: Int,
                    selectionMode: Boolean ->

                updateSelectionHeader(
                    selectedCount =
                        selectedCount,
                    selectionMode =
                        selectionMode
                )
            }
        )

        binding.userRecyclerView.adapter =
            userAdapter
    }

    private fun confirmDeleteUserFromRow(
        user: User
    ) {
        if (
            !currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {
            showError(
                "Only managers can delete profiles"
            )
            return
        }

        if (user.uid == currentUid) {
            showError(
                "You cannot delete your own profile"
            )
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete user")
            .setMessage(
                "Delete ${user.name}'s profile?"
            )
            .setPositiveButton("Delete") {
                    _,
                    _ ->

                deleteUserProfileFromRow(user)
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun deleteUserProfileFromRow(
        user: User
    ) {
        firestore.collection("users")
            .document(user.uid)
            .delete()
            .addOnSuccessListener {
                showSuccess(
                    "User profile deleted"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Could not delete profile",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not delete profile"
                )
            }
    }

    private fun openPrivateChat(user: User) {
        if (user.uid == currentUid) {
            return
        }

        val intent = Intent(
            this,
            PrivateChatActivity::class.java
        )

        intent.putExtra(
            "OTHER_USER_ID",
            user.uid
        )

        intent.putExtra(
            "OTHER_USER_NAME",
            user.name
        )

        intent.putExtra(
            "OTHER_USER_EMAIL",
            user.email
        )

        startActivity(intent)
    }

    private fun updateUserName(
        user: User,
        newName: String
    ) {
        val cleanName = newName.trim()

        if (cleanName.isEmpty()) {
            showError("Please enter a name")
            return
        }

        val isManager =
            currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )

        val isCurrentUser =
            user.uid == currentUid

        if (!isManager && !isCurrentUser) {
            showError("You cannot edit this user")
            return
        }

        firestore.collection("users")
            .document(user.uid)
            .update("name", cleanName)
            .addOnSuccessListener {
                if (isCurrentUser) {
                    currentName = cleanName
                    setupAccountButton()
                }

                showSuccess("User updated")
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Could not update user name",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not update user"
                )
            }
    }
    private fun listenForUsers() {
        usersListener?.remove()
        if (
            currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {
            listenForAllUsers()
        } else {
            listenForCurrentUser()
        }
    }

    private fun listenForAllUsers() {
        usersListener =
            firestore.collection("users")
                .addSnapshotListener {
                        snapshot,
                        exception ->

                    if (exception != null) {
                        Log.e(
                            "UserListActivity",
                            "Could not load users",
                            exception
                        )

                        showError(
                            exception.localizedMessage
                                ?: "Could not load users"
                        )

                        return@addSnapshotListener
                    }

                    val loadedUsers =
                        snapshot
                            ?.documents
                            ?.mapNotNull { document ->

                                document
                                    .toObject(
                                        User::class.java
                                    )
                                    ?.copy(
                                        uid = document.id
                                    )
                            }
                            .orEmpty()
                    allUsers.clear()
                    allUsers.addAll(loadedUsers)
                    applyFilters()
                    updateStatistics(allUsers)
                }
    }

    private fun listenForCurrentUser() {
        usersListener =
            firestore.collection("users")
                .document(currentUid)
                .addSnapshotListener {
                        document,
                        exception ->

                    if (exception != null) {
                        Log.e(
                            "UserListActivity",
                            "Could not load current user",
                            exception
                        )
                        showError(
                            exception.localizedMessage
                                ?: "Could not load your profile"
                        )

                        return@addSnapshotListener
                    }
                    val user =
                        document
                            ?.takeIf {
                                it.exists()
                            }
                            ?.toObject(
                                User::class.java
                            )
                            ?.copy(
                                uid = document.id
                            )

                    allUsers.clear()

                    if (user != null) {
                        allUsers.add(user)

                        currentName = user.name
                        currentEmail = user.email

                        setupAccountButton()
                    }

                    applyFilters()
                    updateStatistics(allUsers)
                }
    }

    private fun setupSearch() {
        binding.searchUser
            .addTextChangedListener { editable ->

                currentSearch =
                    editable
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                if (::userAdapter.isInitialized) {
                    applyFilters()
                }
            }
    }

    private fun setupCardFilters() {
        binding.cardTotalUsers
            .setOnClickListener {

                selectedRoleFilter = "ALL"
                applyFilters()
            }

        binding.cardManagers
            .setOnClickListener {

                selectedRoleFilter = "MANAGER"
                applyFilters()
            }

        binding.cardUsers
            .setOnClickListener {

                selectedRoleFilter = "USER"
                applyFilters()
            }
    }

    private fun applyFilters() {
        if (!::userAdapter.isInitialized) {
            return
        }

        var filteredUsers =
            allUsers.toList()

        if (selectedRoleFilter != "ALL") {
            filteredUsers =
                filteredUsers.filter { user ->
                    user.role.equals(
                        selectedRoleFilter,
                        ignoreCase = true
                    )
                }
        }
        if (currentSearch.isNotEmpty()) {
            filteredUsers =
                filteredUsers.filter { user ->

                    user.name.contains(
                        currentSearch,
                        ignoreCase = true
                    ) ||
                            user.email.contains(
                                currentSearch,
                                ignoreCase = true
                            ) ||
                            user.role.contains(
                                currentSearch,
                                ignoreCase = true
                            )
                }
        }
        userAdapter.updateList(filteredUsers)
    }
    private fun updateStatistics(
        users: List<User>
    ) {
        binding.txtTotalUsers.text = users.size.toString()

        binding.txtManagers.text =
            users.count { user ->
                user.role.equals(
                    "MANAGER",
                    ignoreCase = true
                )
            }.toString()
        binding.txtUsers.text =
            users.count { user ->
                user.role.equals(
                    "USER",
                    ignoreCase = true
                )
            }.toString()
    }

    private fun updateSelectionHeader(
        selectedCount: Int,
        selectionMode: Boolean
    ) {
        binding.btnDeleteSelected.visibility =
            if (selectionMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.txtAccountAvatar.visibility =
            if (selectionMode) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.txtTitle.text =
            if (selectionMode) {
                "$selectedCount selected"
            } else {
                "User Management"
            }
    }

    private fun showUserEditDialog(user: User) {
        val dialog = Dialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_user_edit,
            null
        )

        val avatar =
            view.findViewById<TextView>(
                R.id.detailAvatar
            )

        val nameInput =
            view.findViewById<EditText>(
                R.id.editDetailName
            )

        val emailInput =
            view.findViewById<EditText>(
                R.id.editDetailEmail
            )

        val resetPasswordButton =
            view.findViewById<Button>(
                R.id.btnResetPassword
            )

        val saveButton =
            view.findViewById<Button>(
                R.id.btnSaveDetail
            )

        val deleteButton =
            view.findViewById<Button>(
                R.id.btnDeleteDetail
            )

        val isManager =
            currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )

        val isCurrentUser =
            user.uid == currentUid

        val canEditName =
            isManager || isCurrentUser

        val canDelete =
            isManager && !isCurrentUser

        avatar.text =
            user.name.firstOrNull()
                ?.uppercase()
                ?: "?"

        nameInput.setText(user.name)
        emailInput.setText(user.email)

        /*
         * Do not update only the Firestore email.
         * Firebase Authentication owns the login email.
         */
        emailInput.isEnabled = false
        nameInput.isEnabled = canEditName

        /*
         * Only a manager can use the administrative
         * password-reset function.
         */
        resetPasswordButton.visibility =
            if (isManager && !isCurrentUser) {
                View.VISIBLE
            } else {
                View.GONE
            }

        deleteButton.visibility =
            if (canDelete) {
                View.VISIBLE
            } else {
                View.GONE
            }

        saveButton.visibility =
            if (canEditName) {
                View.VISIBLE
            } else {
                View.GONE
            }

        saveButton.setOnClickListener {
            val updatedName =
                nameInput.text
                    .toString()
                    .trim()

            if (updatedName.isEmpty()) {
                nameInput.error =
                    "Please enter a name"

                nameInput.requestFocus()
                return@setOnClickListener
            }

            saveUserChanges(
                user = user,
                updatedName = updatedName,
                dialog = dialog
            )
        }

        resetPasswordButton.setOnClickListener {
            confirmPasswordReset(
                user = user,
                dialog = dialog
            )
        }

        deleteButton.setOnClickListener {
            confirmDeleteUser(
                user = user,
                dialog = dialog
            )
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
                        resources.displayMetrics.widthPixels *
                                0.88
                        ).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            attributes =
                attributes.apply {
                    dimAmount = 0.6f
                }

            addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
        }
    }

    private fun saveUserChanges(
        user: User,
        updatedName: String,
        dialog: Dialog
    ) {
        val isManager =
            currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )

        val isCurrentUser =
            user.uid == currentUid

        if (!isManager && !isCurrentUser) {
            showError(
                "You cannot edit this user"
            )
            return
        }

        firestore.collection("users")
            .document(user.uid)
            .update(
                "name",
                updatedName
            )
            .addOnSuccessListener {
                if (isCurrentUser) {
                    currentName = updatedName
                    setupAccountButton()
                }

                dialog.dismiss()
                showSuccess("User updated")
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Could not update profile",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not update profile"
                )
            }
    }

    private fun confirmPasswordReset(
        user: User,
        dialog: Dialog
    ) {
        if (
            !currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {
            showError(
                "Only managers can reset passwords"
            )
            return
        }

        if (user.uid == currentUid) {
            showError(
                "Use your account settings to change your own password"
            )
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Reset password")
            .setMessage(
                "Change ${user.name}'s password to 12345678?"
            )
            .setPositiveButton("Reset") {
                    _,
                    _ ->

                resetUserPassword(
                    user = user,
                    dialog = dialog
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun resetUserPassword(
        user: User,
        dialog: Dialog
    ) {
        if (
            !currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {
            showError(
                "Only managers can reset passwords"
            )
            return
        }

        val data =
            hashMapOf<String, Any>(
                "targetUid" to user.uid
            )

        functions
            .getHttpsCallable(
                "resetUserPassword"
            )
            .call(data)
            .addOnSuccessListener {
                dialog.dismiss()

                showSuccess(
                    "Password changed to 12345678"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Password reset failed",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not reset password"
                )
            }
    }

    private fun confirmDeleteUser(
        user: User,
        dialog: Dialog
    ) {
        if (
            !currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {
            showError(
                "Only managers can delete profiles"
            )
            return
        }

        if (user.uid == currentUid) {
            showError(
                "You cannot delete your own profile here"
            )
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete user")
            .setMessage(
                "Delete ${user.name}'s profile?"
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteUserProfile(
                    user = user,
                    dialog = dialog
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun deleteUserProfile(
        user: User,
        dialog: Dialog
    ) {
        firestore.collection("users")
            .document(user.uid)
            .delete()
            .addOnSuccessListener {
                dialog.dismiss()

                showSuccess(
                    "User profile deleted"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Could not delete profile",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not delete profile"
                )
            }
    }

    private fun setupDeleteSelectedButton() {
        binding.btnDeleteSelected
            .setOnClickListener {

                if (!::userAdapter.isInitialized) {
                    return@setOnClickListener
                }

                val selectedUsers =
                    userAdapter.getSelectedUsers()

                if (selectedUsers.isEmpty()) {
                    showError(
                        "Please select users first"
                    )
                    return@setOnClickListener
                }

                AlertDialog.Builder(this)
                    .setTitle("Delete users")
                    .setMessage(
                        "Delete ${selectedUsers.size} selected profiles?"
                    )
                    .setPositiveButton(
                        "Delete"
                    ) { _, _ ->

                        deleteSelectedProfiles(
                            selectedUsers
                        )
                    }
                    .setNegativeButton(
                        "Cancel",
                        null
                    )
                    .show()
            }
    }

    private fun deleteSelectedProfiles(
        selectedUsers: List<User>
    ) {
        if (
            !currentRole.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {
            showError(
                "Only managers can delete profiles"
            )
            return
        }

        val validUsers =
            selectedUsers.filter { user ->
                user.uid.isNotBlank() &&
                        user.uid != currentUid
            }

        if (validUsers.isEmpty()) {
            showError(
                "No valid users were selected"
            )
            return
        }

        val batch = firestore.batch()

        validUsers.forEach { user ->
            val reference =
                firestore.collection("users")
                    .document(user.uid)

            batch.delete(reference)
        }

        batch.commit()
            .addOnSuccessListener {
                userAdapter.clearSelection()

                showSuccess(
                    "${validUsers.size} profiles deleted"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "UserListActivity",
                    "Bulk deletion failed",
                    exception
                )

                showError(
                    exception.localizedMessage
                        ?: "Could not delete profiles"
                )
            }
    }

    private fun setupAccountButton() {
        binding.txtAccountAvatar.text =
            currentName.firstOrNull()
                ?.uppercase()
                ?: "?"

        binding.txtAccountAvatar
            .setOnClickListener {
                showAccountDialog()
            }
    }

    private fun showAccountDialog() {
        val dialog = Dialog(this)

        val view =
            layoutInflater.inflate(
                R.layout.dialog_account,
                null
            )

        val avatar =
            view.findViewById<TextView>(
                R.id.dialogAvatar
            )

        val name =
            view.findViewById<TextView>(
                R.id.dialogName
            )

        val email =
            view.findViewById<TextView>(
                R.id.dialogEmail
            )

        val role =
            view.findViewById<TextView>(
                R.id.dialogRole
            )

        val logout =
            view.findViewById<Button>(
                R.id.btnLogout
            )

        avatar.text =
            currentName.firstOrNull()
                ?.uppercase()
                ?: "?"

        name.text =
            currentName.ifBlank {
                "Unknown"
            }

        email.text =
            currentEmail.ifBlank {
                "No email"
            }

        role.text =
            currentRole.uppercase()

        logout.setOnClickListener {
            dialog.dismiss()
            signOut()
        }

        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(
            Color.TRANSPARENT.toDrawable()
        )

        dialog.show()

        dialog.window?.apply {
            setLayout(
                320,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            setGravity(
                Gravity.TOP or Gravity.END
            )

            attributes =
                attributes.apply {
                    x = 24
                    y = 80
                }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    if (
                        ::userAdapter.isInitialized &&
                        userAdapter.isSelectionMode()
                    ) {
                        userAdapter.clearSelection()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun signOut() {
        usersListener?.remove()
        auth.signOut()
        openSignInScreen()
    }

    private fun openSignInScreen() {
        val intent =
            Intent(
                this,
                SignInActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
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
        usersListener?.remove()
        super.onDestroy()
    }
}