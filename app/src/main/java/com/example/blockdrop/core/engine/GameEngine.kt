package com.example.blockdrop.core.engine

import com.example.blockdrop.core.model.Grid
import com.example.blockdrop.core.model.Orb
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.PowerUpType
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
    
    // Current multiplier
    private val _multiplier = MutableStateFlow(1)
    val multiplier: StateFlow<Int> = _multiplier.asStateFlow()
    
    // Current level
    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level.asStateFlow()
    
    // Multiplier active time in milliseconds
    private val multiplierDuration = 10000L // 10 seconds
    private var multiplierEndTime = 0L
    
    // Ghost block (preview of where the tetrimino will land)
    private val _ghostPosition = MutableStateFlow<Position?>(null)
    val ghostPosition: StateFlow<Position?> = _ghostPosition.asStateFlow()
    
    /**
     * Start a new game
     */
    fun startGame() {
        // Reset the grid
        grid.reset()
        
        // Reset score, multiplier and level
        _score.value = 0
        _multiplier.value = 1
        _level.value = 1
        multiplierEndTime = 0L
        
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
        
        // Calculate ghost position
        updateGhostPosition()
        
        // Check if game is over (can't place the new tetrimino)
        if (currentTetrimino != null && !grid.canPlace(currentTetrimino!!)) {
            _gameState.value = GameState.GameOver
        }
    }
    
    /**
     * Generate a random tetrimino, possibly with a power-up
     */
    private fun generateRandomTetrimino(): Tetrimino {
        val types = TetriminoType.values()
        val randomType = types[Random.nextInt(types.size)]
        
        // Check if this tetrimino should have a power-up
        val powerUp = if (PowerUpType.shouldGeneratePowerUp(_level.value)) {
            PowerUpType.random()
        } else {
            null
        }
        
        return Tetrimino(randomType, powerUp = powerUp)
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
        // Ghost power-up can overlap with existing blocks
        if (tetrimino.powerUp == PowerUpType.GHOST) {
            return tetrimino.getBlocks().all { position ->
                position.y < grid.height && // Within bottom bound
                position.x >= 0 && position.x < grid.width // Within horizontal bounds
            }
        }
        
        // Normal collision check for regular tetriminos
        return tetrimino.getBlocks().all { position ->
            position.y < grid.height && // Within bottom bound
            position.x >= 0 && position.x < grid.width && // Within horizontal bounds
            (position.y < 0 || grid.isEmpty(position)) // Above top or empty cell
        }
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
            // Check for power-up effects before placing
            when (tetrimino.powerUp) {
                PowerUpType.BOMB -> {
                    // Apply bomb effect (clear blocks in a radius)
                    applyBombEffect(tetrimino)
                }
                PowerUpType.LINE_CLEAR -> {
                    // Apply line clear effect (clear lines where the tetrimino is placed)
                    applyLineClearEffect(tetrimino)
                }
                else -> {
                    // Place the tetrimino on the grid normally
                    grid.place(tetrimino)
                }
            }
            
            // Clear completed lines and update score
            val (linesCleared, orbMultiplier) = grid.clearLines()
            
            // Apply orb multiplier if lines were cleared
            if (linesCleared > 0 && orbMultiplier > 1) {
                applyMultiplier(orbMultiplier)
            }
            
            // Update score with current multiplier
            updateScore(linesCleared)
            
            // Check for level up
            checkLevelUp()
            
            // Spawn a new tetrimino
            spawnTetrimino()
        }
    }
    
    /**
     * Apply bomb power-up effect (clear blocks in a radius)
     */
    private fun applyBombEffect(tetrimino: Tetrimino) {
        // Get the center position of the tetrimino
        val blocks = tetrimino.getBlocks()
        if (blocks.isEmpty()) return
        
        // Calculate center point of the tetrimino
        val centerX = blocks.map { it.x }.average().toInt()
        val centerY = blocks.map { it.y }.average().toInt()
        
        // Define the blast radius (3x3)
        val radius = 1
        
        // Clear blocks within the radius
        for (y in (centerY - radius)..(centerY + radius)) {
            for (x in (centerX - radius)..(centerX + radius)) {
                val position = Position(x, y)
                if (grid.isInBounds(position)) {
                    grid.clearCell(position)
                }
            }
        }
        
        // Award points for using a bomb (500 points)
        _score.value += 500 * _multiplier.value
    }
    
    /**
     * Apply line clear power-up effect (clear lines where the tetrimino is placed)
     */
    private fun applyLineClearEffect(tetrimino: Tetrimino) {
        // Get the rows where the tetrimino is located
        val rows = tetrimino.getBlocks().map { it.y }.distinct()
        
        // Clear each row
        rows.forEach { y ->
            if (y >= 0 && y < grid.height) {
                grid.clearLine(y)
                grid.shiftLinesDown(y)
            }
        }
        
        // Award points for using line clear (200 points per line)
        _score.value += (rows.size * 200) * _multiplier.value
    }
    
    /**
     * Update the score based on the number of lines cleared
     */
    private fun updateScore(linesCleared: Int) {
        // Basic scoring: 100 points per line, with bonus for multiple lines
        val basePoints = when (linesCleared) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800
            else -> 0
        }
        
        // Apply current multiplier to points
        val points = basePoints * _multiplier.value
        
        // Update the score
        _score.value += points
        
        // Check if multiplier has expired
        checkMultiplierExpiration()
    }
    
    /**
     * Check if the player should level up based on score
     */
    private fun checkLevelUp() {
        // Level up every 2000 points
        val newLevel = (_score.value / 2000) + 1
        
        // Update level if it has increased
        if (newLevel > _level.value) {
            _level.value = newLevel
        }
    }
    
    /**
     * Apply a new multiplier from collecting orbs
     */
    private fun applyMultiplier(orbMultiplier: Int) {
        // Apply the new multiplier (stack with existing multiplier)
        _multiplier.value *= orbMultiplier
        
        // Set the expiration time
        multiplierEndTime = System.currentTimeMillis() + multiplierDuration
    }
    
    /**
     * Check if the multiplier has expired and reset it if needed
     */
    private fun checkMultiplierExpiration() {
        if (multiplierEndTime > 0 && System.currentTimeMillis() > multiplierEndTime) {
            // Reset multiplier
            _multiplier.value = 1
            multiplierEndTime = 0L
        }
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
     * Get the current grid (for rendering)
     */
    fun getGrid(): Grid {
        return grid
    }
    
    /**
     * Get all orbs on the grid (for rendering)
     */
    fun getOrbs(): List<Orb> {
        return grid.getOrbs()
    }
    
    /**
     * Get the time remaining for the current multiplier (in milliseconds)
     */
    fun getMultiplierTimeRemaining(): Long {
        if (multiplierEndTime <= 0) return 0
        val remaining = multiplierEndTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }
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
