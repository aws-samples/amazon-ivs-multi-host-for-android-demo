package com.amazon.ivs.multihostdemo.common.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.multihostdemo.common.POPUP_TIMEOUT
import com.amazon.ivs.stagebroadcastmanager.common.launchMain
import kotlinx.coroutines.delay

const val ALPHA_VISIBLE = 1f
const val ALPHA_GONE = 0f

fun View.setVisibleOr(show: Boolean, orWhat: Int = GONE) {
    this.visibility = if (show) VISIBLE else orWhat
}

fun View.fadeInAndOut(delay: Long = POPUP_TIMEOUT) = launchMain {
    animateVisibility(true)
    delay(delay)
    animateVisibility(false)
}

fun View.animateVisibility(isVisible: Boolean, orWhat: Int = GONE, onAnimEnd: () -> Unit = {}) {
    if ((visibility == VISIBLE && isVisible) || (visibility == GONE && !isVisible)) {
        onAnimEnd()
        return
    }
    setVisibleOr(true)
    alpha = if (isVisible) ALPHA_GONE else ALPHA_VISIBLE
    animate().setDuration(500L).alpha(if (isVisible) ALPHA_VISIBLE else ALPHA_GONE)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                setVisibleOr(isVisible, orWhat)
                onAnimEnd()
            }
        }).start()
}

fun ConstraintLayout.LayoutParams.clearAllAnchors() {
    startToStart = ConstraintLayout.LayoutParams.UNSET
    startToEnd = ConstraintLayout.LayoutParams.UNSET
    topToTop = ConstraintLayout.LayoutParams.UNSET
    topToBottom = ConstraintLayout.LayoutParams.UNSET
    endToEnd = ConstraintLayout.LayoutParams.UNSET
    endToStart = ConstraintLayout.LayoutParams.UNSET
    bottomToBottom = ConstraintLayout.LayoutParams.UNSET
    bottomToTop = ConstraintLayout.LayoutParams.UNSET
    matchConstraintDefaultHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
    matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
}

fun RecyclerView.scrollDownIfAtBottom() {
    val lastPositionOffset = 1
    val layoutManager = layoutManager as LinearLayoutManager
    val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()
    val itemCount = adapter?.itemCount ?: 0
    val isAtBottom = lastPosition == itemCount - lastPositionOffset
    if (isAtBottom) {
        smoothScrollToPosition(itemCount)
    }
}

fun View?.removeSelf() {
    this ?: return
    val parentView = parent as? ViewGroup ?: return
    parentView.removeView(this)
}
