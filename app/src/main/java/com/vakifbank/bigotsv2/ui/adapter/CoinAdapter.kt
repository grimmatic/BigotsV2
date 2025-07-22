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
    private val onMoreClick: (CoinData) -> Unit
) : ListAdapter<CoinData, CoinAdapter.CoinViewHolder>(CoinDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoinViewHolder(binding, onCoinClick, onMoreClick)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CoinViewHolder(
        private val binding: ItemCoinBinding,
        private val onCoinClick: (CoinData) -> Unit,
        private val onMoreClick: (CoinData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(coin: CoinData) {
            binding.apply {
                tvCoinSymbol.text = coin.symbol
                tvCoinName.text = coin.name
                tvParibuPrice.text = "₺${String.format("%.2f", coin.paribuPrice)}"
                tvBinancePrice.text = "₺${String.format("%.2f", coin.binancePrice)}"
                tvBinancePriceUsd.text = "$${String.format("%.2f", coin.binancePrice / 34.0)}"

                // En yüksek arbitraj farkını göster
                val maxDifference = maxOf(abs(coin.paribuDifference), abs(coin.btcturkDifference))
                val isPositive = coin.paribuDifference > 0 || coin.btcturkDifference > 0

                tvPriceDifference.text =
                    "${if (isPositive) "+" else ""}${String.format("%.2f", maxDifference)}%"

                val colorRes = when {
                    maxDifference > coin.alertThreshold -> {
                        if (isPositive) R.color.success_color else R.color.error_color
                    }

                    else -> R.color.text_secondary
                }
                tvPriceDifference.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

                alertIndicator.visibility = if (maxDifference > coin.alertThreshold) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                root.setOnClickListener { onCoinClick(coin) }
                ivMoreActions.setOnClickListener { onMoreClick(coin) }
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