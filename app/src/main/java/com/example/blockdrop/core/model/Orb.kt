package com.example.blockdrop.core.model

import androidx.compose.ui.graphics.Color
import com.example.blockdrop.ui.theme.NeonBlue
import com.example.blockdrop.ui.theme.NeonGreen
import com.example.blockdrop.ui.theme.NeonPink
import kotlin.random.Random

/**
 * Represents a multiplier orb that can appear on the grid
 */
data class Orb(
    val position: Position,
    val multiplier: Int,
    val type: OrbType
) {
    // Get the color based on orb type
    val color: Color
        get() = type.color
        
    companion object {
        /**
         * Create a random orb at the specified position
         */
        fun createRandom(position: Position): Orb {
            // Select a random orb type based on rarity
            val random = Random.nextFloat()
            val type = when {
                random < 0.6f -> OrbType.SMALL // 60% chance
                random < 0.9f -> OrbType.MEDIUM // 30% chance
                else -> OrbType.LARGE // 10% chance
            }
            
            return Orb(position, type.multiplier, type)
        }
    }
}

/**
 * Types of orbs with different multipliers and colors
 */
enum class OrbType(val multiplier: Int, val color: Color) {
    SMALL(2, NeonBlue),
    MEDIUM(3, NeonGreen),
    LARGE(5, NeonPink)
}
