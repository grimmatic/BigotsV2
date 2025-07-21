package com.vakifbank.bigotsv2.ui.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.vakifbank.bigotsv2.HomeFragmentStateAdapter
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding?=null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeFragmentStateAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding= FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewPagerAdapter()
    }

    private val fragmentList = arrayListOf(
        ParibuFragment(), BtcturkFragment(), SettingsFragment()
    )

    private val tabTitles = arrayListOf(
        "Paribu","BTCTurk","Ayarlar"
    )

    private lateinit var tabIcons: ArrayList<Drawable>

    private fun initViewPagerAdapter(){
        tabIcons = arrayListOf(
            resources.getDrawable(R.drawable.paribu, null),
            resources.getDrawable(R.drawable.btcturk, null),
            resources.getDrawable(R.drawable.settings, null)
        )

      /*  val viewPager = binding.vpHome
        adapter = HomeFragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle,fragmentList)
        viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayoutHomeFragment, viewPager) { tab, position ->
            tab.text = tabTitles[position]
            tab.icon= tabIcons[position]
        }.attach()*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}