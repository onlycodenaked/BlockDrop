package com.example.blockdrop.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data class representing a particle for visual effects
 */
data class Particle(
    val id: Int,
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val alpha: Float,
    val rotation: Float,
    val lifetime: Int
)

/**
 * Class to manage particle effects
 */
class ParticleSystem {
    private val particles = mutableStateListOf<Particle>()
    private var nextParticleId = 0
    
    /**
     * Create a line clear particle effect at the specified row
     */
    fun createLineClearEffect(y: Float, width: Float, cellSize: Float) {
        val particleCount = 20
        val centerY = y * cellSize + cellSize / 2
        
        repeat(particleCount) {
            val randomX = Random.nextFloat() * width
            val particle = Particle(
                id = nextParticleId++,
                position = Offset(randomX, centerY),
                velocity = Offset(
                    (Random.nextFloat() - 0.5f) * 10f,
                    (Random.nextFloat() - 0.5f) * 10f
                ),
                color = when (Random.nextInt(3)) {
                    0 -> Color(0xFF00FFFF) // Cyan
                    1 -> Color(0xFFFF00FF) // Magenta
                    else -> Color(0xFFFFFF00) // Yellow
                },
                size = Random.nextFloat() * 6f + 2f,
                alpha = 1f,
                rotation = Random.nextFloat() * 360f,
                lifetime = 30 // Number of frames the particle will live
            )
            particles.add(particle)
        }
    }
    
    /**
     * Create a power-up activation effect at the specified position
     */
    fun createPowerUpEffect(x: Float, y: Float, cellSize: Float, color: Color) {
        val particleCount = 30
        val centerX = x * cellSize + cellSize / 2
        val centerY = y * cellSize + cellSize / 2
        
        repeat(particleCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 8f + 2f
            val particle = Particle(
                id = nextParticleId++,
                position = Offset(centerX, centerY),
                velocity = Offset(
                    cos(angle) * speed,
                    sin(angle) * speed
                ),
                color = color,
                size = Random.nextFloat() * 5f + 3f,
                alpha = 1f,
                rotation = Random.nextFloat() * 360f,
                lifetime = 40
            )
            particles.add(particle)
        }
    }
    
    /**
     * Update all particles (call this every frame)
     */
    fun update() {
        val iterator = particles.iterator()
        val particlesToUpdate = mutableListOf<Particle>()
        
        while (iterator.hasNext()) {
            val particle = iterator.next()
            
            // Update particle position
            val newPosition = Offset(
                particle.position.x + particle.velocity.x,
                particle.position.y + particle.velocity.y
            )
            
            // Update particle properties
            val newLifetime = particle.lifetime - 1
            val newAlpha = particle.alpha * 0.95f
            
            if (newLifetime <= 0) {
                iterator.remove()
            } else {
                particlesToUpdate.add(
                    particle.copy(
                        position = newPosition,
                        lifetime = newLifetime,
                        alpha = newAlpha,
                        rotation = particle.rotation + 5f
                    )
                )
            }
        }
        
        // Update particles in batch to avoid concurrent modification
        particles.clear()
        particles.addAll(particlesToUpdate)
    }
    
    /**
     * Get all current particles
     */
    fun getParticles(): List<Particle> {
        return particles
    }
    
    /**
     * Clear all particles
     */
    fun clear() {
        particles.clear()
    }
}

/**
 * Data class representing a screen shake effect
 */
data class ScreenShake(
    val duration: Int = 0,
    val intensity: Float = 0f
)

/**
 * Composable function to create a screen shake effect
 */
@Composable
fun rememberScreenShake(): MutableState<ScreenShake> {
    return remember { mutableStateOf(ScreenShake()) }
}

/**
 * Calculate screen shake offset
 */
@Composable
fun calculateScreenShakeOffset(screenShake: MutableState<ScreenShake>): Offset {
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    LaunchedEffect(screenShake.value) {
        if (screenShake.value.duration > 0) {
            val intensity = screenShake.value.intensity
            offset = Offset(
                (Random.nextFloat() - 0.5f) * intensity * 2,
                (Random.nextFloat() - 0.5f) * intensity * 2
            )
            
            // Decrease duration
            screenShake.value = screenShake.value.copy(
                duration = screenShake.value.duration - 1
            )
        } else {
            offset = Offset.Zero
        }
    }
    
    return offset
}

/**
 * Trigger a screen shake effect
 */
fun triggerScreenShake(screenShake: MutableState<ScreenShake>, intensity: Float = 10f, duration: Int = 10) {
    screenShake.value = ScreenShake(duration, intensity)
}

/**
 * Data class representing a glow pulse effect
 */
data class GlowPulse(
    val active: Boolean = false,
    val color: Color = Color.White,
    val intensity: Float = 0f
)

/**
 * Composable function to create a glow pulse effect
 */
@Composable
fun rememberGlowPulse(): MutableState<GlowPulse> {
    return remember { mutableStateOf(GlowPulse()) }
}

/**
 * Calculate glow pulse intensity
 */
@Composable
fun calculateGlowPulseIntensity(glowPulse: MutableState<GlowPulse>): Float {
    val infiniteTransition = rememberInfiniteTransition()
    val intensity by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    return if (glowPulse.value.active) intensity * glowPulse.value.intensity else 0f
}

/**
 * Trigger a glow pulse effect
 */
fun triggerGlowPulse(glowPulse: MutableState<GlowPulse>, color: Color, intensity: Float = 0.5f) {
    glowPulse.value = GlowPulse(true, color, intensity)
}

/**
 * Stop a glow pulse effect
 */
fun stopGlowPulse(glowPulse: MutableState<GlowPulse>) {
    glowPulse.value = GlowPulse(false)
}
