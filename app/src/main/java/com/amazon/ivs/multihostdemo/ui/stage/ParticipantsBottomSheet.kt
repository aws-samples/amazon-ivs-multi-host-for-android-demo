package com.amazon.ivs.multihostdemo.ui.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.extensions.launchUI
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.BottomSheetStageParticipantsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

class ParticipantsBottomSheet(
    private val viewModel: StageViewModel
) : BottomSheetDialogFragment() {
    private val binding by viewBinding(BottomSheetStageParticipantsBinding::bind)
    private val adapter by lazy {
        ConnectedParticipantAdapter(
            onMuteClick = viewModel::muteParticipant,
            onRemoveParticipantClick = {
                dismiss()
                viewModel.removeParticipant(it)
            },
            onVideoClick = viewModel::hideVideoOfParticipant,
            showRemoveIcon = viewModel.isHost
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_stage_participants, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.participantsList.adapter = adapter

        setupListeners()
        setupCollectors()
    }

    private fun setupListeners() = with(binding) {
        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupCollectors() {
        launchUI {
            viewModel.connectedParticipants.collectLatest { connectedParticipants ->
                adapter.submitList(connectedParticipants.sortedBy { !it.isLocal })
                Timber.d("New connected participants: $connectedParticipants")
            }
        }
    }
}
