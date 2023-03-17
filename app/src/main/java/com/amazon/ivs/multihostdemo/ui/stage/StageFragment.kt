package com.amazon.ivs.multihostdemo.ui.stage

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.PARTICIPANTS_POPUP_TAG
import com.amazon.ivs.multihostdemo.common.SELF_PARTICIPANT_ID
import com.amazon.ivs.multihostdemo.common.extensions.fadeInAndOut
import com.amazon.ivs.multihostdemo.common.extensions.launchUI
import com.amazon.ivs.multihostdemo.common.extensions.scrollDownIfAtBottom
import com.amazon.ivs.multihostdemo.common.extensions.setVisibleOr
import com.amazon.ivs.multihostdemo.common.loadImage
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.FragmentStageBinding
import com.amazon.ivs.multihostdemo.ui.BackHandler
import com.amazon.ivs.stagebroadcastmanager.models.BroadcastState
import com.amazon.ivs.stagebroadcastmanager.models.StageEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
class StageFragment : Fragment(R.layout.fragment_stage), BackHandler {
    private val binding by viewBinding(FragmentStageBinding::bind)
    private val viewModel by viewModels<StageViewModel>()
    private val navArgs by navArgs<StageFragmentArgs>()
    private val adapter by lazy {
        StageChatMessageAdapter {
            // When clicking on the RecyclerView messages, the only way
            // not to ignore the event is to set this listener to all views
            stageControlSheet.state = STATE_COLLAPSED
        }
    }
    private val stageControlSheet: BottomSheetBehavior<View> by lazy {
        BottomSheetBehavior.from(binding.stageControls.root)
    }
    private val clipboard by lazy {
        getSystemService(requireContext(), ClipboardManager::class.java)!!
    }

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
        stageCreatedPopup.text = if (viewModel.isHost) {
            getString(R.string.stage_created)
        } else {
            getString(R.string.stage_joined)
        }
        stageCreatedPopup.root.fadeInAndOut()
        stageControlSheet.peekHeight = resources.getDimension(R.dimen.stage_controls_peek_height).toInt()
        chatMessages.adapter = adapter
        userIcon.loadImage(viewModel.userAvatar.url)
        if (!viewModel.isHost) {
            stageTitle.text = getString(R.string.stage_name, viewModel.hostName)
        }

