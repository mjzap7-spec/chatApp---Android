package com.example.test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test.databinding.ItemMessageReceivedBinding
import com.example.test.databinding.ItemMessageSentBinding
import com.example.test.model.PrivateMessage
import java.text.SimpleDateFormat
import java.util.Locale

class PrivateMessageAdapter(
    private val currentUid: String,
    private val otherUserName: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages =
        mutableListOf<PrivateMessage>()

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    override fun getItemViewType(
        position: Int
    ): Int {
        return if (
            messages[position].senderId ==
            currentUid
        ) {
            TYPE_SENT
        } else {
            TYPE_RECEIVED
        }
    }

    inner class SentViewHolder(
        private val binding:
        ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: PrivateMessage) {
            binding.txtMessage.text =
                message.text

            binding.txtAvatar.text = "Y"

            binding.txtMessageTime.text =
                formatTime(message)
        }
    }

    inner class ReceivedViewHolder(
        private val binding:
        ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: PrivateMessage) {
            binding.txtSenderName.text =
                otherUserName

            binding.txtMessage.text =
                message.text

            binding.txtAvatar.text =
                otherUserName
                    .firstOrNull()
                    ?.uppercase()
                    ?: "?"

            binding.txtMessageTime.text =
                formatTime(message)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            SentViewHolder(
                ItemMessageSentBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ),
                    parent,
                    false
                )
            )
        } else {
            ReceivedViewHolder(
                ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = messages[position]

        when (holder) {
            is SentViewHolder ->
                holder.bind(message)

            is ReceivedViewHolder ->
                holder.bind(message)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun updateMessages(
        newMessages: List<PrivateMessage>
    ) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    private fun formatTime(
        message: PrivateMessage
    ): String {
        val date =
            message.createdAt?.toDate()

        return if (date == null) {
            "Sending..."
        } else {
            SimpleDateFormat(
                "h:mm a",
                Locale.getDefault()
            ).format(date)
        }
    }
}