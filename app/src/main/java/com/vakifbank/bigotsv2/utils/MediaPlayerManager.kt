package com.vakifbank.bigotsv2.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
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
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }

            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$soundResource"))
                prepareAsync()

                setOnPreparedListener { player ->
                    player.setVolume(volume, volume)
                    player.start()
                }

                setOnCompletionListener { player ->
                    player.release()
                    mediaPlayers.remove(soundResource)
                }

                setOnErrorListener { player, what, extra ->
                    Log.e("MediaPlayerManager", "Error playing sound: $what, $extra")
                    player.release()
                    mediaPlayers.remove(soundResource)
                    true
                }
            }

            mediaPlayers[soundResource] = mediaPlayer
            volumeLevels[soundResource] = volume

        } catch (e: Exception) {
            Log.e("MediaPlayerManager", "Error creating MediaPlayer", e)
        }
    }

    fun updateVolume(soundResource: Int, volume: Float) {
        volumeLevels[soundResource] = volume
        mediaPlayers[soundResource]?.setVolume(volume, volume)
    }

    fun stopSound(soundResource: Int) {
        mediaPlayers[soundResource]?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            mediaPlayers.remove(soundResource)
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
                Log.e("MediaPlayerManager", "Error stopping player", e)
            }
        }
        mediaPlayers.clear()
    }

    fun releaseAll() {
        stopAllSounds()
        volumeLevels.clear()
    }
}