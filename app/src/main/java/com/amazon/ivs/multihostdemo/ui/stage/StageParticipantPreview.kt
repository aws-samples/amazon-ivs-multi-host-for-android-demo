package com.amazon.ivs.multihostdemo.ui.stage

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.extensions.clearAllAnchors
import com.amazon.ivs.multihostdemo.common.extensions.removeSelf
import com.amazon.ivs.multihostdemo.common.extensions.setVisibleOr
import com.amazon.ivs.multihostdemo.common.loadImage
import com.amazon.ivs.multihostdemo.databinding.ItemStageParticipantsPreviewBinding
import timber.log.Timber

private const val MASK_TAG = "Mask_"

class StageParticipantPreview(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var participantAmount = 0
    private var binding = ItemStageParticipantsPreviewBinding.inflate(LayoutInflater.from(context), this, true)

    fun addParticipant(
        view: View?,
        participantId: String,
        name: String,
        avatar: String,
        isVideoOff: Boolean
    ) = with(binding) {
        view?.layoutParams = FrameLayout.LayoutParams(-1, -1)
        participantAmount++
        Timber.d("Participant amount: $participantAmount")
        when (participantAmount) {
            1 -> participant1.let { participant ->
                Timber.d("Adding participant view of 1")
                participant.removeAllViews()
                view.removeSelf()
                view?.let { participant.addView(it) }
                participant.setVisibleOr(!isVideoOff, INVISIBLE)
                participant.tag = participantId
                participant1Mask.tag = "$MASK_TAG$participantId"
                participant1Mask.setVisibleOr(isVideoOff)
                participant1name.text = name
                participant1avatar.loadImage(avatar)
            }
            2 -> participant2.let { participant ->
                participant.removeAllViews()
                view?.let { participant.addView(it) }
                participant.setVisibleOr(!isVideoOff, INVISIBLE)
                participant.tag = participantId
                participant2Mask.tag = "$MASK_TAG$participantId"
                participant2Mask.setVisibleOr(isVideoOff)
                participant2name.text = name
                participant2avatar.loadImage(avatar)
            }
            3 -> participant3.let { participant ->
                participant.removeAllViews()
                view?.let { participant.addView(it) }
                participant.setVisibleOr(!isVideoOff, INVISIBLE)
                participant.tag = participantId
                participant3Mask.tag = "$MASK_TAG$participantId"
                participant3Mask.setVisibleOr(isVideoOff)
                participant3name.text = name
                participant3avatar.loadImage(avatar)
            }
            4 -> participant4.let { participant ->
                participant.removeAllViews()
                view?.let { participant.addView(it) }
                participant.setVisibleOr(!isVideoOff, INVISIBLE)
                participant.tag = participantId
                participant4Mask.tag = "$MASK_TAG$participantId"
                participant4Mask.setVisibleOr(isVideoOff)
                participant4name.text = name
                participant4avatar.loadImage(avatar)
            }
        }
        Timber.d("Participant added: $participantId, $participantAmount")
        updateConstraints()
    }

    fun removeParticipant(participantId: String) {
        findParticipantMask(participantId)?.let { mask ->
            mask.tag = null
            mask.setVisibleOr(false)
        }
        findParticipant(participantId)?.let { participant ->
            participantAmount--
            participant.removeAllViews()
            participant.setVisibleOr(false)
            participant.tag = null
            Timber.d("Removed participant: $participantId. Left: $participantAmount")
            updateConstraints()
        }
    }

    fun updateParticipant(view: View?, participantId: String) {
        findParticipant(participantId)?.let { participant ->
            participant.removeAllViews()
            view?.let { participant.addView(it) }
            Timber.d("Updated participant: $participantId, $id")
        }
    }

    fun onParticipantVideoStateChanged(participantId: String, isCameraOff: Boolean) {
        findParticipant(participantId)?.setVisibleOr(!isCameraOff, INVISIBLE)
        findParticipantMask(participantId)?.let { participant ->
            Timber.d("Participant: $participantId mask visible: $isCameraOff")
            participant.setVisibleOr(isCameraOff)
        }
    }

    private fun findParticipant(participantId: String): CardView? = when {
        binding.participant1.tag == participantId -> binding.participant1
        binding.participant2.tag == participantId -> binding.participant2
        binding.participant3.tag == participantId -> binding.participant3
        binding.participant4.tag == participantId -> binding.participant4
        else -> null
    }

    private fun findParticipantMask(participantId: String): CardView? = when {
        binding.participant1Mask.tag == "$MASK_TAG$participantId" -> binding.participant1Mask
        binding.participant2Mask.tag == "$MASK_TAG$participantId" -> binding.participant2Mask
        binding.participant3Mask.tag == "$MASK_TAG$participantId" -> binding.participant3Mask
        binding.participant4Mask.tag == "$MASK_TAG$participantId" -> binding.participant4Mask
        else -> null
    }

    private fun updateConstraints() = with(binding) {
        Timber.d("Update layout. Participants: $participantAmount")
        val layoutParams1 = participant1.layoutParams as LayoutParams
        val layoutParams2 = participant2.layoutParams as LayoutParams
        val layoutParams3 = participant3.layoutParams as LayoutParams
        val layoutParams4 = participant4.layoutParams as LayoutParams
        layoutParams1.clearAllAnchors()
        layoutParams2.clearAllAnchors()
        layoutParams3.clearAllAnchors()
        layoutParams4.clearAllAnchors()

        when (participantAmount) {
            1 -> {
                layoutParams1.topToTop = participantHolder.id
                layoutParams1.bottomToBottom = participantHolder.id
                layoutParams1.endToEnd = participantHolder.id
                layoutParams1.startToStart = participantHolder.id
                layoutParams1.topMargin = 0
                layoutParams1.bottomMargin = 0
                layoutParams1.marginStart = 0
                participant1.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant1Mask.radius = resources.getDimension(R.dimen.stage_radius_big)
            }
            2 -> {
                layoutParams1.topToTop = participantHolder.id
                layoutParams1.bottomToTop = participant2.id
                layoutParams1.endToEnd = participantHolder.id
                layoutParams1.startToStart = participantHolder.id
                participant1.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant1Mask.radius = resources.getDimension(R.dimen.stage_radius_big)

                layoutParams2.topToBottom = participant1.id
                layoutParams2.bottomToBottom = participantHolder.id
                layoutParams2.endToEnd = participantHolder.id
                layoutParams2.startToStart = participantHolder.id
                layoutParams2.bottomMargin = 0
                participant2.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant2Mask.radius = resources.getDimension(R.dimen.stage_radius_big)
            }
            3 -> {
                layoutParams1.topToTop = participantHolder.id
                layoutParams1.bottomToTop = participant2.id
                layoutParams1.endToEnd = participantHolder.id
                layoutParams1.startToStart = participantHolder.id
                participant1.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant1Mask.radius = resources.getDimension(R.dimen.stage_radius_big)

                layoutParams2.topToBottom = participant1.id
                layoutParams2.bottomToBottom = participantHolder.id
                layoutParams2.endToStart = participant3.id
                layoutParams2.startToStart = participantHolder.id
                layoutParams2.marginEnd = 0
                layoutParams2.bottomMargin = 0
                participant2.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant2Mask.radius = resources.getDimension(R.dimen.stage_radius_big)

                layoutParams3.topToBottom = participant1.id
                layoutParams3.bottomToBottom = participantHolder.id
                layoutParams3.endToEnd = participantHolder.id
                layoutParams3.startToEnd = participant2.id
                layoutParams3.bottomMargin = 0
                participant3.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant3Mask.radius = resources.getDimension(R.dimen.stage_radius_big)
            }
            4 -> {
                layoutParams1.topToTop = participantHolder.id
                layoutParams1.bottomToTop = participant3.id
                layoutParams1.endToStart = participant2.id
                layoutParams1.startToStart = participantHolder.id
                layoutParams1.bottomMargin = 0
                participant1.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant1Mask.radius = resources.getDimension(R.dimen.stage_radius_big)

                layoutParams2.topToTop = participantHolder.id
                layoutParams2.bottomToTop = participant4.id
                layoutParams2.endToEnd = participantHolder.id
                layoutParams2.startToEnd = participant1.id
                participant2.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant2Mask.radius = resources.getDimension(R.dimen.stage_radius_big)

                layoutParams3.topToBottom = participant1.id
                layoutParams3.bottomToBottom = participantHolder.id
                layoutParams3.endToStart = participant4.id
                layoutParams3.startToStart = participantHolder.id
                layoutParams3.bottomMargin = 0
                participant3.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant3Mask.radius = resources.getDimension(R.dimen.stage_radius_big)

                layoutParams4.topToBottom = participant2.id
                layoutParams4.bottomToBottom = participantHolder.id
                layoutParams4.endToEnd = participantHolder.id
                layoutParams4.startToEnd = participant3.id
                layoutParams4.bottomMargin = 0
                participant4.radius = resources.getDimension(R.dimen.stage_radius_big)
                participant4Mask.radius = resources.getDimension(R.dimen.stage_radius_big)
            }
        }

        participant1.layoutParams = layoutParams1
        participant2.layoutParams = layoutParams2
        participant3.layoutParams = layoutParams3
        participant4.layoutParams = layoutParams4
        participant1.invalidate()
        participant2.invalidate()
        participant3.invalidate()
        participant4.invalidate()
    }
}
