package com.vakifbank.bigotsv2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.vakifbank.bigotsv2.databinding.FragmentSettingsBinding
import com.vakifbank.bigotsv2.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settingsViewModel: SettingsViewModel by viewModels()

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
        observeViewModel()
    }

    private fun setupViews() {
        binding.seekBarMasterVolume.max = 15
        binding.seekBarRefreshRate.max = 15
    }

    private fun setupListeners() {
        // Master volume seekbar
        binding.seekBarMasterVolume.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvVolumeValue.text = settingsViewModel.getVolumePercentage(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val volume = binding.seekBarMasterVolume.progress
                settingsViewModel.updateMasterVolume(volume)
                Toast.makeText(
                    requireContext(),
                    "Ses seviyesi: ${settingsViewModel.getVolumePercentage(volume)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Refresh rate seekbar
        binding.seekBarRefreshRate.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvRefreshRateValue.text = settingsViewModel.getRefreshRateText(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = binding.seekBarRefreshRate.progress
                val rate = settingsViewModel.getRefreshRateFromProgress(progress)
                settingsViewModel.updateRefreshRate(rate)
                Toast.makeText(
                    requireContext(),
                    "Yenileme hızı: ${rate}s",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Reset button
        binding.btnResetAllSettings.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            settingsViewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.vakifbank.bigotsv2.ui.viewmodel.SettingsUiState) {
        if (!state.isLoading) {
            // Update volume
            binding.seekBarMasterVolume.progress = state.masterVolume
            binding.tvVolumeValue.text = settingsViewModel.getVolumePercentage(state.masterVolume)

            // Update refresh rate
            val progress = settingsViewModel.getProgressFromRefreshRate(state.refreshRate)
            binding.seekBarRefreshRate.progress = progress
            binding.tvRefreshRateValue.text = "${state.refreshRate}s"
        }
    }

    private fun showResetConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Ayarları Sıfırla")
            .setMessage("Tüm ayarlar varsayılan değerlere döndürülecek. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?")
            .setPositiveButton("Evet") { _, _ ->
                settingsViewModel.resetAllSettings()
                Toast.makeText(requireContext(), "Tüm ayarlar sıfırlandı", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}