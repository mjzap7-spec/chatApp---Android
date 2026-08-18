package com.example.test.ui.user

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast

import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.test.R
import com.example.test.data.model.User
import com.example.test.databinding.ActivityUserListBinding
import com.example.test.viewModel.UserViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

import kotlinx.coroutines.launch

class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding

    private val viewModel: UserViewModel by viewModels()

    private lateinit var adapter: UserAdapter

    private lateinit var auth: FirebaseAuth

    // -----------------------------
    // Current user information
    // -----------------------------

    private var currentUid: String = ""

    private var currentRole: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        currentUid =
            auth.currentUser?.uid ?: ""

        if (currentUid.isEmpty()) {
            finish()
            return
        }

        binding =
            ActivityUserListBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel.loadCurrentUser(
            currentUid
        )

        setupRecyclerView()

        observeViewModel()

        viewModel.loadUsers()
    }


    // =========================================================
    // RecyclerView
    // =========================================================

    private fun setupRecyclerView() {

        adapter = UserAdapter(

            currentUid = currentUid,

            currentRole = currentRole,

            onEditUserClick = { user ->

                showEditUserDialog(user)
            },

            onSaveName = { user, newName ->

                viewModel.updateUserName(
                    user,
                    newName
                )
            },

            onDeleteUser = { user ->

                viewModel.deleteUser(user)
            },

            onSelectionChanged = { count, hasSelection ->

                updateSelectionUI(
                    count,
                    hasSelection
                )
            }
        )


        binding.userRecyclerView.layoutManager =
            LinearLayoutManager(this)

        binding.userRecyclerView.adapter =
            adapter

        binding.btnDeleteSelected.setOnClickListener {

            val selectedUsers =
                adapter.getSelectedUsers()

            if (selectedUsers.isEmpty()) {
                return@setOnClickListener
            }

            viewModel.deleteUsers(
                selectedUsers
            )

            adapter.clearSelection()
        }
    }


    // =========================================================
    // Observe ViewModel
    // =========================================================

    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    // Users

                    adapter.updateList(
                        state.users
                    )

                    binding.txtTotalUsers.text =
                        state.users.size.toString()

                    binding.txtManagers.text =
                        state.users.count {
                            it.role.equals(
                                "MANAGER",
                                ignoreCase = true
                            )
                        }.toString()

                    binding.txtUsers.text =
                        state.users.count {
                            it.role.equals(
                                "USER",
                                ignoreCase = true
                            )
                        }.toString()


                    // Current user

                    state.currentUser?.let { user ->

                        currentRole =
                            user.role

                        adapter.updateCurrentUser(
                            currentUid,
                            currentRole
                        )
                    }


                    // Error

                    state.error?.let { error ->

                        Toast.makeText(
                            this@UserListActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }


    // =========================================================
    // Selection UI
    // =========================================================

    private fun updateSelectionUI(
        count: Int,
        hasSelection: Boolean
    ) {

        binding.btnDeleteSelected.visibility =
            if (hasSelection) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }


    // =========================================================
    // Edit User Dialog
    // =========================================================

    private fun showEditUserDialog(user: User) {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_user_edit,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val editName =
            dialogView.findViewById<EditText>(
                R.id.editDetailName
            )

        val editEmail =
            dialogView.findViewById<EditText>(
                R.id.editDetailEmail
            )

        val btnSave =
            dialogView.findViewById<MaterialButton>(
                R.id.btnSaveDetail
            )

        val btnDelete =
            dialogView.findViewById<MaterialButton>(
                R.id.btnDeleteDetail
            )

        editName.setText(user.name)
        editEmail.setText(user.email)

        btnSave.setOnClickListener {

            val newName =
                editName.text.toString().trim()

            val newEmail =
                editEmail.text.toString().trim()

            if (newName.isEmpty()) {
                editName.error = "Name is required"
                return@setOnClickListener
            }

            if (newEmail.isEmpty()) {
                editEmail.error = "Email is required"
                return@setOnClickListener
            }

            viewModel.updateUser(
                user = user,
                newName = newName,
                newEmail = newEmail
            )

            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            viewModel.deleteUser(user)

            dialog.dismiss()
        }

        dialog.show()
    }
}