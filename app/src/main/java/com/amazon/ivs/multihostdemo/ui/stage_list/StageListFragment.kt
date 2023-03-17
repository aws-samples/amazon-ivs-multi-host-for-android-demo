package com.amazon.ivs.multihostdemo.ui.stage_list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.common.extensions.launchUI
import com.amazon.ivs.multihostdemo.common.extensions.navigate
import com.amazon.ivs.multihostdemo.common.extensions.setVisibleOr
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.FragmentStageListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StageListFragment : Fragment(R.layout.fragment_stage_list) {
    private val binding by viewBinding(FragmentStageListBinding::bind)
    private val viewModel by viewModels<StageListViewModel>()
    private val stageListAdapter by lazy {
        StageListAdapter(
            onStageClicked = { stage ->
                navigate(StageListFragmentDirections.openJoinPreviewPopover(stage.stageId, stage.name))
            },
            onStageLongClicked = { stage ->
                viewModel.deleteStage(stage.stageId)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.stagesList.adapter = stageListAdapter
        viewModel.getStages()

        setupListeners()
        setupCollectors()
    }

    private fun setupListeners() = with(binding) {
        createStageButton.setOnClickListener {
            viewModel.createStage()
        }
        stagesRefreshLayout.setOnRefreshListener {
            viewModel.getStages()
        }
    }

    private fun setupCollectors() = with(binding) {
        launchUI {
            viewModel.stageList.collect { stageItems ->
                noStagesFoundLabel.setVisibleOr(stageItems?.isEmpty() == true)
                stageListAdapter.submitList(stageItems ?: emptyList())
            }
        }
        launchUI {
            viewModel.isLoading.collect { isLoading ->
                createStageProgressBar.root.setVisibleOr(isLoading)
            }
        }
        launchUI {
            viewModel.isRefreshingStages.collect { isRefreshingStages ->
                stagesRefreshLayout.isRefreshing = isRefreshingStages
            }
        }
        launchUI {
            viewModel.onNavigateToStage.collect { stageNavArgs ->
                navigate(StageListFragmentDirections.toStage(stageNavArgs))
            }
        }
    }
}
