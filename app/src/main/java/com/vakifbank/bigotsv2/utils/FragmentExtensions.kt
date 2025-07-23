package com.vakifbank.bigotsv2.utils

import android.view.View
import com.vakifbank.bigotsv2.databinding.FragmentCryptoListBinding

fun FragmentCryptoListBinding.updateEmptyState(
    coinList: List<Any>,
    isLoading: Boolean
) {
    if (coinList.isEmpty() && !isLoading) {
        layoutEmptyState.visibility = View.VISIBLE
        recyclerViewCoins.visibility = View.GONE
    } else {
        layoutEmptyState.visibility = View.GONE
        recyclerViewCoins.visibility = View.VISIBLE
    }
}