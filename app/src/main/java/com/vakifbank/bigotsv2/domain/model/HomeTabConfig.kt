package com.vakifbank.bigotsv2.domain.model

import androidx.fragment.app.Fragment
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.ui.fragment.BtcturkFragment
import com.vakifbank.bigotsv2.ui.fragment.ParibuFragment
import com.vakifbank.bigotsv2.ui.fragment.SettingsFragment
import com.vakifbank.bigotsv2.utils.Constants

enum class HomeTabConfig(
    val title: String,
    val iconRes: Int,
    val fragmentFactory: () -> Fragment
) {
    PARIBU(
        title = Constants.ExchangeNames.PARIBU,
        iconRes = R.drawable.paribu,
        fragmentFactory = { ParibuFragment() }
    ),

    BTCTURK(
        title = Constants.ExchangeNames.BTCTURK,
        iconRes = R.drawable.btcturk,
        fragmentFactory = { BtcturkFragment() }
    ),

    SETTINGS(
        title = "Ayarlar",
        iconRes = R.drawable.ic_settings,
        fragmentFactory = { SettingsFragment() }
    );

    companion object {
        fun getFragments(): List<Fragment> = values().map { it.fragmentFactory() }
    }
}