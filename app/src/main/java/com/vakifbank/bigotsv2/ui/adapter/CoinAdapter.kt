package com.vakifbank.bigotsv2.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.databinding.ItemCoinBinding
import kotlin.math.abs

class CoinAdapter(
    private val onCoinClick: (CoinData) -> Unit,
    private val onMoreClick: (CoinData) -> Unit,
    private val exchangeType: ExchangeType = ExchangeType.PARIBU
) : ListAdapter<CoinData, CoinAdapter.CoinViewHolder>(CoinDiffCallback()) {

    enum class ExchangeType {
        PARIBU, BTCTURK, BINANCE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoinViewHolder(binding, onCoinClick, onMoreClick, exchangeType)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CoinViewHolder(
        private val binding: ItemCoinBinding,
        private val onCoinClick: (CoinData) -> Unit,
        private val onMoreClick: (CoinData) -> Unit,
        private val exchangeType: ExchangeType
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(coin: CoinData) {
            binding.apply {
                tvCoinSymbol.text = coin.symbol
                tvCoinName.text = coin.name

                when (exchangeType) {
                    ExchangeType.PARIBU -> {
                        tvParibuPrice.text = "₺${String.format("%.2f", coin.paribuPrice)}"
                        setupDifferenceDisplay(coin.paribuDifference, coin.alertThreshold)
                    }
                    ExchangeType.BTCTURK -> {
                        tvParibuPrice.text = "₺${String.format("%.2f", coin.btcturkPrice)}"
                        setupDifferenceDisplay(coin.btcturkDifference, coin.alertThreshold)
                    }
                    ExchangeType.BINANCE -> {
                        tvParibuPrice.text = "₺${String.format("%.2f", coin.binancePrice)}"
                        setupDifferenceDisplay(0.0, coin.alertThreshold)
                    }
                }

                tvBinancePrice.text = "₺${String.format("%.2f", coin.binancePrice)}"
                tvBinancePriceUsd.text = "$${String.format("%.2f", coin.binancePrice?.div(34.0))}"

                root.setOnClickListener { onCoinClick(coin) }
                ivMoreActions.setOnClickListener { onMoreClick(coin) }
            }
        }

        private fun setupDifferenceDisplay(difference: Double?, alertThreshold: Double?) {
            val diff = difference ?: 0.0
            val threshold = alertThreshold ?: 2.5
            val absDiff = abs(diff)
            val isPositive = diff > 0

            binding.apply {
                tvPriceDifference.text = "${if (isPositive) "+" else ""}${String.format("%.2f", diff)}%"

                val colorRes = when {
                    absDiff > threshold -> {
                        if (isPositive) R.color.success_color else R.color.error_color
                    }
                    else -> R.color.text_secondary
                }
                tvPriceDifference.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

                alertIndicator.visibility = if (absDiff > threshold) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    private class CoinDiffCallback : DiffUtil.ItemCallback<CoinData>() {
        override fun areItemsTheSame(oldItem: CoinData, newItem: CoinData): Boolean {
            return oldItem.symbol == newItem.symbol
        }

        override fun areContentsTheSame(oldItem: CoinData, newItem: CoinData): Boolean {
            return oldItem == newItem
        }
    }
}