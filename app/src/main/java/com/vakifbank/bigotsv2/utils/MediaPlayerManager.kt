package com.vakifbank.bigotsv2.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.util.concurrent.ConcurrentHashMap

class MediaPlayerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MediaPlayerManager? = null

        fun getInstance(context: Context): MediaPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaPlayerManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val mediaPlayers = ConcurrentHashMap<Int, MediaPlayer>()
    private val volumeLevels = ConcurrentHashMap<Int, Float>()

    fun playSound(soundResource: Int, volume: Float = 1.0f) {
        try {
            mediaPlayers[soundResource]?.let { player ->
                try {
                    if (player.isPlaying) {
                        val currentVolume = volumeLevels[soundResource] ?: 0f
                        if (kotlin.math.abs(currentVolume - volume) > 0.01f) {
                            player.setVolume(volume, volume)
                            volumeLevels[soundResource] = volume
                        }
                        return
                    }
                } catch (e: Exception) {
                    player.release()
                    mediaPlayers.remove(soundResource)
                    volumeLevels.remove(soundResource)
                }
            }

            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(
                    context,
                    android.net.Uri.parse("android.resource://${context.packageName}/$soundResource")
                )

                isLooping = true

                prepareAsync()

                setOnPreparedListener { player ->
                    player.setVolume(volume, volume)
                    player.start()
                }

                setOnErrorListener { player, what, extra ->
                    player.release()
                    mediaPlayers.remove(soundResource)
                    volumeLevels.remove(soundResource)
                    true
                }
            }

            mediaPlayers[soundResource] = mediaPlayer
            volumeLevels[soundResource] = volume

        } catch (e: Exception) {
        }
    }


    fun stopSound(soundResource: Int) {
        mediaPlayers[soundResource]?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
            }
            mediaPlayers.remove(soundResource)
            volumeLevels.remove(soundResource)
        }
    }

    fun stopAllSounds() {
        mediaPlayers.values.forEach { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
            }
        }
        mediaPlayers.clear()
        volumeLevels.clear()
    }


    fun releaseAll() {
        stopAllSounds()
    }

    fun playTestSound(soundResource: Int, volume: Float = 1.0f) {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(
                    context,
                    android.net.Uri.parse("android.resource://${context.packageName}/$soundResource")
                )

                isLooping = false

                prepareAsync()

                setOnPreparedListener { player ->
                    player.setVolume(volume, volume)
                    player.start()
                }

                setOnCompletionListener { player ->
                    player.release()
                }

                setOnErrorListener { player, what, extra ->
                    player.release()
                    true
                }
            }

        } catch (e: Exception) {
        }
    }
}