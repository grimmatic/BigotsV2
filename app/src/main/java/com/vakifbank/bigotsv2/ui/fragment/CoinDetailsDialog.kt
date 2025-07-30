package com.vakifbank.bigotsv2.ui.fragment

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.databinding.FragmentCoinDetailsDialogBinding
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.utils.Constants
import com.vakifbank.bigotsv2.utils.SoundMapping

class CoinDetailsDialog : DialogFragment() {

    private var _binding: FragmentCoinDetailsDialogBinding? = null
    private val binding get() = _binding!!

    private var coinData: CoinData? = null
    private var isFromBtcTurk: Boolean = false
    private var mediaPlayer: MediaPlayer? = null

    private var onThresholdChanged: ((Double) -> Unit)? = null
    private var onSoundLevelChanged: ((Int) -> Unit)? = null
    private var onDialogDismissed: (() -> Unit)? = null

    companion object {
        private const val ARG_COIN_DATA = "coin_data"
        private const val ARG_IS_BTC_TURK = "is_btc_turk"

        fun newInstance(
            coinData: CoinData,
            isFromBtcTurk: Boolean = false
        ): CoinDetailsDialog {
            return CoinDetailsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COIN_DATA, coinData)
                    putBoolean(ARG_IS_BTC_TURK, isFromBtcTurk)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentCoinDetailsDialogBinding.inflate(layoutInflater)

        coinData = arguments?.getParcelable(ARG_COIN_DATA)
        isFromBtcTurk = arguments?.getBoolean(ARG_IS_BTC_TURK, false) == true

        val dialog = Dialog(requireContext())
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupViews()
        setupClickListeners()

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onDialogDismissed?.invoke()
    }

    private fun setupViews() {
        coinData?.let { coin ->
            binding.tvCoinName.text = coin.name
            binding.tvCoinSymbol.text = coin.symbol

            val currentThreshold = coin.alertThreshold ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD
            binding.etThreshold.setText(currentThreshold.toString())

            binding.seekBarThreshold.progress = (currentThreshold * 10).toInt()
            binding.seekBarThreshold.max = 100

            val currentSoundLevel = coin.soundLevel ?: Constants.Numeric.DEFAULT_SOUND_LEVEL
            binding.seekBarVolume.progress = currentSoundLevel
            binding.seekBarVolume.max = 15

            setupExchangeButtons(coin)
        }
    }

    private fun setupExchangeButtons(coin: CoinData) {
        if (isFromBtcTurk) {
            binding.btnExchange1.text = "BTCTurk Parite"
            binding.btnExchange1.setBackgroundColor(Color.parseColor("#1e88e5"))
            binding.btnExchange2.text = "BTCTurk Cüzdan"
            binding.btnExchange2.setBackgroundColor(Color.parseColor("#1e88e5"))
        } else {
            binding.btnExchange1.text = "Paribu Parite"
            binding.btnExchange1.setBackgroundColor(Color.parseColor("#2E7D32"))
            binding.btnExchange2.text = "Paribu Cüzdan"
            binding.btnExchange2.setBackgroundColor(Color.parseColor("#2E7D32"))
        }

        binding.btnBinanceTrade.text = "Binance Parite"
        binding.btnBinanceWallet.text = "Binance Cüzdan"
    }

    private fun setupClickListeners() {
        binding.seekBarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val threshold = progress / 10.0
                binding.etThreshold.setText(threshold.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                playTestSound(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnExchange1.setOnClickListener {
            openExchangeApp(true)
        }

        binding.btnExchange2.setOnClickListener {
            openExchangeWallet(true)
        }

        binding.btnBinanceTrade.setOnClickListener {
            openBinanceTrade()
        }

        binding.btnBinanceWallet.setOnClickListener {
            openBinanceWallet()
        }
    }

    private fun playTestSound(volume: Int) {
        coinData?.symbol?.let { symbol ->
            val soundRes = SoundMapping.getSoundResource(symbol)
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(requireContext(), soundRes)
                val volumeLevel = volume / 15.0f
                mediaPlayer?.setVolume(volumeLevel, volumeLevel)
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveSettings() {
        try {
            val threshold = binding.etThreshold.text.toString().toDouble()
            val soundLevel = binding.seekBarVolume.progress

            onThresholdChanged?.invoke(threshold)
            onSoundLevelChanged?.invoke(soundLevel)

            Toast.makeText(requireContext(), "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Geçerli bir eşik değeri girin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExchangeApp(isTrade: Boolean) {
        coinData?.symbol?.let { symbol ->
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                if (isFromBtcTurk) {
                    val btcTurkSymbol = symbol.lowercase() + "try"
                    val url = if (isTrade) {
                       // "btcturkpro://host/trade/$btcTurkSymbol"
                        "btcturkpro://deeplink?screenId=TRADE_BTCTRY"

                        //web sayfası intenti =                         "https://kripto.btcturk.com/pro/al-sat/NEO_TRY"
                    }

                    else {
                        "btcturkpro://host/wallet"
                    }
                    intent.data = Uri.parse(url)
                } else {
                    val paribuSymbol = symbol.lowercase() + "_tl"
                    val url = if (isTrade) {
                        "paribu://markets/$paribuSymbol"
                    } else {
                        "paribu://wallet/${symbol.lowercase()}/deposit"
                    }
                    intent.data = Uri.parse(url)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Uygulama bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openExchangeWallet(isExchange1: Boolean) {
        openExchangeApp(false)
    }

    private fun openBinanceTrade() {
        coinData?.symbol?.let { symbol ->
            try {
                val binanceSymbol = symbol.lowercase() + "usdt"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("bnc://app.binance.com/trade/trade?at=spot&symbol=$binanceSymbol")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Binance uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openBinanceWallet() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("bnc://app.binance.com/funds/withdrawChooseCoin")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Binance uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun setOnThresholdChangedListener(listener: (Double) -> Unit) {
        onThresholdChanged = listener
    }

    fun setOnSoundLevelChangedListener(listener: (Int) -> Unit) {
        onSoundLevelChanged = listener
    }
    fun setOnDialogDismissedListener(listener: () -> Unit) {
        onDialogDismissed = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}