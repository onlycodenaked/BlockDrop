package com.example.blockdrop.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blockdrop.core.engine.GameEngine
import com.example.blockdrop.core.engine.GameState
import com.example.blockdrop.core.model.Grid
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.Tetrimino
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
    // Game engine
    private val gameEngine = GameEngine()
    
    // Game state
    val gameState: StateFlow<GameState> = gameEngine.gameState
    
    // Score
    val score: StateFlow<Int> = gameEngine.score
    
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
     * Clean up when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
