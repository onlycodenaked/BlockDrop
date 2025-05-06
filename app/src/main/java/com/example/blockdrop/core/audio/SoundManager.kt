package com.example.blockdrop.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.blockdrop.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Sound effect types available in the game
 */
enum class SoundEffect {
    MOVE,
    ROTATE,
    DROP,
    LINE_CLEAR,
    LEVEL_UP,
    GAME_OVER,
    MENU_SELECT,
    BOMB_EXPLODE,
    ORB_COLLECT,
    POWER_UP
}

/**
 * Manages sound effects and background music for the game
 */
class SoundManager(private val context: Context) {
    // Sound pool for short sound effects
    private val soundPool: SoundPool by lazy {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
    }
    
    // Media player for background music
    private var musicPlayer: MediaPlayer? = null
    
    // Map of loaded sound effects
    private val soundMap = mutableMapOf<SoundEffect, Int>()
    
    // Sound settings
    var soundEnabled by mutableStateOf(true)
        private set
    
    var musicEnabled by mutableStateOf(true)
        private set
    
    var soundVolume by mutableStateOf(0.7f)
        private set
    
    var musicVolume by mutableStateOf(0.5f)
        private set
    
    /**
     * Initialize the sound manager and load all sound resources
     */
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            // Load sound effects
            loadSoundEffects()
            
            // Initialize music player
            initMusicPlayer()
        }
    }
    
    /**
     * Load all sound effects into the sound pool
     */
    private fun loadSoundEffects() {
        // These would be actual resource IDs in a real implementation
        // For now, we'll use placeholders and add the actual resources later
        soundMap[SoundEffect.MOVE] = soundPool.load(context, R.raw.move, 1)
        soundMap[SoundEffect.ROTATE] = soundPool.load(context, R.raw.rotate, 1)
        soundMap[SoundEffect.DROP] = soundPool.load(context, R.raw.drop, 1)
        soundMap[SoundEffect.LINE_CLEAR] = soundPool.load(context, R.raw.line_clear, 1)
        soundMap[SoundEffect.LEVEL_UP] = soundPool.load(context, R.raw.level_up, 1)
        soundMap[SoundEffect.GAME_OVER] = soundPool.load(context, R.raw.game_over, 1)
        soundMap[SoundEffect.MENU_SELECT] = soundPool.load(context, R.raw.menu_select, 1)
        soundMap[SoundEffect.BOMB_EXPLODE] = soundPool.load(context, R.raw.bomb_explode, 1)
        soundMap[SoundEffect.ORB_COLLECT] = soundPool.load(context, R.raw.orb_collect, 1)
        soundMap[SoundEffect.POWER_UP] = soundPool.load(context, R.raw.power_up, 1)
    }
    
    /**
     * Initialize the music player with background music
     */
    private fun initMusicPlayer() {
        try {
            musicPlayer = MediaPlayer.create(context, R.raw.background_music)
            musicPlayer?.isLooping = true
            musicPlayer?.setVolume(musicVolume, musicVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Play a sound effect
     */
    fun playSound(effect: SoundEffect) {
        if (!soundEnabled) return
        
        val soundId = soundMap[effect] ?: return
        soundPool.play(soundId, soundVolume, soundVolume, 1, 0, 1.0f)
    }
    
    /**
     * Start playing background music
     */
    fun startMusic() {
        if (!musicEnabled) return
        
        try {
            musicPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Pause background music
     */
    fun pauseMusic() {
        try {
            musicPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Set sound effect volume
     */
    fun setSoundVolume(volume: Float) {
        soundVolume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * Set music volume
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        musicPlayer?.setVolume(musicVolume, musicVolume)
    }
    
    /**
     * Enable or disable sound effects
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }
    
    /**
     * Enable or disable background music
     */
    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (enabled) {
            startMusic()
        } else {
            pauseMusic()
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        musicPlayer?.release()
        musicPlayer = null
        soundPool.release()
    }
}
