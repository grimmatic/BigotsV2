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
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.ui.viewmodel.ParibuViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class ParibuFragment : Fragment() {
    private var _binding: FragmentCryptoListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val paribuViewModel: ParibuViewModel by viewModels()

    private lateinit var coinAdapter: CoinAdapter
    private var filteredCoins: List<CoinData> = emptyList()
    private var allCoins: List<CoinData> = emptyList()
    private var isSearchExpanded = false

    private var currentFilterType = FilterType.ALL
    private var currentSortType = SortType.DIFFERENCE_DESC

    enum class FilterType {
        ALL, ALERTS_ONLY, POSITIVE_ONLY, NEGATIVE_ONLY
    }

    enum class SortType {
        DIFFERENCE_DESC, DIFFERENCE_ASC, NAME_ASC, NAME_DESC, PRICE_DESC, PRICE_ASC
    }

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
            ivExchangeLogo.setImageResource(paribuViewModel.getExchangeIcon())
            tvExchangeName.text = paribuViewModel.getExchangeName()
            tvHeaderExchange.text = paribuViewModel.getExchangeName()
        }
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

    private fun setupCompactControls() {
        binding.btnSearchToggle.setOnClickListener {
            toggleSearchExpansion()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFiltersAndSort()
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
                .withEndAction {
                    expandedLayout.visibility = View.GONE
                }
                .start()

            searchButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )

            binding.etSearch.setText("")
            clearAllFilters()
        }
    }

    private fun scrollToTabSection() {
        try {
            val homeFragment = parentFragment as? HomeFragment
            homeFragment?.scrollToTabSection()
        } catch (e: Exception) {
        }
    }

    private fun applyFiltersAndSort() {
        val searchQuery = binding.etSearch.text.toString().lowercase().trim()

        var filtered = if (searchQuery.isEmpty()) {
            allCoins
        } else {
            allCoins.filter { coin ->
                coin.symbol?.lowercase()?.contains(searchQuery) == true ||
                        coin.name?.lowercase()?.contains(searchQuery) == true
            }
        }

        filtered = when (currentFilterType) {
            FilterType.ALL -> filtered
            FilterType.ALERTS_ONLY -> filtered.filter { coin ->
                val difference = abs(coin.paribuDifference ?: 0.0)
                val threshold = coin.alertThreshold ?: 2.5
                difference > threshold
            }

            FilterType.POSITIVE_ONLY -> filtered.filter { coin ->
                (coin.paribuDifference ?: 0.0) > 0
            }

            FilterType.NEGATIVE_ONLY -> filtered.filter { coin ->
                (coin.paribuDifference ?: 0.0) < 0
            }
        }

        filteredCoins = when (currentSortType) {
            SortType.DIFFERENCE_DESC -> filtered.sortedByDescending {
                abs(
                    it.paribuDifference ?: 0.0
                )
            }

            SortType.DIFFERENCE_ASC -> filtered.sortedBy { abs(it.paribuDifference ?: 0.0) }
            SortType.NAME_ASC -> filtered.sortedBy { it.symbol }
            SortType.NAME_DESC -> filtered.sortedByDescending { it.symbol }
            SortType.PRICE_DESC -> filtered.sortedByDescending { it.paribuPrice ?: 0.0 }
            SortType.PRICE_ASC -> filtered.sortedBy { it.paribuPrice ?: 0.0 }
        }

        coinAdapter.submitList(filteredCoins)
        updateResultsInfo()
        updateEmptyState()
        updateSearchButtonState()
    }

    private fun updateSearchButtonState() {
        val hasActiveFilters = binding.etSearch.text.toString().isNotEmpty() ||
                currentFilterType != FilterType.ALL ||
                currentSortType != SortType.DIFFERENCE_DESC

        val searchButton = binding.btnSearchToggle

        if (hasActiveFilters && !isSearchExpanded) {
            searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary_color_light
                )
            )
            searchButton.strokeColor =
                ContextCompat.getColorStateList(requireContext(), R.color.primary_color)
        } else if (!isSearchExpanded) {
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

    private fun updateResultsInfo() {
        val hasFilters = binding.etSearch.text.toString().isNotEmpty() ||
                currentFilterType != FilterType.ALL ||
                currentSortType != SortType.DIFFERENCE_DESC

        if (hasFilters && allCoins.isNotEmpty()) {
            binding.layoutResultsInfo.visibility = View.VISIBLE
            binding.tvResultsInfo.text =
                "${allCoins.size} sonuçtan ${filteredCoins.size} tanesi gösteriliyor"
        } else {
            binding.layoutResultsInfo.visibility = View.GONE
        }
    }

    private fun updateEmptyState() {
        val hasFilters = binding.etSearch.text.toString().isNotEmpty() ||
                currentFilterType != FilterType.ALL ||
                currentSortType != SortType.DIFFERENCE_DESC

        when {
            filteredCoins.isEmpty() && allCoins.isEmpty() -> {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewCoins.visibility = View.GONE
                binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_search_off)
                binding.tvEmptyStateTitle.text = "Henüz veri yok"
                binding.tvEmptyStateMessage.text =
                    "Servisi başlatın ve verilerin gelmesini bekleyin"
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
        currentFilterType = FilterType.ALL
        currentSortType = SortType.DIFFERENCE_DESC

        binding.btnFilter.text = "Tümü"
        binding.btnSort.text = "Fark %"

        applyFiltersAndSort()
    }

    private fun showFilterDialog() {
        val options = arrayOf("Tümü", "Alarmlar", "Pozitif Fark", "Negatif Fark")
        val currentSelection = when (currentFilterType) {
            FilterType.ALL -> 0
            FilterType.ALERTS_ONLY -> 1
            FilterType.POSITIVE_ONLY -> 2
            FilterType.NEGATIVE_ONLY -> 3
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Filtrele")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                currentFilterType = when (which) {
                    0 -> FilterType.ALL
                    1 -> FilterType.ALERTS_ONLY
                    2 -> FilterType.POSITIVE_ONLY
                    3 -> FilterType.NEGATIVE_ONLY
                    else -> FilterType.ALL
                }

                binding.btnFilter.text = options[which]
                applyFiltersAndSort()
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

        val currentSelection = when (currentSortType) {
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
                currentSortType = when (which) {
                    0 -> SortType.DIFFERENCE_DESC
                    1 -> SortType.DIFFERENCE_ASC
                    2 -> SortType.NAME_ASC
                    3 -> SortType.NAME_DESC
                    4 -> SortType.PRICE_DESC
                    5 -> SortType.PRICE_ASC
                    else -> SortType.DIFFERENCE_DESC
                }

                binding.btnSort.text = when (which) {
                    0 -> "Fark % ↓"
                    1 -> "Fark % ↑"
                    2 -> "İsim ↓"
                    3 -> "İsim ↑"
                    4 -> "Fiyat ↓"
                    5 -> "Fiyat ↑"
                    else -> "Fark %"
                }

                applyFiltersAndSort()
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
        builder.show()
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            paribuViewModel.uiState.collect { paribuState ->
                allCoins = paribuState.coinList
                applyFiltersAndSort()

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

    private fun showCoinDetailsDialog(coin: CoinData) {
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
    }

    private fun showCoinDetailDialog(coin: CoinData) {
        showCoinDetailsDialog(coin)
        paribuViewModel.hideCoinDetailDialog()
    }

    private fun showCoinOptionsMenu(coin: CoinData) {
        showCoinDetailsDialog(coin)
        paribuViewModel.hideOptionsMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}