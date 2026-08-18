package com.example.test.ui.user

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test.databinding.ItemUserBinding
import com.example.test.data.model.User

class UserAdapter(
    private var currentUid: String,
    private var currentRole: String,

    private val onEditUserClick: (User) -> Unit,

    private val onSaveName: (
        User,
        String
    ) -> Unit,

    private val onDeleteUser: (
        User
    ) -> Unit,

    private val onSelectionChanged: (
        Int,
        Boolean
    ) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val users = mutableListOf<User>()

    private val selectedUserIds =
        mutableSetOf<String>()

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            user: User,
            position: Int
        ) {
            val isManager =
                currentRole.equals(
                    "MANAGER",
                    ignoreCase = true
                )

            val isSelected =
                selectedUserIds.contains(user.uid)

            showUserInformation(
                user = user,
                position = position
            )

            showSelectionState(
                isSelected = isSelected,
                isManager = isManager
            )

            configureListeners(
                user = user,
                isManager = isManager
            )
        }

        private fun showUserInformation(
            user: User,
            position: Int
        ) {
            binding.txtNo.text =
                (position + 1)
                    .toString()
                    .padStart(2, '0')

            binding.txtName.text =
                user.name.ifBlank { "Unknown user" }

            binding.txtEmail.text =
                user.email.ifBlank { "No email" }

            binding.txtRole.text = user.role.uppercase()

            binding.txtRole.setTextColor(
                if (
                    user.role.equals(
                        "MANAGER",
                        ignoreCase = true
                    )
                ) {
                    Color.parseColor("#B388FF")
                } else {
                    Color.parseColor("#FF9800")
                }
            )

            binding.editName.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        }

        private fun showSelectionState(
            isSelected: Boolean,
            isManager: Boolean
        ) {
            val selectionMode = isSelectionMode()

            binding.checkSelect.visibility =
                if (
                    selectionMode &&
                    isManager
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.checkSelect.isChecked = isSelected
            binding.rowContainer.setBackgroundColor(
                if (isSelected) {
                    Color.parseColor("#293D5A")
                } else {
                    Color.TRANSPARENT
                }
            )

            binding.root.alpha =
                if (isSelected) {
                    0.88f
                } else {
                    1.0f
                }
        }

        private fun configureListeners(
            user: User,
            isManager: Boolean
        ) {
            binding.root.setOnClickListener {
                when {
                    isSelectionMode() &&
                            isManager -> {

                        toggleSelection(user)
                    }

                    else -> {
                        onEditUserClick(user)
                    }
                }
            }

            binding.root.setOnLongClickListener {
                if (isManager) {
                    toggleSelection(user)
                    true
                } else {
                    false
                }
            }

            binding.checkSelect.setOnClickListener {
                if (isManager) {
                    toggleSelection(user)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {
        val binding =
            ItemUserBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int
    ) {
        holder.bind(
            user = users[position],
            position = position
        )
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun updateCurrentUser(
        uid: String,
        role: String
    ) {
        currentUid = uid
        currentRole = role

        notifyDataSetChanged()
    }

    private fun toggleSelection(user: User) {

        if (user.uid.isBlank()) {
            return
        }

        // Don't allow manager to select themselves
        if (user.uid == currentUid) {
            return
        }

        if (selectedUserIds.contains(user.uid)) {

            selectedUserIds.remove(user.uid)

        } else {

            selectedUserIds.add(user.uid)
        }

        notifyDataSetChanged()

        onSelectionChanged(
            selectedUserIds.size,
            selectedUserIds.isNotEmpty()
        )
    }

    fun updateList(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)

        val visibleIds =
            users.map { user ->
                user.uid
            }.toSet()

        selectedUserIds.retainAll(visibleIds)

        notifyDataSetChanged()

        onSelectionChanged(
            selectedUserIds.size,
            selectedUserIds.isNotEmpty()
        )
    }

    fun getSelectedUsers(): List<User> {
        return users.filter { user ->
            selectedUserIds.contains(user.uid)
        }
    }

    fun isSelectionMode(): Boolean {
        return selectedUserIds.isNotEmpty()
    }

    fun clearSelection() {
        selectedUserIds.clear()
        notifyDataSetChanged()

        onSelectionChanged(0, false)
    }
}