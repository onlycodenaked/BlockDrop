package com.example.blockdrop.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.sin
import kotlin.random.Random

/**
 * Manages screen shake effects
 */
class ScreenShake {
    // Current shake offset
    var offset by mutableStateOf(Offset.Zero)
        private set
    
    // Shake intensity (0.0 to 1.0)
    private var intensity = 0f
    
    // Shake duration in seconds
    private var duration = 0f
    
    // Remaining shake time
    private var remainingTime = 0f
    
    // Random seed for shake pattern
    private var seed = Random.nextInt()
    
    /**
     * Start a screen shake effect
     * @param intensity Shake intensity from 0.0 to 1.0
     * @param duration Duration of the shake in seconds
     */
    fun shake(intensity: Float, duration: Float) {
        this.intensity = intensity.coerceIn(0f, 1f)
        this.duration = duration
        this.remainingTime = duration
        this.seed = Random.nextInt()
    }
    
    /**
     * Update the screen shake effect
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun update(deltaTime: Float) {
        if (remainingTime <= 0) {
            offset = Offset.Zero
            return
        }
        
        remainingTime -= deltaTime
        
        // Calculate decay factor (shake reduces over time)
        val progress = (remainingTime / duration).coerceIn(0f, 1f)
        val decayFactor = sin(progress * Math.PI.toFloat())
        
        // Calculate random offset based on intensity and decay
        val maxOffset = 15f * intensity * decayFactor
        val randomX = (Random.nextFloat() * 2 - 1) * maxOffset
        val randomY = (Random.nextFloat() * 2 - 1) * maxOffset
        
        offset = Offset(randomX, randomY)
        
        // If shake is complete, reset offset
        if (remainingTime <= 0) {
            offset = Offset.Zero
        }
    }
    
    /**
     * Reset the screen shake effect
     */
    fun reset() {
        intensity = 0f
        duration = 0f
        remainingTime = 0f
        offset = Offset.Zero
    }
}
