package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.vakifbank.bigotsv2.domain.model.HomeTabConfig
import com.vakifbank.bigotsv2.databinding.FragmentHomeBinding
import com.vakifbank.bigotsv2.ui.adapter.HomeFragmentStateAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeFragmentStateAdapter

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewPagerAdapter()
        setupFabActions()
        observeViewModel()
    }

    private fun initViewPagerAdapter() {
        val currentBinding = _binding ?: return

        val viewPager = currentBinding.vpHome
        val fragmentList = HomeTabConfig.getFragments()

        adapter = HomeFragmentStateAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            fragmentList
        )

        viewPager.adapter = adapter

        TabLayoutMediator(currentBinding.tabLayoutHomeFragment, viewPager) { tab, position ->
            val tabConfig = HomeTabConfig.values()[position]
            tab.text = tabConfig.title
            tab.icon = ContextCompat.getDrawable(requireContext(), tabConfig.iconRes)
        }.attach()

        currentBinding.tabLayoutHomeFragment.setTabIconTint(null)
    }

    private fun setupFabActions() {
        val currentBinding = _binding ?: return

        currentBinding.fabStartStop.setOnClickListener {
            viewModel.toggleService(requireContext())
        }

        currentBinding.btnSetAllThresholds.setOnClickListener {
            val thresholdText = currentBinding.etThreshold.text.toString()
            val threshold = thresholdText.toDoubleOrNull() ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD
            viewModel.setAllThresholds(threshold)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (_binding != null) {
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: com.vakifbank.bigotsv2.ui.viewmodel.MainUiState) {
        val currentBinding = _binding ?: return


        currentBinding.run {
            tvBtcPrice.text=state.btcPrice
            tvUsdTryRate.text= viewModel.getFormattedUsdTryRate()
            fabStartStop.setImageResource(viewModel.getServiceStatusIcon())
            tvServiceStatus.text= viewModel.getServiceStatusText()
            statusIndicator.setBackgroundResource(viewModel.getStatusIndicatorBackground())
        }

        /*if (state.isRefreshing) {
        }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}