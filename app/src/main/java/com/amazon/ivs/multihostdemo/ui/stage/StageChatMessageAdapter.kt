package com.amazon.ivs.multihostdemo.ui.stage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.multihostdemo.common.loadImage
import com.amazon.ivs.multihostdemo.databinding.ItemMessageBinding
import com.amazon.ivs.multihostdemo.repository.models.StageChatMessage

private val stageChatMessageDiff = object : DiffUtil.ItemCallback<StageChatMessage>() {
    override fun areItemsTheSame(oldItem: StageChatMessage, newItem: StageChatMessage) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: StageChatMessage, newItem: StageChatMessage) =
        oldItem == newItem
}

class StageChatMessageAdapter(
    private val onClick: () -> Unit,
) : ListAdapter<StageChatMessage, StageChatMessageAdapter.ViewHolder>(stageChatMessageDiff) {
    inner class ViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentViewerMessage = currentList[position]
        with(holder.binding) {
            username.text = currentViewerMessage.username
            message.text = currentViewerMessage.message
            userIcon.loadImage(currentViewerMessage.avatarUrl)
            root.setOnClickListener { onClick() }
        }
    }
}
