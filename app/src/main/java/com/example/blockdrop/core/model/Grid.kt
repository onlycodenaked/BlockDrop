package com.example.blockdrop.core.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Represents the 10x20 game grid
 */
class Grid(
    val width: Int = 10,
    val height: Int = 20
) {
    // Grid cells: 0 = empty, positive values = occupied with color index
    private val cells = Array(height) { IntArray(width) { 0 } }
    
    // Map of color indices to actual colors
    private val colorMap = mutableMapOf<Int, Color>()
    private var nextColorIndex = 1
    
    // Collection of orbs on the grid
    private val orbs = mutableListOf<Orb>()
    
    // Maximum number of orbs allowed on the grid at once
    private val maxOrbs = 4
    
    /**
     * Check if a position is within the grid bounds
     */
    fun isInBounds(position: Position): Boolean {
        return position.x in 0 until width && position.y in 0 until height
    }
    
    /**
     * Check if a position is empty (not occupied by another block)
     */
    fun isEmpty(position: Position): Boolean {
        return if (isInBounds(position)) {
            cells[position.y][position.x] == 0
        } else {
            false // Out of bounds is considered not empty
        }
    }
    
    /**
     * Check if a tetrimino can be placed at its current position
     */
    fun canPlace(tetrimino: Tetrimino): Boolean {
        return tetrimino.getBlocks().all { isEmpty(it) }
    }
    
    /**
     * Place a tetrimino on the grid
     */
    fun place(tetrimino: Tetrimino) {
        // Get a unique color index for this tetrimino
        val colorIndex = getColorIndex(tetrimino.color)
        
        // Place each block of the tetrimino
        tetrimino.getBlocks().forEach { position ->
            if (isInBounds(position)) {
                cells[position.y][position.x] = colorIndex
            }
        }
    }
    
    /**
     * Get the color index for a given color, creating a new one if needed
     */
    private fun getColorIndex(color: Color): Int {
        // Find existing color index
        colorMap.entries.find { it.value == color }?.let {
            return it.key
        }
        
        // Create new color index
        val index = nextColorIndex++
        colorMap[index] = color
        return index
    }
    
    /**
     * Get the color at a specific position
     */
    fun getColorAt(position: Position): Color? {
        if (!isInBounds(position)) return null
        
        val colorIndex = cells[position.y][position.x]
        return if (colorIndex > 0) colorMap[colorIndex] else null
    }
    
    /**
     * Check for and clear completed lines, returning the number of lines cleared and multiplier
     */
    fun clearLines(): Pair<Int, Int> {
        var linesCleared = 0
        var totalMultiplier = 1 // Default multiplier is 1x
        val orbsToRemove = mutableListOf<Orb>()
        
        // Check each row from bottom to top
        var y = height - 1
        while (y >= 0) {
            if (isLineComplete(y)) {
                // Check for orbs in this line before clearing
                orbs.filter { it.position.y == y }.forEach { orb ->
                    totalMultiplier *= orb.multiplier
                    orbsToRemove.add(orb)
                }
                
                clearLine(y)
                shiftLinesDown(y)
                linesCleared++
                // Don't decrement y here to check the same row again after shifting
            } else {
                // Only move to the next row if no line was cleared
                y--
            }
        }
        
        // Track if we removed the 4th orb
        val hadFourOrbs = orbs.size == maxOrbs
        val willHaveLessThanMax = orbs.size - orbsToRemove.size < maxOrbs
        
        // Remove collected orbs
        orbs.removeAll(orbsToRemove)
        
        // Try to spawn new orbs
        if (orbs.size < maxOrbs) {
            // Higher chance to spawn if we just removed the 4th orb
            val spawnChance = if (hadFourOrbs && willHaveLessThanMax) 0.5f else 0.3f
            
            // Try to spawn with the calculated chance
            if (Random.nextFloat() < spawnChance) {
                trySpawnOrb()
            }
        }
        
        return Pair(linesCleared, totalMultiplier)
    }
    
    /**
     * Check if a line is complete (all cells filled)
     */
    private fun isLineComplete(y: Int): Boolean {
        return cells[y].all { it > 0 }
    }
    
    /**
     * Clear a specific line
     */
    fun clearLine(y: Int) {
        for (x in 0 until width) {
            cells[y][x] = 0
        }
    }
    
    /**
     * Clear a specific cell
     */
    fun clearCell(position: Position) {
        if (isInBounds(position)) {
            cells[position.y][position.x] = 0
        }
    }
    
    /**
     * Shift all lines above the cleared line down
     */
    fun shiftLinesDown(clearedY: Int) {
        for (y in clearedY downTo 1) {
            cells[y] = cells[y - 1].clone()
        }
        
        // Clear the top line
        clearLine(0)
        
        // Shift orbs down as well
        shiftOrbsDown(clearedY)
    }
    
    /**
     * Check if the game is over (blocks stacked to the top)
     */
    fun isGameOver(): Boolean {
        // Game is over if any cell in the top row is filled
        return cells[0].any { it > 0 }
    }
    
    /**
     * Reset the grid
     */
    fun reset() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                cells[y][x] = 0
            }
        }
        colorMap.clear()
        nextColorIndex = 1
        orbs.clear()
    }
    
    /**
     * Get a copy of the grid cells (for rendering)
     */
    fun getCells(): Array<IntArray> {
        return Array(height) { y -> cells[y].clone() }
    }
    
    /**
     * Get all orbs on the grid
     */
    fun getOrbs(): List<Orb> {
        return orbs.toList()
    }
    
    /**
     * Try to spawn a new orb at a random empty position
     */
    private fun trySpawnOrb() {
        // Only spawn if we have fewer than the maximum
        if (orbs.size >= maxOrbs) return
        
        // Calculate the midpoint of the grid (excluding the top 4 rows)
        val midpoint = (height + 4) / 2
        
        // Separate empty positions into upper and lower halves
        val upperHalfPositions = mutableListOf<Position>()
        val lowerHalfPositions = mutableListOf<Position>()
        
        // Find empty positions (avoid top 4 rows to not interfere with spawning)
        for (y in 4 until height) {
            for (x in 0 until width) {
                val position = Position(x, y)
                if (isEmpty(position) && !isOrbAt(position)) {
                    if (y < midpoint) {
                        upperHalfPositions.add(position)
                    } else {
                        lowerHalfPositions.add(position)
                    }
                }
            }
        }
        
        // Determine which half to spawn in (75% chance for lower half if available)
        val position = when {
            lowerHalfPositions.isEmpty() && upperHalfPositions.isEmpty() -> null
            lowerHalfPositions.isEmpty() -> upperHalfPositions[Random.nextInt(upperHalfPositions.size)]
            upperHalfPositions.isEmpty() -> lowerHalfPositions[Random.nextInt(lowerHalfPositions.size)]
            else -> {
                // 75% chance to spawn in lower half, 25% chance for upper half
                if (Random.nextFloat() < 0.75f) {
                    lowerHalfPositions[Random.nextInt(lowerHalfPositions.size)]
                } else {
                    upperHalfPositions[Random.nextInt(upperHalfPositions.size)]
                }
            }
        }
        
        // Spawn the orb if we found a valid position
        position?.let {
            orbs.add(Orb.createRandom(it))
        }
    }
    
    /**
     * Check if there is an orb at the specified position
     */
    private fun isOrbAt(position: Position): Boolean {
        return orbs.any { it.position.x == position.x && it.position.y == position.y }
    }
    
    /**
     * Get the orb at the specified position, or null if there is none
     */
    fun getOrbAt(position: Position): Orb? {
        return orbs.find { it.position.x == position.x && it.position.y == position.y }
    }
    
    /**
     * Shift orbs down when lines are cleared
     */
    private fun shiftOrbsDown(clearedY: Int) {
        // Update positions of orbs above the cleared line
        orbs.forEach { orb ->
            if (orb.position.y < clearedY) {
                // Create a new orb with updated position
                val newPosition = Position(orb.position.x, orb.position.y + 1)
                val index = orbs.indexOf(orb)
                orbs[index] = Orb(newPosition, orb.multiplier, orb.type)
            }
        }
    }
}
