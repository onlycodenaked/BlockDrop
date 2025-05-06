package com.example.blockdrop.core.model

import androidx.compose.ui.graphics.Color

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
     * Check for and clear completed lines, returning the number of lines cleared
     */
    fun clearLines(): Int {
        var linesCleared = 0
        
        // Check each row from bottom to top
        var y = height - 1
        while (y >= 0) {
            if (isLineComplete(y)) {
                clearLine(y)
                shiftLinesDown(y)
                linesCleared++
                // Don't decrement y here to check the same row again after shifting
            } else {
                // Only move to the next row if no line was cleared
                y--
            }
        }
        
        return linesCleared
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
    private fun clearLine(y: Int) {
        for (x in 0 until width) {
            cells[y][x] = 0
        }
    }
    
    /**
     * Shift all lines above the cleared line down
     */
    private fun shiftLinesDown(clearedY: Int) {
        for (y in clearedY downTo 1) {
            cells[y] = cells[y - 1].clone()
        }
        
        // Clear the top line
        clearLine(0)
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
    }
    
    /**
     * Get a copy of the grid cells (for rendering)
     */
    fun getCells(): Array<IntArray> {
        return Array(height) { y -> cells[y].clone() }
    }
}
