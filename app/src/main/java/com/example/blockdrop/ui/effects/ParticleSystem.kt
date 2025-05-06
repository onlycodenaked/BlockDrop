package com.example.blockdrop.ui.effects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Represents a single particle in the particle system
 */
data class Particle(
    val id: Int,
    var position: Offset,
    var velocity: Offset,
    var color: Color,
    var size: Float,
    var alpha: Float,
    var lifetime: Float,
    var maxLifetime: Float
) {
    /**
     * Update the particle's position and properties
     */
    fun update(deltaTime: Float) {
        position += velocity * deltaTime
        lifetime -= deltaTime
        alpha = (lifetime / maxLifetime).coerceIn(0f, 1f)
        size = size * (0.9f + (lifetime / maxLifetime) * 0.1f)
    }
    
    /**
     * Check if the particle is still alive
     */
    fun isAlive(): Boolean = lifetime > 0
}

/**
 * Manages a collection of particles for visual effects
 */
class ParticleSystem {
    // List of active particles
    var particles by mutableStateOf(listOf<Particle>())
        private set
    
    // Counter for generating unique particle IDs
    private var nextParticleId = 0
    
    /**
     * Create an explosion effect at the specified position
     */
    fun createExplosion(
        position: Offset,
        particleCount: Int = 30,
        color: Color,
        radius: Float = 50f,
        minSize: Float = 3f,
        maxSize: Float = 8f,
        minLifetime: Float = 0.5f,
        maxLifetime: Float = 1.5f,
        minSpeed: Float = 50f,
        maxSpeed: Float = 150f
    ) {
        val newParticles = List(particleCount) { i ->
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = minSpeed + Random.nextFloat() * (maxSpeed - minSpeed)
            val velocity = Offset(
                cos(angle) * speed,
                sin(angle) * speed
            )
            val size = minSize + Random.nextFloat() * (maxSize - minSize)
            val lifetime = minLifetime + Random.nextFloat() * (maxLifetime - minLifetime)
            
            Particle(
                id = nextParticleId++,
                position = position,
                velocity = velocity,
                color = color.copy(alpha = 0.8f),
                size = size,
                alpha = 1f,
                lifetime = lifetime,
                maxLifetime = lifetime
            )
        }
        
        particles = particles + newParticles
    }
    
    /**
     * Create a line clear effect across a row
     */
    fun createLineClearEffect(
        y: Float,
        width: Float,
        particleCount: Int = 40,
        color: Color,
        minSize: Float = 2f,
        maxSize: Float = 6f,
        minLifetime: Float = 0.5f,
        maxLifetime: Float = 1.2f
    ) {
        val newParticles = List(particleCount) { i ->
            val x = Random.nextFloat() * width
            val angle = (Random.nextFloat() * Math.PI).toFloat() - (Math.PI / 2).toFloat()
            val speed = 50f + Random.nextFloat() * 100f
            val velocity = Offset(
                cos(angle) * speed,
                sin(angle) * speed
            )
            val size = minSize + Random.nextFloat() * (maxSize - minSize)
            val lifetime = minLifetime + Random.nextFloat() * (maxLifetime - minLifetime)
            
            Particle(
                id = nextParticleId++,
                position = Offset(x, y),
                velocity = velocity,
                color = color.copy(alpha = 0.8f),
                size = size,
                alpha = 1f,
                lifetime = lifetime,
                maxLifetime = lifetime
            )
        }
        
        particles = particles + newParticles
    }
    
    /**
     * Create a collection effect when an orb is collected
     */
    fun createOrbCollectionEffect(
        position: Offset,
        color: Color,
        particleCount: Int = 20,
        minSize: Float = 2f,
        maxSize: Float = 5f
    ) {
        val newParticles = List(particleCount) { i ->
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = 30f + Random.nextFloat() * 70f
            val velocity = Offset(
                cos(angle) * speed,
                sin(angle) * speed
            )
            val size = minSize + Random.nextFloat() * (maxSize - minSize)
            val lifetime = 0.3f + Random.nextFloat() * 0.7f
            
            Particle(
                id = nextParticleId++,
                position = position,
                velocity = velocity,
                color = color.copy(alpha = 0.8f),
                size = size,
                alpha = 1f,
                lifetime = lifetime,
                maxLifetime = lifetime
            )
        }
        
        particles = particles + newParticles
    }
    
    /**
     * Update all particles in the system
     */
    fun update(deltaTime: Float) {
        particles.forEach { it.update(deltaTime) }
        particles = particles.filter { it.isAlive() }
    }
    
    /**
     * Clear all particles
     */
    fun clear() {
        particles = emptyList()
    }
}
