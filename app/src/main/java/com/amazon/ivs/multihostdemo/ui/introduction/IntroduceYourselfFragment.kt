package com.amazon.ivs.multihostdemo.ui.introduction

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.extensions.launchUI
import com.amazon.ivs.multihostdemo.common.extensions.navigate
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.FragmentIntroduceYourselfBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class IntroduceYourselfFragment : Fragment(R.layout.fragment_introduce_yourself) {
    private val binding by viewBinding(FragmentIntroduceYourselfBinding::bind)
    private val viewModel by viewModels<IntroductionViewModel>()
    private val adapter by lazy {
        AvatarAdapter { avatar ->
            viewModel.selectAvatar(avatar)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.introduceName.setText(viewModel.displayName.value)
        binding.introduceAvatars.adapter = adapter

        setupListeners()
        setupCollectors()
    }

    private fun setupListeners() = with(binding) {
        introduceName.doOnTextChanged { text, _, _, _ ->
            viewModel.setDisplayName(text.toString())
        }

        signInButton.setOnClickListener {
            viewModel.onSignIn()
            navigate(IntroduceYourselfFragmentDirections.toStageList())
        }
    }

    private fun setupCollectors() = with(binding) {
        launchUI {
            viewModel.avatars.collect { avatars ->
                Timber.d("Collected avatars: $avatars")
                adapter.submitList(avatars)
            }
        }

        launchUI {
            viewModel.isUserInfoSet.collect { isInfoSet ->
                signInButton.isEnabled = isInfoSet
            }
        }
    }
}
