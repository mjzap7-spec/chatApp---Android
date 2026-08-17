package com.example.test.ui.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test.data.model.CommunityMessage
import com.example.test.databinding.ItemMessageReceivedBinding
import com.example.test.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityAdapter(
    private val currentUserId: String,
    private val onSelectionChanged: (
        selectedMessages: List<CommunityMessage>
    ) -> Unit,
    private val onAvatarClick: (CommunityMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages =
        mutableListOf<CommunityMessage>()

    private val selectedMessageIds =
        mutableSetOf<String>()

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (
            messages[position].senderId == currentUserId
        ) {
            TYPE_SENT
        } else {
            TYPE_RECEIVED
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: CommunityMessage) {
            binding.txtMessage.text =
                message.text

            binding.txtAvatar.text =
                message.senderName
                    .firstOrNull()
                    ?.uppercase()
                    ?: "?"

            binding.txtAvatar.setOnClickListener {
                if (!isSelectionMode()) {
                    onAvatarClick(message)
                }
            }

            binding.txtMessageTime.text =
                buildTimeText(message)

            showSelectedState(
                bubble = binding.messageBubble,
                avatar = binding.txtAvatar,
                message = message
            )

            binding.root.setOnClickListener {
                if (isSelectionMode()) {
                    toggleSelection(message)
                }
            }

            binding.root.setOnLongClickListener {
                toggleSelection(message)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding:
        ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: CommunityMessage) {
            binding.txtSenderName.text =
                message.senderName.ifBlank {
                    "Unknown user"
                }

            binding.txtMessage.text =
                message.text

            binding.txtAvatar.text =
                message.senderName
                    .firstOrNull()
                    ?.uppercase()
                    ?: "?"

            binding.txtAvatar.setOnClickListener {
                if (!isSelectionMode()) {
                    onAvatarClick(message)
                }
            }

            binding.txtMessageTime.text =
                buildTimeText(message)

            showSelectedState(
                bubble = binding.messageBubble,
                avatar = binding.txtAvatar,
                message = message
            )

            binding.root.setOnClickListener {
                if (isSelectionMode()) {
                    toggleSelection(message)
                }
            }

            binding.root.setOnLongClickListener {
                toggleSelection(message)
                true
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == TYPE_SENT) {
            val binding =
                ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

            SentMessageViewHolder(binding)
        } else {
            val binding =
                ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = messages[position]

        when (holder) {
            is SentMessageViewHolder ->
                holder.bind(message)

            is ReceivedMessageViewHolder ->
                holder.bind(message)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    private fun toggleSelection(
        message: CommunityMessage
    ) {
        if (message.id.isBlank()) {
            return
        }

        if (selectedMessageIds.contains(message.id)) {
            selectedMessageIds.remove(message.id)
        } else {
            selectedMessageIds.add(message.id)
        }

        notifyDataSetChanged()

        onSelectionChanged(
            getSelectedMessages()
        )
    }

    private fun showSelectedState(
        bubble: View,
        avatar: View,
        message: CommunityMessage
    ) {
        val isSelected =
            selectedMessageIds.contains(message.id)

        bubble.alpha =
            if (isSelected) {
                0.58f
            } else {
                1.0f
            }

        avatar.alpha =
            if (isSelected) {
                0.58f
            } else {
                1.0f
            }
    }

    private fun buildTimeText(
        message: CommunityMessage
    ): String {
        val date = message.createdAt?.toDate()

        val time =
            if (date == null) {
                "Sending..."
            } else {
                SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(date)
            }

        return if (message.edited) {
            "$time • Edited"
        } else {
            time
        }
    }

    fun updateMessages(
        newMessages: List<CommunityMessage>
    ) {
        messages.clear()
        messages.addAll(newMessages)

        val validIds =
            messages.map { it.id }.toSet()

        selectedMessageIds.retainAll(validIds)

        notifyDataSetChanged()

        onSelectionChanged(
            getSelectedMessages()
        )
    }

    fun getSelectedMessages():
            List<CommunityMessage> {

        return messages.filter {
            selectedMessageIds.contains(it.id)
        }
    }

    fun isSelectionMode(): Boolean {
        return selectedMessageIds.isNotEmpty()
    }

    fun clearSelection() {
        selectedMessageIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }
}