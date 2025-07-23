package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.databinding.FragmentCryptoListBinding
import com.vakifbank.bigotsv2.ui.adapter.CoinAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.BtcturkViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.BtcturkViewModelFactory
import com.vakifbank.bigotsv2.utils.updateEmptyState
import kotlinx.coroutines.launch

class BtcturkFragment : Fragment() {
    private var _binding: FragmentCryptoListBinding? = null
    private val binding get() = _binding!!

    private val btcturkViewModel: BtcturkViewModel by viewModels {
        BtcturkViewModelFactory()
    }

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
        observeViewModel()
    }

    private fun setupUI() {
        binding.ivExchangeLogo.setImageResource(btcturkViewModel.getExchangeIcon())
        binding.tvExchangeName.text = btcturkViewModel.getExchangeName()
        binding.tvHeaderExchange.text = btcturkViewModel.getExchangeName()
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter(
            onCoinClick = { coin ->
                btcturkViewModel.showCoinDetailDialog(coin)
            },
            onMoreClick = { coin ->
                btcturkViewModel.showCoinOptionsMenu(coin)
            }
        )

        binding.recyclerViewCoins.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coinAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            btcturkViewModel.uiState.collect { state ->
                coinAdapter.submitList(state.coinList)

                binding.updateEmptyState(state.coinList, state.isLoading)
                binding.tvActiveAlerts.text = btcturkViewModel.getActiveAlertText()

                state.selectedCoin?.let { coin ->
                    if (state.showOptionsMenu) {
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