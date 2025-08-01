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
import com.vakifbank.bigotsv2.ui.viewmodel.FilterType
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.ParibuViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.SortType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParibuFragment : Fragment() {
    private var _binding: FragmentCryptoListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val paribuViewModel: ParibuViewModel by viewModels()

    private lateinit var coinAdapter: CoinAdapter

    private var previousSelectedCoin: CoinData? = null
    private var previousShowOptionsMenu: Boolean = false
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
        setupListeners()
        observeViewModels()
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter(
            onCoinClick = { coin -> paribuViewModel.showCoinDetailDialog(coin) },
            onMoreClick = { coin -> paribuViewModel.showCoinOptionsMenu(coin) },
            exchangeType = CoinAdapter.ExchangeType.PARIBU
        )

        binding.recyclerViewCoins.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coinAdapter
        }
    }

    private fun setupUI() {
        binding.run {
            ivExchangeLogo.setImageResource(paribuViewModel.getExchangeIcon())
            tvExchangeName.text = paribuViewModel.getExchangeName()
            tvHeaderExchange.text = paribuViewModel.getExchangeName()
        }
    }

    private fun setupListeners() {
        // Search toggle
        binding.btnSearchToggle.setOnClickListener {
            paribuViewModel.toggleSearchExpansion()
        }

        // Search text watcher
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                paribuViewModel.updateSearchQuery(s.toString())
            }
        })

        // Filter and sort buttons
        binding.btnFilter.setOnClickListener { showFilterDialog() }
        binding.btnSort.setOnClickListener { showSortDialog() }
        binding.btnClearFilters.setOnClickListener { paribuViewModel.clearAllFilters() }

        // Retry button
        binding.btnRetry.setOnClickListener { mainViewModel.refreshData() }
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe main UI state
            paribuViewModel.uiState.collect { state ->
                updateCoinList(state.coinList)
                updateActiveAlertsCount(state.alertCount)
                updateResultsInfo(
                    state.filteredCoinCount,
                    state.totalCoinCount,
                    state.hasActiveFilters
                )
                updateEmptyState(state.coinList, state.isLoading, state.hasActiveFilters)

                // Dialog handling - BTCTurk gibi basit mantık
                state.selectedCoin?.let { coin ->
                    if (state.showOptionsMenu) {
                        showCoinOptionsMenu(coin)
                    } else {
                        showCoinDetailDialog(coin)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe search expansion state
            paribuViewModel.isSearchExpanded.collect { isExpanded ->
                updateSearchExpansion(isExpanded)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe filter and sort states for button text updates
            paribuViewModel.currentFilterType.collect { filterType ->
                binding.btnFilter.text = paribuViewModel.getFilterButtonText()
                updateSearchButtonState()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            paribuViewModel.currentSortType.collect { sortType ->
                binding.btnSort.text = paribuViewModel.getSortButtonText()
                updateSearchButtonState()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            paribuViewModel.searchQuery.collect { query ->
                if (binding.etSearch.text.toString() != query) {
                    binding.etSearch.setText(query)
                }
                updateSearchButtonState()
            }
        }
    }

    private fun updateCoinList(coins: List<CoinData>) {
        coinAdapter.submitList(coins)
    }

    private fun updateActiveAlertsCount(count: Int) {
        binding.tvActiveAlerts.text = count.toString()
    }

    private fun updateSearchExpansion(isExpanded: Boolean) {
        val expandedLayout = binding.layoutSearchExpanded
        val searchButton = binding.btnSearchToggle

        if (isExpanded) {
            expandedLayout.visibility = View.VISIBLE
            expandedLayout.alpha = 0f
            expandedLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            searchButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_clear)
            searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary_color_light
                )
            )

            binding.etSearch.requestFocus()
            scrollToTabSection()

        } else {
            expandedLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { expandedLayout.visibility = View.GONE }
                .start()

            searchButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )
        }
    }

    private fun updateSearchButtonState() {
        val hasActiveFilters = paribuViewModel.uiState.value.hasActiveFilters
        val isExpanded = paribuViewModel.isSearchExpanded.value
        val searchButton = binding.btnSearchToggle

        if (hasActiveFilters && !isExpanded) {
            searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary_color_light
                )
            )
            searchButton.strokeColor =
                ContextCompat.getColorStateList(requireContext(), R.color.primary_color)
        } else if (!isExpanded) {
            searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )
            searchButton.strokeColor =
                ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)
        }
    }

    private fun updateResultsInfo(filteredCount: Int, totalCount: Int, hasFilters: Boolean) {
        if (hasFilters && totalCount > 0) {
            binding.layoutResultsInfo.visibility = View.VISIBLE
            binding.tvResultsInfo.text = "$totalCount sonuçtan $filteredCount tanesi gösteriliyor"
        } else {
            binding.layoutResultsInfo.visibility = View.GONE
        }
    }

    private fun updateEmptyState(
        coinList: List<CoinData>,
        isLoading: Boolean,
        hasFilters: Boolean
    ) {
        when {
            coinList.isEmpty() && !isLoading && !hasFilters -> {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewCoins.visibility = View.GONE
                binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_search_off)
                binding.tvEmptyStateTitle.text = "Henüz veri yok"
                binding.tvEmptyStateMessage.text =
                    "Servisi başlatın ve verilerin gelmesini bekleyin"
                binding.btnRetry.visibility = View.VISIBLE
            }

            coinList.isEmpty() && hasFilters -> {
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


    private fun scrollToTabSection() {
        try {
            val homeFragment = parentFragment as? HomeFragment
            homeFragment?.scrollToTabSection()
        } catch (e: Exception) {
            // Handle exception silently
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf("Tümü", "Alarmlar", "Pozitif Fark", "Negatif Fark")
        val currentSelection = when (paribuViewModel.currentFilterType.value) {
            FilterType.ALL -> 0
            FilterType.ALERTS_ONLY -> 1
            FilterType.POSITIVE_ONLY -> 2
            FilterType.NEGATIVE_ONLY -> 3
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Filtrele")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val filterType = when (which) {
                    0 -> FilterType.ALL
                    1 -> FilterType.ALERTS_ONLY
                    2 -> FilterType.POSITIVE_ONLY
                    3 -> FilterType.NEGATIVE_ONLY
                    else -> FilterType.ALL
                }
                paribuViewModel.updateFilterType(filterType)
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
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

        val currentSelection = when (paribuViewModel.currentSortType.value) {
            SortType.DIFFERENCE_DESC -> 0
            SortType.DIFFERENCE_ASC -> 1
            SortType.NAME_ASC -> 2
            SortType.NAME_DESC -> 3
            SortType.PRICE_DESC -> 4
            SortType.PRICE_ASC -> 5
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Sırala")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val sortType = when (which) {
                    0 -> SortType.DIFFERENCE_DESC
                    1 -> SortType.DIFFERENCE_ASC
                    2 -> SortType.NAME_ASC
                    3 -> SortType.NAME_DESC
                    4 -> SortType.PRICE_DESC
                    5 -> SortType.PRICE_ASC
                    else -> SortType.DIFFERENCE_DESC
                }
                paribuViewModel.updateSortType(sortType)
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
        builder.show()
    }

    private fun showCoinDetailDialog(coin: CoinData) {
        val dialog = CoinDetailsDialog.newInstance(coin, isFromBtcTurk = false)

        dialog.setOnThresholdChangedListener { threshold ->
            paribuViewModel.updateCoinThreshold(coin, threshold)
        }

        dialog.setOnSoundLevelChangedListener { soundLevel ->
            paribuViewModel.updateCoinSoundLevel(coin, soundLevel)
        }

        dialog.setOnDialogDismissedListener {
            paribuViewModel.hideCoinDetailDialog()
        }

        dialog.show(childFragmentManager, "CoinDetailsDialog")
        paribuViewModel.hideCoinDetailDialog()
    }

    private fun showCoinOptionsMenu(coin: CoinData) {
        val dialog = CoinDetailsDialog.newInstance(coin, isFromBtcTurk = false)

        dialog.setOnThresholdChangedListener { threshold ->
            paribuViewModel.updateCoinThreshold(coin, threshold)
        }

        dialog.setOnSoundLevelChangedListener { soundLevel ->
            paribuViewModel.updateCoinSoundLevel(coin, soundLevel)
        }

        dialog.setOnDialogDismissedListener {
            paribuViewModel.hideOptionsMenu()
        }

        dialog.show(childFragmentManager, "CoinDetailsDialog")
        paribuViewModel.hideOptionsMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}