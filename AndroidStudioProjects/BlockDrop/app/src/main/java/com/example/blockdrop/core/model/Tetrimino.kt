package com.example.blockdrop.core.model

import androidx.compose.ui.graphics.Color
import com.example.blockdrop.ui.theme.*

/**
 * Represents a Tetrimino (Tetris piece) with its shape, color, and position.
 */
data class Tetrimino(
    val type: TetriminoType,
    var position: Position = Position(4, 0), // Default starting position (center-top)
    var rotation: Int = 0 // 0, 1, 2, or 3 (0°, 90°, 180°, 270°)
) {
    // Get the current shape based on rotation
    val shape: Array<IntArray>
        get() = type.getShape(rotation)
    
    // Get the color of this Tetrimino
    val color: Color
        get() = type.color
    
    // Get the blocks (coordinates) of this Tetrimino in its current position and rotation
    fun getBlocks(): List<Position> {
        val blocks = mutableListOf<Position>()
        
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] == 1) {
                    blocks.add(Position(position.x + x, position.y + y))
                }
            }
        }
        
        return blocks
    }
    
    // Move the Tetrimino
    fun moveLeft() = copy(position = Position(position.x - 1, position.y))
    fun moveRight() = copy(position = Position(position.x + 1, position.y))
    fun moveDown() = copy(position = Position(position.x, position.y + 1))
    
    // Rotate the Tetrimino (clockwise)
    fun rotate(): Tetrimino {
        val newRotation = (rotation + 1) % 4
        return copy(rotation = newRotation)
    }
}

/**
 * Represents a position on the grid
 */
data class Position(val x: Int, val y: Int)

/**
 * Enum representing the different types of Tetriminos
 */
enum class TetriminoType(val color: Color) {
    I(IBlockColor),
    O(OBlockColor),
    T(TBlockColor),
    L(LBlockColor),
    J(JBlockColor),
    S(SBlockColor),
    Z(ZBlockColor);
    
    /**
     * Get the shape of this Tetrimino type based on rotation
     */
    fun getShape(rotation: Int): Array<IntArray> {
        return when (this) {
            I -> when (rotation % 2) {
                0 -> arrayOf(
                    intArrayOf(0, 0, 0, 0),
                    intArrayOf(1, 1, 1, 1),
                    intArrayOf(0, 0, 0, 0),
                    intArrayOf(0, 0, 0, 0)
                )
                else -> arrayOf(
                    intArrayOf(0, 0, 1, 0),
                    intArrayOf(0, 0, 1, 0),
                    intArrayOf(0, 0, 1, 0),
                    intArrayOf(0, 0, 1, 0)
                )
            }
            O -> arrayOf(
                intArrayOf(1, 1),
                intArrayOf(1, 1)
            )
            T -> when (rotation) {
                0 -> arrayOf(
                    intArrayOf(0, 1, 0),
                    intArrayOf(1, 1, 1),
                    intArrayOf(0, 0, 0)
                )
                1 -> arrayOf(
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 1),
                    intArrayOf(0, 1, 0)
                )
                2 -> arrayOf(
                    intArrayOf(0, 0, 0),
                    intArrayOf(1, 1, 1),
                    intArrayOf(0, 1, 0)
                )
                else -> arrayOf(
                    intArrayOf(0, 1, 0),
                    intArrayOf(1, 1, 0),
                    intArrayOf(0, 1, 0)
                )
            }
            L -> when (rotation) {
                0 -> arrayOf(
                    intArrayOf(0, 0, 1),
                    intArrayOf(1, 1, 1),
                    intArrayOf(0, 0, 0)
                )
                1 -> arrayOf(
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 1)
                )
                2 -> arrayOf(
                    intArrayOf(0, 0, 0),
                    intArrayOf(1, 1, 1),
                    intArrayOf(1, 0, 0)
                )
                else -> arrayOf(
                    intArrayOf(1, 1, 0),
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 0)
                )
            }
            J -> when (rotation) {
                0 -> arrayOf(
                    intArrayOf(1, 0, 0),
                    intArrayOf(1, 1, 1),
                    intArrayOf(0, 0, 0)
                )
                1 -> arrayOf(
                    intArrayOf(0, 1, 1),
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 0)
                )
                2 -> arrayOf(
                    intArrayOf(0, 0, 0),
                    intArrayOf(1, 1, 1),
                    intArrayOf(0, 0, 1)
                )
                else -> arrayOf(
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 0),
                    intArrayOf(1, 1, 0)
                )
            }
            S -> when (rotation % 2) {
                0 -> arrayOf(
                    intArrayOf(0, 1, 1),
                    intArrayOf(1, 1, 0),
                    intArrayOf(0, 0, 0)
                )
                else -> arrayOf(
                    intArrayOf(0, 1, 0),
                    intArrayOf(0, 1, 1),
                    intArrayOf(0, 0, 1)
                )
            }
            Z -> when (rotation % 2) {
                0 -> arrayOf(
                    intArrayOf(1, 1, 0),
                    intArrayOf(0, 1, 1),
                    intArrayOf(0, 0, 0)
                )
                else -> arrayOf(
                    intArrayOf(0, 0, 1),
                    intArrayOf(0, 1, 1),
                    intArrayOf(0, 1, 0)
                )
            }
        }
    }
}
