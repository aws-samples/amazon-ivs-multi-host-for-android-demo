package com.amazon.ivs.multihostdemo.ui.stage_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.SELF_PARTICIPANT_ID
import com.amazon.ivs.multihostdemo.common.extensions.launchUI
import com.amazon.ivs.multihostdemo.common.extensions.navigate
import com.amazon.ivs.multihostdemo.common.extensions.setVisibleOr
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.BottomSheetJoinPreviewPopoverBinding
import com.amazon.ivs.stagebroadcastmanager.models.StageEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
class JoinPreviewPopoverBottomSheet : BottomSheetDialogFragment() {
    private val binding by viewBinding(BottomSheetJoinPreviewPopoverBinding::bind)
    private val viewModel by viewModels<JoinPreviewPopoverViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_join_preview_popover, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupListeners()
        setupCollectors()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadCameraPreview()
    }

    private fun setupView() = with(binding) {
        val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    private fun setupListeners() = with(binding) {
        joinButton.setOnClickListener {
            viewModel.joinStage()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        toggleMicButton.setOnClickListener {
            viewModel.toggleMute()
        }

        toggleVideoButton.setOnClickListener {
            viewModel.toggleVideo()
        }

        flipCameraButton.setOnClickListener {
            viewModel.flipCameraDirection()
        }
    }

    private fun setupCollectors() = with(binding) {
        launchUI {
            viewModel.onNavigateToStage.collect { stageNavArgs ->
                navigate(JoinPreviewPopoverBottomSheetDirections.toStage(stageNavArgs))
            }
        }

        launchUI {
            viewModel.onMicrophoneMuted.collect { isMicrophoneMuted ->
                if (isMicrophoneMuted) {
                    toggleMicButton.setImageResource(R.drawable.ic_mic_off)
                    toggleMicButton.setBackgroundResource(R.drawable.bg_round_button_white)
                } else {
                    toggleMicButton.setImageResource(R.drawable.ic_mic)
                    toggleMicButton.setBackgroundResource(R.drawable.bg_round_button_gray)
                }
            }
        }

        launchUI {
            viewModel.onVideoMuted.collect { isVideoMuted ->
                Timber.d("Video muted: $isVideoMuted")
                videoPreviewLayoutHolder.onParticipantVideoStateChanged(SELF_PARTICIPANT_ID, isVideoMuted)
                if (isVideoMuted) {
                    toggleVideoButton.setImageResource(R.drawable.ic_videocam_off)
                    toggleVideoButton.setBackgroundResource(R.drawable.bg_round_button_white)
                } else {
                    toggleVideoButton.setImageResource(R.drawable.ic_videocam)
                    toggleVideoButton.setBackgroundResource(R.drawable.bg_round_button_gray)
                }
            }
        }

        launchUI {
            viewModel.isLoading.collectLatest { isLoading ->
                joinButtonText.setVisibleOr(!isLoading)
                joinButtonProgressBar.setVisibleOr(isLoading)
            }
        }

        launchUI {
            viewModel.broadcastEvents.collect { broadcastEvent ->
                Timber.d("Broadcast event triggered: $broadcastEvent")
                when (broadcastEvent) {
                    is StageEvent.AddParticipantView -> {
                        Timber.d("Added participant view: $videoPreviewLayoutHolder")
                        videoPreviewLayoutHolder.setVisibleOr(true)
                        videoPreviewLayoutHolder.addParticipant(
                            view = broadcastEvent.viewToAdd,
                            participantId = broadcastEvent.participantId,
                            name = broadcastEvent.name,
                            avatar = broadcastEvent.avatar,
                            isVideoOff = broadcastEvent.isVideoOff,
                        )
                    }
                    is StageEvent.RemoveParticipantView -> {
                        Timber.d("Removed participant view")
                        videoPreviewLayoutHolder.removeParticipant(broadcastEvent.participantId)
                    }
                    is StageEvent.PreviewUpdated -> {
                        Timber.d("Flipped camera")
                        videoPreviewLayoutHolder.updateParticipant(
                            view = broadcastEvent.updatedView,
                            participantId = broadcastEvent.participantId
                        )
                    }
                    is StageEvent.StageClosed -> {
                        Timber.d("Stage closed")
                        findNavController().navigateUp()
                    }
                    is StageEvent.StageDisconnected -> {
                        Toast.makeText(
                            context,
                            getString(R.string.stage_disconnected),
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }
}
