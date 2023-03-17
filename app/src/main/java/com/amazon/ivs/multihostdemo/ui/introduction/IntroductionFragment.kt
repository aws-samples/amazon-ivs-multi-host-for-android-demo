package com.amazon.ivs.multihostdemo.ui.introduction

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.extensions.navigate
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.FragmentIntroductionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroductionFragment : Fragment(R.layout.fragment_introduction) {
    private val binding by viewBinding(FragmentIntroductionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() = with(binding) {
        getStartedButton.setOnClickListener {
            navigate(IntroductionFragmentDirections.openIntroduceYourselfBottomSheet())
        }

        sourceCodeButton.setOnClickListener {
            navigate(IntroductionFragmentDirections.toWebView())
        }
    }
}
