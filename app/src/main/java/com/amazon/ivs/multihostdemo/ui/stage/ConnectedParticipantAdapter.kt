package com.amazon.ivs.multihostdemo.ui.stage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.multihostdemo.common.extensions.setVisibleOr
import com.amazon.ivs.multihostdemo.databinding.ItemConnectedParticipantBinding
import com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant

private val connectedParticipantDiff = object : DiffUtil.ItemCallback<ConnectedParticipant>() {
    override fun areItemsTheSame(oldItem: ConnectedParticipant, newItem: ConnectedParticipant) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ConnectedParticipant, newItem: ConnectedParticipant) =
        oldItem == newItem
}

class ConnectedParticipantAdapter(
    private val onRemoveParticipantClick: (ConnectedParticipant) -> Unit,
    private val onVideoClick: (ConnectedParticipant) -> Unit,
    private val onMuteClick: (ConnectedParticipant) -> Unit,
    private val showRemoveIcon: Boolean
) : ListAdapter<ConnectedParticipant, ConnectedParticipantAdapter.ViewHolder>(connectedParticipantDiff) {
    inner class ViewHolder(val binding: ItemConnectedParticipantBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemConnectedParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connectedParticipant = currentList[position]
        holder.binding.participantItem = connectedParticipant
        holder.binding.root.tag = connectedParticipant
        holder.binding.removeButton.setVisibleOr(showRemoveIcon)
        holder.binding.removeButton.setOnClickListener {
            onRemoveParticipantClick(holder.binding.root.tag as ConnectedParticipant)
        }

        holder.binding.videocamOffIcon.setOnClickListener {
            onVideoClick(holder.binding.root.tag as ConnectedParticipant)
        }

        holder.binding.micOffIcon.setOnClickListener {
            onMuteClick(holder.binding.root.tag as ConnectedParticipant)
        }
    }
}
