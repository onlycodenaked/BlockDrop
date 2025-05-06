package com.example.blockdrop.core.engine

import com.example.blockdrop.core.model.Grid
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.Tetrimino
import com.example.blockdrop.core.model.TetriminoType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Main game engine that manages the game state and logic
 */
class GameEngine {
    // Game grid (10x20)
    private val grid = Grid(10, 20)
    
    // Current falling tetrimino
    private var currentTetrimino: Tetrimino? = null
    
    // Next tetrimino to spawn
    private var nextTetrimino: Tetrimino? = null
    
    // Game state
    private val _gameState = MutableStateFlow<GameState>(GameState.NotStarted)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    // Score
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()
    
    // Ghost block (preview of where the tetrimino will land)
    private val _ghostPosition = MutableStateFlow<Position?>(null)
    val ghostPosition: StateFlow<Position?> = _ghostPosition.asStateFlow()
    
    /**
     * Start a new game
     */
    fun startGame() {
        // Reset the grid
        grid.reset()
        
        // Reset score
        _score.value = 0
        
        // Spawn the first tetrimino
        spawnTetrimino()
        
        // Set game state to running
        _gameState.value = GameState.Running
    }
    
    /**
     * Spawn a new tetrimino at the top of the grid
     */
    private fun spawnTetrimino() {
        // If we have a next tetrimino, use it
        currentTetrimino = nextTetrimino ?: generateRandomTetrimino()
        
        // Generate the next tetrimino
        nextTetrimino = generateRandomTetrimino()
        
        // Check if game is over (can't place the new tetrimino)
        if (currentTetrimino != null && !grid.canPlace(currentTetrimino!!)) {
            // Game over - don't update ghost position
            // This allows the piece to remain partially off-screen
            _gameState.value = GameState.GameOver
        } else {
            // Only update ghost position if the game is still running
            updateGhostPosition()
        }
    }
    
    /**
     * Generate a random tetrimino
     */
    private fun generateRandomTetrimino(): Tetrimino {
        val types = TetriminoType.values()
        val randomType = types[Random.nextInt(types.size)]
        return Tetrimino(randomType)
    }
    
    /**
     * Update the ghost position (preview of where the tetrimino will land)
     */
    private fun updateGhostPosition() {
        currentTetrimino?.let { tetrimino ->
            // Create a copy of the current tetrimino
            var ghost = tetrimino.copy()
            
            // Move the ghost down until it hits something
            while (isValidMove(ghost.moveDown())) {
                ghost = ghost.moveDown()
            }
            
            // Update the ghost position
            _ghostPosition.value = ghost.position
        } ?: run {
            _ghostPosition.value = null
        }
    }
    
    /**
     * Check if a move is valid (within bounds and not colliding)
     */
    private fun isValidMove(tetrimino: Tetrimino): Boolean {
        return tetrimino.getBlocks().all { position ->
            position.y < grid.height && // Within bottom bound
            position.x >= 0 && position.x < grid.width && // Within horizontal bounds
            (position.y < 0 || grid.isEmpty(position)) // Above top or empty cell
        }
    }
    
    /**
     * Get the blocks of the current tetrimino that are within the grid bounds
     * This is used for rendering the final position at game over
     */
    fun getVisibleBlocks(): List<Position> {
        return currentTetrimino?.getBlocks()?.filter { position ->
            position.y >= 0 && position.y < grid.height &&
            position.x >= 0 && position.x < grid.width
        } ?: emptyList()
    }
    
    /**
     * Move the current tetrimino left
     */
    fun moveLeft() {
        currentTetrimino?.let { tetrimino ->
            val movedTetrimino = tetrimino.moveLeft()
            if (isValidMove(movedTetrimino)) {
                currentTetrimino = movedTetrimino
                updateGhostPosition()
            }
        }
    }
    
    /**
     * Move the current tetrimino right
     */
    fun moveRight() {
        currentTetrimino?.let { tetrimino ->
            val movedTetrimino = tetrimino.moveRight()
            if (isValidMove(movedTetrimino)) {
                currentTetrimino = movedTetrimino
                updateGhostPosition()
            }
        }
    }
    
    /**
     * Move the current tetrimino down (soft drop)
     * Returns true if the move was successful, false if the tetrimino was locked
     */
    fun moveDown(): Boolean {
        currentTetrimino?.let { tetrimino ->
            val movedTetrimino = tetrimino.moveDown()
            if (isValidMove(movedTetrimino)) {
                currentTetrimino = movedTetrimino
                return true
            } else {
                // Lock the tetrimino in place
                lockTetrimino()
                return false
            }
        }
        return false
    }
    
    /**
     * Rotate the current tetrimino
     */
    fun rotate() {
        currentTetrimino?.let { tetrimino ->
            val rotatedTetrimino = tetrimino.rotate()
            if (isValidMove(rotatedTetrimino)) {
                currentTetrimino = rotatedTetrimino
                updateGhostPosition()
            }
        }
    }
    
    /**
     * Hard drop the current tetrimino (instantly drop to the bottom)
     */
    fun hardDrop() {
        currentTetrimino?.let { tetrimino ->
            // Move down until invalid
            var movedTetrimino = tetrimino
            while (isValidMove(movedTetrimino.moveDown())) {
                movedTetrimino = movedTetrimino.moveDown()
            }
            
            // Update current tetrimino
            currentTetrimino = movedTetrimino
            
            // Lock the tetrimino in place
            lockTetrimino()
        }
    }
    
    /**
     * Lock the current tetrimino in place and spawn a new one
     */
    private fun lockTetrimino() {
        currentTetrimino?.let { tetrimino ->
            // Place the tetrimino on the grid
            grid.place(tetrimino)
            
            // Clear completed lines and update score
            val linesCleared = grid.clearLines()
            updateScore(linesCleared)
            
            // Spawn a new tetrimino
            spawnTetrimino()
        }
    }
    
    /**
     * Update the score based on lines cleared
     */
    private fun updateScore(linesCleared: Int) {
        val points = when (linesCleared) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800 // Tetris!
            else -> 0
        }
        
        _score.value += points
    }
    
    /**
     * Pause the game
     */
    fun pauseGame() {
        if (_gameState.value == GameState.Running) {
            _gameState.value = GameState.Paused
        }
    }
    
    /**
     * Resume the game
     */
    fun resumeGame() {
        if (_gameState.value == GameState.Paused) {
            _gameState.value = GameState.Running
        }
    }
    
    /**
     * Get the current tetrimino
     */
    fun getCurrentTetrimino(): Tetrimino? = currentTetrimino
    
    /**
     * Get the next tetrimino
     */
    fun getNextTetrimino(): Tetrimino? = nextTetrimino
    
    /**
     * Get a copy of the grid for rendering
     */
    fun getGrid(): Grid = grid
}

/**
 * Enum representing the different game states
 */
enum class GameState {
    NotStarted,
    Running,
    Paused,
    GameOver
}
