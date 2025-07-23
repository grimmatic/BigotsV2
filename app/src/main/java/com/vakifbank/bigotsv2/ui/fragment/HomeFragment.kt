package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.data.model.HomeTabConfig
import com.vakifbank.bigotsv2.databinding.FragmentHomeBinding
import com.vakifbank.bigotsv2.ui.adapter.HomeFragmentStateAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModelFactory
import com.vakifbank.bigotsv2.utils.Constants
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeFragmentStateAdapter

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory()
    }

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
            val threshold =
                thresholdText.toDoubleOrNull() ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD
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


        val btcCoin = state.coinList.find { it.symbol == "BTC" }
        currentBinding.tvBtcPrice.text = btcCoin?.let {
            if (state.usdTryRate > 0) {
                "$${String.format("%.2f", it.binancePrice?.div(state.usdTryRate))}"
            } else "$0.00"
        } ?: "$0.00"

        currentBinding.tvUsdTryRate.text = "₺${String.format("%.2f", state.usdTryRate)}"

        if (state.isServiceRunning)
            currentBinding.run {
                fabStartStop.setImageResource(R.drawable.ic_stop)
                tvServiceStatus.text = "Çalışıyor"
                statusIndicator.setBackgroundResource(R.drawable.circle_green)
            }
        else {
            currentBinding.run {
                fabStartStop.setImageResource(R.drawable.ic_play_arrow)
                tvServiceStatus.text = "Durduruldu"
                statusIndicator.setBackgroundResource(R.drawable.circle_red)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}