package com.example.test.ui.friend

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test.data.model.Friend
import com.example.test.databinding.ItemFriendBinding

class FriendAdapter(
    private val onFriendClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    private val friends =
        mutableListOf<Friend>()

    inner class FriendViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend) {
            binding.txtFriendName.text =
                friend.name.ifBlank {
                    "Unknown user"
                }

            binding.txtFriendEmail.text =
                friend.email

            binding.txtFriendAvatar.text =
                friend.name
                    .firstOrNull()
                    ?.uppercase()
                    ?: "?"

            binding.root.setOnClickListener {
                onFriendClick(friend)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FriendViewHolder {
        val binding =
            ItemFriendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FriendViewHolder,
        position: Int
    ) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    fun updateFriends(
        newFriends: List<Friend>
    ) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }
}