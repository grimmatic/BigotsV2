package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.databinding.FragmentCryptoListBinding
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.ui.adapter.CoinAdapter
import com.vakifbank.bigotsv2.ui.viewmodel.BtcturkViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BtcturkFragment : Fragment() {
    private var _binding: FragmentCryptoListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val btcturkViewModel: BtcturkViewModel by viewModels()

    private lateinit var coinAdapter: CoinAdapter
    private var filteredCoins: List<CoinData> = emptyList()
    private var allCoins: List<CoinData> = emptyList()
    private var isSearchExpanded = false

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
        setupCompactControls()
        observeViewModels()
    }

    private fun setupUI() {
        binding.run {
            ivExchangeLogo.setImageResource(btcturkViewModel.getExchangeIcon())
            tvExchangeName.text = btcturkViewModel.getExchangeName()
            tvHeaderExchange.text = btcturkViewModel.getExchangeName()
        }
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter(
            onCoinClick = { coin ->
                btcturkViewModel.showCoinDetailDialog(coin)
            },
            onMoreClick = { coin ->
                btcturkViewModel.showCoinOptionsMenu(coin)
            },
            exchangeType = CoinAdapter.ExchangeType.BTCTURK
        )

        binding.recyclerViewCoins.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coinAdapter
        }
    }

    private fun setupCompactControls() {
        binding.btnSearchToggle.setOnClickListener {
            toggleSearchExpansion()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCoins()
            }
        })

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        binding.btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        binding.btnRetry.setOnClickListener {
            mainViewModel.refreshData()
        }

    }

    private fun toggleSearchExpansion() {
        isSearchExpanded = !isSearchExpanded

        val expandedLayout = binding.layoutSearchExpanded
        val searchButton = binding.btnSearchToggle

        if (isSearchExpanded) {
            expandedLayout.visibility = View.VISIBLE
            expandedLayout.alpha = 0f
            expandedLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            searchButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_clear)
            searchButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_color_light))

            binding.etSearch.requestFocus()

        } else {
            expandedLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    expandedLayout.visibility = View.GONE
                }
                .start()

            searchButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            searchButton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))

            binding.etSearch.setText("")
            clearAllFilters()
        }
    }

    private fun updateChipFilter() {
        filterCoins()
        updateResultsInfo()
        updateSearchButtonState()
    }

    private fun updateSearchButtonState() {


        val searchButton = binding.btnSearchToggle

        if (!isSearchExpanded) {
            searchButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_color_light))
            searchButton.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary_color)
        } else if (!isSearchExpanded) {
            searchButton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            searchButton.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)
        }
    }

    private fun filterCoins() {
        val searchQuery = binding.etSearch.text.toString().lowercase().trim()

        filteredCoins = allCoins.filter { coin ->
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                coin.symbol?.lowercase()?.contains(searchQuery) == true ||
                        coin.name?.lowercase()?.contains(searchQuery) == true
            }



            matchesSearch
        }

        coinAdapter.submitList(filteredCoins)
        updateResultsInfo()
        updateEmptyState()
    }

    private fun updateResultsInfo() {
        val hasFilters = binding.etSearch.text.toString().isNotEmpty()

        if (hasFilters && allCoins.isNotEmpty()) {
            binding.layoutResultsInfo.visibility = View.VISIBLE
            binding.tvResultsInfo.text = "${allCoins.size} sonuçtan ${filteredCoins.size} tanesi gösteriliyor"
        } else {
            binding.layoutResultsInfo.visibility = View.GONE
        }
    }

    private fun updateEmptyState() {
        val hasFilters = binding.etSearch.text.toString().isNotEmpty()
        when {
            filteredCoins.isEmpty() && allCoins.isEmpty() -> {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewCoins.visibility = View.GONE
                binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_search_off)
                binding.tvEmptyStateTitle.text = "Henüz veri yok"
                binding.tvEmptyStateMessage.text = "Servisi başlatın ve verilerin gelmesini bekleyin"
                binding.btnRetry.visibility = View.VISIBLE
            }
            filteredCoins.isEmpty() && hasFilters -> {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewCoins.visibility = View.GONE
                binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_search_off)
                binding.tvEmptyStateTitle.text = "Sonuç bulunamadı"
                binding.tvEmptyStateMessage.text = "Arama kriterlerinizi değiştirmeyi deneyin"
                binding.btnRetry.visibility = View.GONE
            }
            else -> {
                binding.layoutEmptyState.visibility = View.GONE
                binding.recyclerViewCoins.visibility = View.VISIBLE
            }
        }
    }

    private fun clearAllFilters() {
        binding.etSearch.setText("")


        binding.btnFilter.text = "Tümü"
        binding.btnSort.text = "Fark %"

        filterCoins()
        updateSearchButtonState()
    }

    private fun showFilterDialog() {
        val options = arrayOf("Tümü", "Sadece Alarmlar", "Pozitif Fark", "Negatif Fark")

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Filtrele")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        clearAllFilters()
                        binding.btnFilter.text = "Tümü"
                    }
                    1 -> {
                        clearAllFilters()
                        binding.btnFilter.text = "Alarmlar"
                    }
                    2 -> {
                        clearAllFilters()
                        binding.btnFilter.text = "Pozitif"
                    }
                    3 -> {
                        clearAllFilters()
                        binding.btnFilter.text = "Negatif"
                    }
                }
            }
        builder.show()
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "Fark % (Yüksek→Düşük)",
            "Fark % (Düşük→Yüksek)",
            "İsim (A→Z)",
            "İsim (Z→A)",
            "Fiyat (Yüksek→Düşük)",
            "Fiyat (Düşük→Yüksek)"
        )

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Sırala")
            .setItems(options) { _, which ->
                sortCoins(which)
                binding.btnSort.text = when (which) {
                    0 -> "Fark % ↓"
                    1 -> "Fark % ↑"
                    2 -> "İsim ↓"
                    3 -> "İsim ↑"
                    4 -> "Fiyat ↓"
                    5 -> "Fiyat ↑"
                    else -> "Fark %"
                }
            }
        builder.show()
    }

    private fun sortCoins(sortType: Int) {
        filteredCoins = when (sortType) {
            0 -> filteredCoins.sortedByDescending { kotlin.math.abs(it.btcturkDifference ?: 0.0) }
            1 -> filteredCoins.sortedBy { kotlin.math.abs(it.btcturkDifference ?: 0.0) }
            2 -> filteredCoins.sortedBy { it.symbol }
            3 -> filteredCoins.sortedByDescending { it.symbol }
            4 -> filteredCoins.sortedByDescending { it.btcturkPrice ?: 0.0 }
            5 -> filteredCoins.sortedBy { it.btcturkPrice ?: 0.0 }
            else -> filteredCoins
        }
        coinAdapter.submitList(filteredCoins)
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            btcturkViewModel.uiState.collect { btcturkState ->
                allCoins = btcturkState.coinList
                filterCoins()

                binding.tvActiveAlerts.text = btcturkViewModel.getActiveAlertText()

                btcturkState.selectedCoin?.let { coin ->
                    if (btcturkState.showOptionsMenu) {
                        showCoinOptionsMenu(coin)
                    } else {
                        showCoinDetailDialog(coin)
                    }
                }
            }
        }
    }

    private fun showCoinDetailsDialog(coin: CoinData) {
        val dialog = CoinDetailsDialog.newInstance(coin, isFromBtcTurk = true)

        dialog.setOnThresholdChangedListener { threshold ->
            btcturkViewModel.updateCoinThreshold(coin, threshold)
        }

        dialog.setOnSoundLevelChangedListener { soundLevel ->
            btcturkViewModel.updateCoinSoundLevel(coin, soundLevel)
        }

        dialog.setOnDialogDismissedListener {
            btcturkViewModel.hideCoinDetailDialog()
        }

        dialog.show(childFragmentManager, "CoinDetailsDialog")
    }

    private fun showCoinDetailDialog(coin: CoinData) {
        showCoinDetailsDialog(coin)
        btcturkViewModel.hideCoinDetailDialog()
    }

    private fun showCoinOptionsMenu(coin: CoinData) {
        showCoinDetailsDialog(coin)
        btcturkViewModel.hideOptionsMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}