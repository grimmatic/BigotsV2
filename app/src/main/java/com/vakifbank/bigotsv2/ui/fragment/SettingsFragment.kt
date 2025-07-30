package com.vakifbank.bigotsv2.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.vakifbank.bigotsv2.databinding.FragmentSettingsBinding
import com.vakifbank.bigotsv2.ui.viewmodel.MainViewModel
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
        loadCurrentSettings()
    }

    private fun setupViews() {
        binding.seekBarMasterVolume.max = 15

        binding.seekBarRefreshRate.max = 15
    }

    private fun setupListeners() {
        binding.seekBarMasterVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateVolumeText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val volume = binding.seekBarMasterVolume.progress
                saveVolumeSettings(volume)
                Toast.makeText(requireContext(), "Ses seviyesi: ${volume * 100 / 15}%", Toast.LENGTH_SHORT).show()
            }
        })

        binding.seekBarRefreshRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRefreshRateText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val rate = getRefreshRateFromProgress(binding.seekBarRefreshRate.progress)
                saveRefreshRateSettings(rate)
                Toast.makeText(requireContext(), "Yenileme hızı: ${rate}s", Toast.LENGTH_SHORT).show()
            }
        })

        binding.btnResetAllSettings.setOnClickListener {
            resetAllSettings()
        }
    }

    private fun loadCurrentSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        val masterVolume = prefs.getInt("master_volume", 15)
        binding.seekBarMasterVolume.progress = masterVolume
        updateVolumeText(masterVolume)

        val refreshRate = prefs.getFloat("refresh_rate", 2.0f)
        val progress = getProgressFromRefreshRate(refreshRate)
        binding.seekBarRefreshRate.progress = progress
        updateRefreshRateText(progress)
    }

    private fun updateVolumeText(volume: Int) {
        val percentage = (volume * 100) / 15
        binding.tvVolumeValue.text = "$percentage%"
    }

    private fun updateRefreshRateText(progress: Int) {
        val rate = getRefreshRateFromProgress(progress)
        binding.tvRefreshRateValue.text = "${rate}s"
    }

    private fun getRefreshRateFromProgress(progress: Int): Float {
        return when (progress) {
            0 -> 1.7f
            1 -> 1.85f
            2 -> 2.0f
            3 -> 2.5f
            4 -> 3.0f
            5 -> 3.5f
            6 -> 4.0f
            7 -> 4.5f
            8 -> 5.0f
            9 -> 5.5f
            10 -> 6.0f
            11 -> 6.5f
            12 -> 7.0f
            13 -> 8.5f
            14 -> 10.0f
            15 -> 15.0f
            else -> 2.0f
        }
    }

    private fun getProgressFromRefreshRate(rate: Float): Int {
        return when {
            rate <= 1.7f -> 0
            rate <= 1.85f -> 1
            rate <= 2.0f -> 2
            rate <= 2.5f -> 3
            rate <= 3.0f -> 4
            rate <= 3.5f -> 5
            rate <= 4.0f -> 6
            rate <= 4.5f -> 7
            rate <= 5.0f -> 8
            rate <= 5.5f -> 9
            rate <= 6.0f -> 10
            rate <= 6.5f -> 11
            rate <= 7.0f -> 12
            rate <= 8.5f -> 13
            rate <= 10.0f -> 14
            else -> 15
        }
    }

    private fun saveVolumeSettings(volume: Int) {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putInt("master_volume", volume).apply()

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.updateAllSoundLevels(volume)
        }
    }

    private fun saveRefreshRateSettings(rate: Float) {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("refresh_rate", rate).apply()

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.updateRefreshRate(rate)
        }
    }

    private fun resetAllSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.setAllThresholds(Constants.Numeric.DEFAULT_ALERT_THRESHOLD)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.updateAllSoundLevels(15)
        }

        saveRefreshRateSettings(2.0f)

        binding.seekBarMasterVolume.progress = 15
        updateVolumeText(15)

        binding.seekBarRefreshRate.progress = 2
        updateRefreshRateText(2)

        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit()
            .putInt("master_volume", 15)
            .putFloat("refresh_rate", 2.0f)
            .apply()

        Toast.makeText(requireContext(), "Tüm ayarlar sıfırlandı", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}