        sendMessageField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                viewModel.sendMessage(sendMessageField.text.toString())
                sendMessageField.text.clear()
            }
            false
        }

        stageControlSheet.isDraggable = viewModel.isHost
        stageControls.grabItem.setVisibleOr(viewModel.isHost, View.INVISIBLE)
        stageControls.streamingButton.setVisibleOr(viewModel.isHost)
    }

    private fun setupListeners() = with(binding) {
        root.setOnClickListener {
            stageControlSheet.state = STATE_COLLAPSED
        }

        stageControls.toggleChatButton.setOnClickListener {
            viewModel.toggleChat()
        }

        stageControls.streamingButton.setOnClickListener {
            viewModel.toggleBroadcasting()
        }

        stageControls.toggleMicButton.setOnClickListener {
            viewModel.toggleMute()
        }

        stageControls.toggleVideoButton.setOnClickListener {
            viewModel.toggleVideo()
        }

        stageControls.swapCameraButton.setOnClickListener {
            viewModel.flipCameraDirection()
        }

        stageParticipantButton.setOnClickListener {
            // Note: Making it as a navigation destination will lead into not being able to open it
            ParticipantsBottomSheet(viewModel).show(requireActivity().supportFragmentManager, PARTICIPANTS_POPUP_TAG)
        }

        stageLeaveButton.setOnClickListener {
            viewModel.leaveStage()
        }

        stageControls.copyPlaybackUrlButton.setOnClickListener {
            val playbackUrl = getString(R.string.copy_playback_template, navArgs.stageNavArgs.playbackUrl)
            Timber.d("Copied playback URL: $playbackUrl")
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Playback URL",
                    playbackUrl
                )
            )
        }
    }

    private fun setupCollectors() = with(binding) {
        launchUI {
            viewModel.broadcastState.collect { broadcastState ->
                when (broadcastState) {
                    BroadcastState.LOADING -> {
                        stageControls.streamingButtonText.setVisibleOr(false)
                        stageControls.streamingButtonProgressBar.setVisibleOr(true)
                        stageControls.streamingButton.setBackgroundResource(R.drawable.bg_button_stateful_yellow)
                    }
                    BroadcastState.BROADCASTING -> {
                        stageControls.streamingButtonText.setText(R.string.stop_streaming)
                        stageControls.streamingButtonProgressBar.setVisibleOr(false)
                        stageControls.streamingButtonText.setVisibleOr(true)
                        stageControls.streamingButton.setBackgroundResource(R.drawable.bg_square_button_red)
                    }
                    BroadcastState.NOT_BROADCASTING -> {
                        stageControls.streamingButtonText.setText(R.string.start_streaming)
                        stageControls.streamingButtonProgressBar.setVisibleOr(false)
                        stageControls.streamingButtonText.setVisibleOr(true)
                        stageControls.streamingButton.setBackgroundResource(R.drawable.bg_button_stateful_yellow)
                    }
                }
            }
        }

        launchUI {
            viewModel.broadcastEvents.collect { broadcastEvent ->
                Timber.d("Broadcast event triggered: $broadcastEvent")
                when (broadcastEvent) {
                    is StageEvent.AddParticipantView -> {
                        Timber.d("Added participant view")
                        stageParticipantLayoutHolder.setVisibleOr(true)
                        stageParticipantLayoutHolder.addParticipant(
                            view = broadcastEvent.viewToAdd,
                            participantId = broadcastEvent.participantId,
                            name = broadcastEvent.name,
                            avatar = broadcastEvent.avatar,
                            isVideoOff = broadcastEvent.isVideoOff
                        )
                    }
                    is StageEvent.RemoveParticipantView -> {
                        Timber.d("Removed participant view")
                        stageParticipantLayoutHolder.removeParticipant(broadcastEvent.participantId)
                    }
                    is StageEvent.PreviewUpdated -> {
                        Timber.d("Flipped camera")
                        stageParticipantLayoutHolder.updateParticipant(
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

        launchUI {
            viewModel.isClosingStage.collect { isClosingStage ->
                Timber.d("Is closing stage: $isClosingStage")
                progressBarBackground.root.setVisibleOr(isClosingStage)
            }
        }

        launchUI {
            viewModel.onMicrophoneMuted.collect { isMicrophoneMuted ->
                if (isMicrophoneMuted) {
                    stageControls.toggleMicButton.setImageResource(R.drawable.ic_mic_off)
                    stageControls.toggleMicButton.setBackgroundResource(R.drawable.bg_round_button_white)
                } else {
                    stageControls.toggleMicButton.setImageResource(R.drawable.ic_mic)
                    stageControls.toggleMicButton.setBackgroundResource(R.drawable.bg_round_button_gray)
                }
            }
        }

        launchUI {
            viewModel.onVideoMuted.collect { isVideoMuted ->
                Timber.d("Video muted: $isVideoMuted")
                stageParticipantLayoutHolder.onParticipantVideoStateChanged(SELF_PARTICIPANT_ID, isVideoMuted)
                if (isVideoMuted) {
                    stageControls.toggleVideoButton.setImageResource(R.drawable.ic_videocam_off)
                    stageControls.toggleVideoButton.setBackgroundResource(R.drawable.bg_round_button_white)
                } else {
                    stageControls.toggleVideoButton.setImageResource(R.drawable.ic_videocam)
                    stageControls.toggleVideoButton.setBackgroundResource(R.drawable.bg_round_button_gray)
                }
            }
        }

        launchUI {
            viewModel.isChatOpen.collect { isChatOpen ->
                chat.setVisibleOr(isChatOpen)
                if (isChatOpen) {
                    stageControls.toggleChatButton.setImageResource(R.drawable.ic_chat)
                    stageControls.toggleChatButton.setBackgroundResource(R.drawable.bg_round_button_gray)
                } else {
                    stageControls.toggleChatButton.setImageResource(R.drawable.ic_chat_off)
                    stageControls.toggleChatButton.setBackgroundResource(R.drawable.bg_round_button_white)
                }
            }
        }

        launchUI {
            viewModel.messages.collectLatest { messages ->
                adapter.submitList(messages)
                chatMessages.scrollDownIfAtBottom()
            }
        }

        launchUI {
            viewModel.connectedParticipants.collect { participants ->
                Timber.d("Connected participants of the stage: \n$participants")
                participants.forEach { participant ->
                    if (!participant.isLocal) {
                        stageParticipantLayoutHolder.onParticipantVideoStateChanged(
                            participant.id,
                            participant.isVideoOff
                        )
                    }
                }
            }
        }
    }

    override fun canGoBack() = false
}
