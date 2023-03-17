package com.amazon.ivs.multihostdemo.ui.stage_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.multihostdemo.databinding.ItemStageListBinding
import com.amazon.ivs.multihostdemo.repository.models.Stage

private val stageDiff = object : DiffUtil.ItemCallback<Stage>() {
    override fun areItemsTheSame(oldItem: Stage, newItem: Stage) =
        oldItem.stageId == newItem.stageId

    override fun areContentsTheSame(oldItem: Stage, newItem: Stage) =
        oldItem == newItem
}

class StageListAdapter(
    private val onStageClicked: (Stage) -> Unit,
    private val onStageLongClicked: (Stage) -> Unit,
) : ListAdapter<Stage, StageListAdapter.ViewHolder>(stageDiff) {
    inner class ViewHolder(val binding: ItemStageListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemStageListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentStage = currentList[position]
        holder.binding.userItem = currentStage
        holder.binding.root.setOnClickListener {
            onStageClicked(currentStage)
        }
        holder.binding.root.setOnLongClickListener {
            onStageLongClicked(currentStage)
            true
        }
    }
}
