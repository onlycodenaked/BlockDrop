package com.example.blockdrop.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.example.blockdrop.core.engine.GameEngine
import com.example.blockdrop.core.engine.GameState
import com.example.blockdrop.core.model.Grid
import com.example.blockdrop.core.model.Orb
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.PowerUpType
import com.example.blockdrop.core.model.Tetrimino
import com.example.blockdrop.ui.effects.ParticleSystem
import com.example.blockdrop.ui.effects.GlowPulse
import com.example.blockdrop.ui.effects.ScreenShake
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for the game screen that manages UI state and user interactions
 */
class GameViewModel : ViewModel() {
    // Visual effects systems
    val particleSystem = ParticleSystem()
    // Game engine
    private val gameEngine = GameEngine()
    
    // Game state
    val gameState: StateFlow<GameState> = gameEngine.gameState
    
    // Score
    val score: StateFlow<Int> = gameEngine.score
    
    // Multiplier
    val multiplier: StateFlow<Int> = gameEngine.multiplier
    
    // Level
    val level: StateFlow<Int> = gameEngine.level
    
    // Ghost position
    val ghostPosition: StateFlow<Position?> = gameEngine.ghostPosition
    
    // Grid state (for rendering)
    private val _gridState = MutableStateFlow<Grid?>(null)
    val gridState: StateFlow<Grid?> = _gridState.asStateFlow()
    
    // Current tetrimino (for rendering)
    private val _currentTetrimino = MutableStateFlow<Tetrimino?>(null)
    val currentTetrimino: StateFlow<Tetrimino?> = _currentTetrimino.asStateFlow()
    
    // Next tetrimino (for preview)
    private val _nextTetrimino = MutableStateFlow<Tetrimino?>(null)
    val nextTetrimino: StateFlow<Tetrimino?> = _nextTetrimino.asStateFlow()
    
    // Orbs on the grid
    private val _orbs = MutableStateFlow<List<Orb>>(emptyList())
    val orbs: StateFlow<List<Orb>> = _orbs.asStateFlow()
    
    // Multiplier time remaining (in milliseconds)
    private val _multiplierTimeRemaining = MutableStateFlow(0L)
    val multiplierTimeRemaining: StateFlow<Long> = _multiplierTimeRemaining.asStateFlow()
    
    // Game loop job
    private var gameLoopJob: Job? = null
    
    // Drop speed in milliseconds (adjust for difficulty)
    private var dropSpeed = 800L
    
    init {
        // Update the UI state whenever the game state changes
        viewModelScope.launch {
            gameEngine.gameState.collect { state ->
                updateUIState()
                
                // Start or stop the game loop based on the game state
                when (state) {
                    GameState.Running -> startGameLoop()
                    else -> stopGameLoop()
                }
            }
        }
        
        // Adjust drop speed when level changes
        viewModelScope.launch {
            gameEngine.level.collect { level ->
                adjustDropSpeedForLevel()
            }
        }
    }
    
    /**
     * Start a new game
     */
    fun startGame() {
        gameEngine.startGame()
    }
    
    /**
     * Pause the game
     */
    fun pauseGame() {
        gameEngine.pauseGame()
    }
    
    /**
     * Resume the game
     */
    fun resumeGame() {
        gameEngine.resumeGame()
    }
    
    /**
     * Move the current tetrimino left
     */
    fun moveLeft() {
        if (gameState.value == GameState.Running) {
            gameEngine.moveLeft()
            updateUIState()
        }
    }
    
    /**
     * Move the current tetrimino right
     */
    fun moveRight() {
        if (gameState.value == GameState.Running) {
            gameEngine.moveRight()
            updateUIState()
        }
    }
    
    /**
     * Soft drop (move down faster)
     */
    fun softDrop() {
        if (gameState.value == GameState.Running) {
            gameEngine.moveDown()
            updateUIState()
        }
    }
    
    /**
     * Hard drop (instantly drop to the bottom)
     */
    fun hardDrop() {
        if (gameState.value == GameState.Running) {
            gameEngine.hardDrop()
            updateUIState()
        }
    }
    
    /**
     * Rotate the current tetrimino
     */
    fun rotate() {
        if (gameState.value == GameState.Running) {
            gameEngine.rotate()
            updateUIState()
        }
    }
    
    /**
     * Start the game loop that automatically moves the tetrimino down
     */
    private fun startGameLoop() {
        // Cancel any existing game loop
        gameLoopJob?.cancel()
        
        // Start a new game loop
        gameLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(dropSpeed)
                
                if (gameState.value == GameState.Running) {
                    gameEngine.moveDown()
                    updateUIState()
                }
            }
        }
    }
    
    /**
     * Stop the game loop
     */
    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }
    
    /**
     * Update the UI state from the game engine
     */
    private fun updateUIState() {
        _gridState.value = gameEngine.getGrid()
        _currentTetrimino.value = gameEngine.getCurrentTetrimino()
        _nextTetrimino.value = gameEngine.getNextTetrimino()
        _orbs.value = gameEngine.getOrbs()
        _multiplierTimeRemaining.value = gameEngine.getMultiplierTimeRemaining()
    }
    
    /**
     * Set the drop speed (for difficulty adjustment)
     */
    fun setDropSpeed(speed: Long) {
        dropSpeed = speed
        // Restart the game loop if the game is running
        if (gameState.value == GameState.Running) {
            startGameLoop()
        }
    }
    
    /**
     * Adjust drop speed based on level
     */
    private fun adjustDropSpeedForLevel() {
        // Base speed is 800ms, decreases by 50ms per level (minimum 200ms)
        val newSpeed = (800 - (level.value - 1) * 50).coerceAtLeast(200).toLong()
        if (newSpeed != dropSpeed) {
            setDropSpeed(newSpeed)
        }
    }
    
    /**
     * Clean up when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
