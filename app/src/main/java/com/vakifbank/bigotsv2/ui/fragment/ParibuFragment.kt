package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.databinding.FragmentCryptoListBinding
import com.vakifbank.bigotsv2.ui.adapter.CoinAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.ParibuViewModel
import com.vakifbank.bigotsv2.utils.updateEmptyState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParibuFragment : Fragment() {
    private var _binding: FragmentCryptoListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    private val paribuViewModel: ParibuViewModel by viewModels ()

    private lateinit var coinAdapter: CoinAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCryptoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUI()
        observeViewModels()
    }

    private fun setupUI() {
        binding.run {
            ivExchangeLogo.setImageResource(paribuViewModel.getExchangeIcon())
            tvExchangeName.text = paribuViewModel.getExchangeName()
            tvHeaderExchange.text = paribuViewModel.getExchangeName()}
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter(
            onCoinClick = { coin ->
                paribuViewModel.showCoinDetailDialog(coin)
            },
            onMoreClick = { coin ->
                paribuViewModel.showCoinOptionsMenu(coin)
            },
            exchangeType = CoinAdapter.ExchangeType.PARIBU
        )

        binding.recyclerViewCoins.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coinAdapter
        }
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            paribuViewModel.uiState.collect { paribuState ->
                coinAdapter.submitList(paribuState.coinList)

                binding.updateEmptyState(paribuState.coinList, paribuState.isLoading)
                binding.tvActiveAlerts.text = paribuViewModel.getActiveAlertText()

                paribuState.selectedCoin?.let { coin ->
                    if (paribuState.showOptionsMenu) {
                        showCoinOptionsMenu(coin)
                    } else {
                        showCoinDetailDialog(coin)
                    }
                }
            }
        }
    }

    private fun showCoinDetailDialog(coin: CoinData) {
    }

    private fun showCoinOptionsMenu(coin: CoinData) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}