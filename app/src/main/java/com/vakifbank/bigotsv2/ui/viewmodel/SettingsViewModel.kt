package com.vakifbank.bigotsv2.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: CryptoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSettings()
    }

    private fun loadCurrentSettings() {
        val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        val masterVolume = appPrefs.getInt("master_volume", 15)
        val refreshRate = appPrefs.getFloat("refresh_rate", 2.0f)

        _uiState.value = _uiState.value.copy(
            masterVolume = masterVolume,
            refreshRate = refreshRate,
            isLoading = false
        )
    }

    fun updateMasterVolume(volume: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(masterVolume = volume)
            saveMasterVolume(volume)
            repository.updateAllSoundLevels(volume)
        }
    }

    fun updateRefreshRate(rate: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(refreshRate = rate)
            saveRefreshRate(rate)
            repository.updateRefreshRate(rate)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            val defaultVolume = 15
            val defaultRefreshRate = 2.0f
            val defaultThreshold = Constants.Numeric.DEFAULT_ALERT_THRESHOLD

            repository.updateAllThresholds(defaultThreshold)
            repository.updateAllSoundLevels(defaultVolume)
            repository.updateRefreshRate(defaultRefreshRate)

            val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val coinPrefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)

            appPrefs.edit { clear() }
            coinPrefs.edit { clear() }

            appPrefs.edit {
                putInt("master_volume", defaultVolume)
                    .putFloat("refresh_rate", defaultRefreshRate)
            }

            _uiState.value = _uiState.value.copy(
                masterVolume = defaultVolume,
                refreshRate = defaultRefreshRate
            )
        }
    }

    private fun saveMasterVolume(volume: Int) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit { putInt("master_volume", volume) }
    }

    private fun saveRefreshRate(rate: Float) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("refresh_rate", rate).apply()
    }

    fun getVolumePercentage(volume: Int): String {
        val percentage = (volume * 100) / 15
        return "$percentage%"
    }

    fun getRefreshRateText(progress: Int): String {
        val rate = getRefreshRateFromProgress(progress)
        return "${rate}s"
    }

    fun getRefreshRateFromProgress(progress: Int): Float {
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

    fun getProgressFromRefreshRate(rate: Float): Int {
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
}

data class SettingsUiState(
    val masterVolume: Int = 15,
    val refreshRate: Float = 2.0f,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)