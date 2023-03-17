package com.amazon.ivs.multihostdemo.ui.introduction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.multihostdemo.databinding.ItemAvatarBinding
import com.amazon.ivs.multihostdemo.repository.models.Avatar

private val avatarDiff = object : DiffUtil.ItemCallback<Avatar>() {
    override fun areItemsTheSame(oldItem: Avatar, newItem: Avatar) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Avatar, newItem: Avatar) =
        oldItem == newItem
}

class AvatarAdapter(
    private val onAvatarClicked: (AvatarUIModel: Avatar) -> Unit
) : ListAdapter<Avatar, AvatarAdapter.ViewHolder>(avatarDiff) {
    inner class ViewHolder(val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentAvatar = currentList[position]
        with(holder.binding) {
            item = currentAvatar
            avatarItem.setOnClickListener {
                onAvatarClicked(currentAvatar)
            }
        }
    }
}
