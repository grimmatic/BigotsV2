package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vakifbank.bigotsv2.data.model.Exchange
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.databinding.FragmentCryptoListBinding
import com.vakifbank.bigotsv2.ui.adapter.CoinAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch

class BtcturkFragment : Fragment() {
    private var _binding: FragmentCryptoListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory()
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
        observeViewModel()

        binding.ivExchangeLogo.setImageResource(com.vakifbank.bigotsv2.R.drawable.btcturk)
        binding.tvExchangeName.text = "BTCTurk"
        binding.tvHeaderExchange.text="BTCTurk"

    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter(
            onCoinClick = { coin ->
            },
            onMoreClick = { coin ->
            }
        )

        binding.recyclerViewCoins.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coinAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val btcturkCoins = state.coinList.filter { coin ->
                    coin.btcturkPrice!! > 0 && kotlin.math.abs(coin.btcturkDifference!!) >= 0.1
                }.sortedByDescending { it.btcturkDifference?.let { x -> kotlin.math.abs(x) } }

                coinAdapter.submitList(btcturkCoins)

                if (btcturkCoins.isEmpty() && !state.isLoading) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewCoins.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewCoins.visibility = View.VISIBLE
                }

                val alertCount = state.arbitrageOpportunities.count {
                    it.exchange == Exchange.BTCTURK
                }
                binding.tvActiveAlerts.text = "$alertCount aktif alarm"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}