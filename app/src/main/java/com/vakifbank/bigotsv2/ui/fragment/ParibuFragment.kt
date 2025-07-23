package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.unit.Constraints
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.data.model.Exchange
import com.vakifbank.bigotsv2.databinding.FragmentCryptoListBinding
import com.vakifbank.bigotsv2.ui.adapter.CoinAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModelFactory
import com.vakifbank.bigotsv2.utils.Constants
import kotlinx.coroutines.launch

class ParibuFragment : Fragment() {
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

        binding.ivExchangeLogo.setImageResource(com.vakifbank.bigotsv2.R.drawable.paribu)
        binding.tvExchangeName.text = Constants.ExchangeNames.PARIBU
        binding.tvHeaderExchange.text = Constants.ExchangeNames.PARIBU
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter(
            onCoinClick = { coin ->
                showCoinDetailDialog(coin)
            },
            onMoreClick = { coin ->
                showCoinOptionsMenu(coin)
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
                val paribuCoins = state.coinList.filter { coin ->
                    coin.paribuPrice!! > 0 && kotlin.math.abs(coin.paribuDifference!!) >= 0.1
                }.sortedByDescending { it.paribuDifference?.let { x -> kotlin.math.abs(x) } }

                coinAdapter.submitList(paribuCoins)

                if (paribuCoins.isEmpty() && !state.isLoading) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewCoins.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewCoins.visibility = View.VISIBLE
                }

                val alertCount = state.arbitrageOpportunities.count {
                    it.exchange == Exchange.PARIBU
                }
                binding.tvActiveAlerts.text = "$alertCount aktif alarm"
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