package com.example.blockdrop.ui.game

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blockdrop.core.audio.SoundEffect
import com.example.blockdrop.core.audio.SoundManager
import com.example.blockdrop.core.engine.GameEngine
import com.example.blockdrop.core.engine.GameState
import com.example.blockdrop.core.model.Grid
import com.example.blockdrop.core.model.Orb
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.PowerUpType
import com.example.blockdrop.core.model.Tetrimino
import com.example.blockdrop.ui.effects.ParticleSystem
import com.example.blockdrop.ui.effects.ScreenShake
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the game screen that manages UI state and user interactions
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // Particle system for visual effects
    val particleSystem = ParticleSystem()
    
    // Screen shake effect
    val screenShake = ScreenShake()
    
    // Sound manager
    private val soundManager = SoundManager(application.applicationContext)
    
    // Last update time for delta time calculation
    private var lastUpdateTime = System.currentTimeMillis()
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
        // Initialize sound manager
        soundManager.initialize()
        soundManager.startMusic()
        
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
        
        // Start the effect update loop
        startEffectsUpdateLoop()
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
     * Start the effects update loop
     */
    private fun startEffectsUpdateLoop() {
        viewModelScope.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastUpdateTime) / 1000f
                lastUpdateTime = currentTime
                
                // Update particle system
                particleSystem.update(deltaTime)
                
                // Update screen shake
                screenShake.update(deltaTime)
                
                delay(16) // ~60fps
            }
        }
    }
    
    /**
     * Update game state
     */
    private fun update() {
        val previousScore = gameEngine.score
        val previousLevel = gameEngine.level
        val previousOrbs = gameEngine.grid.orbs.toList()
        
        val result = gameEngine.update()
        
        // Update state flows with new game state
        _score.value = gameEngine.score
        _multiplier.value = gameEngine.multiplier
        _multiplierTimeRemaining.value = gameEngine.multiplierTimeRemaining / gameEngine.MULTIPLIER_DURATION
        _level.value = gameEngine.level
        _gridState.value = gameEngine.grid.copy()
        _currentTetrimino.value = gameEngine.currentTetrimino?.copy()
        _nextTetrimino.value = gameEngine.nextTetrimino?.copy()
        _ghostPosition.value = gameEngine.calculateGhostPosition()
        _orbs.value = gameEngine.grid.orbs.toList()
        
        // Handle visual and sound effects
        if (result.linesCleared > 0) {
            // Play line clear sound
            soundManager.playSound(SoundEffect.LINE_CLEAR)
            
            // Create line clear particle effects
            val cellSize = 30f // Approximate cell size
            val gridWidth = gameEngine.grid.width * cellSize
            
            for (line in result.clearedLines) {
                val y = line * cellSize + cellSize / 2
                particleSystem.createLineClearEffect(
                    y = y,
                    width = gridWidth,
                    color = when {
                        result.linesCleared >= 4 -> com.example.blockdrop.ui.theme.NeonPink
                        result.linesCleared >= 2 -> com.example.blockdrop.ui.theme.NeonBlue
                        else -> com.example.blockdrop.ui.theme.NeonGreen
                    }
                )
            }
            
            // Add screen shake based on number of lines cleared
            val shakeIntensity = when {
                result.linesCleared >= 4 -> 0.8f
                result.linesCleared >= 2 -> 0.5f
                else -> 0.3f
            }
            screenShake.shake(shakeIntensity, 0.3f)
        }
        
        // Check if any orbs were collected
        val currentOrbs = gameEngine.grid.orbs.toList()
        if (previousOrbs.size > currentOrbs.size) {
            // Find collected orbs
            val collectedOrbs = previousOrbs.filter { orb -> 
                !currentOrbs.any { it.position == orb.position } 
            }
            
            // Create effects for each collected orb
            collectedOrbs.forEach { orb ->
                val cellSize = 30f // Approximate cell size
                val x = orb.position.x * cellSize + cellSize / 2
                val y = orb.position.y * cellSize + cellSize / 2
                
                // Play orb collection sound
                soundManager.playSound(SoundEffect.ORB_COLLECT)
                
                // Create particle effect
                particleSystem.createOrbCollectionEffect(
                    position = Offset(x, y),
                    color = when (orb.type) {
                        Orb.OrbType.SMALL -> com.example.blockdrop.ui.theme.NeonGreen
                        Orb.OrbType.MEDIUM -> com.example.blockdrop.ui.theme.NeonBlue
                        Orb.OrbType.LARGE -> com.example.blockdrop.ui.theme.NeonPink
                    }
                )
            }
        }
        
        // Check if a power-up was used
        if (result.powerUpUsed) {
            // Play power-up sound and create effects
            when (result.powerUpType) {
                PowerUpType.BOMB -> {
                    soundManager.playSound(SoundEffect.BOMB_EXPLODE)
                    
                    // Create explosion effect at the center of the bomb
                    val cellSize = 30f
                    val bombCenter = result.powerUpPosition?.let { pos -> 
                        Offset(
                            (pos.x + 1) * cellSize,
                            (pos.y + 1) * cellSize
                        )
                    } ?: Offset(
                        gameEngine.grid.width * cellSize / 2,
                        gameEngine.grid.height * cellSize / 2
                    )
                    
                    particleSystem.createExplosion(
                        position = bombCenter,
                        color = com.example.blockdrop.ui.theme.NeonPink,
                        particleCount = 50
                    )
                    
                    // Add strong screen shake
                    screenShake.shake(1.0f, 0.5f)
                }
                PowerUpType.LINE_CLEAR -> {
                    soundManager.playSound(SoundEffect.POWER_UP)
                    
                    // Screen shake
                    screenShake.shake(0.6f, 0.4f)
                }
                PowerUpType.GHOST -> {
                    soundManager.playSound(SoundEffect.POWER_UP)
                }
                null -> { /* No power-up */ }
            }
        }
        
        // Check if level increased
        if (gameEngine.level > previousLevel) {
            soundManager.playSound(SoundEffect.LEVEL_UP)
        }
        
        // Update game state
        if (result.gameOver) {
            gameState.value = GameState.GAME_OVER
            soundManager.playSound(SoundEffect.GAME_OVER)
        }
        
        // Update drop speed based on level
        dropSpeed = calculateDropDelay(gameEngine.level)
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        soundManager.release()
    }
    
    /**
     * Sound settings
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundManager.setSoundEnabled(enabled)
    }
    
    fun setMusicEnabled(enabled: Boolean) {
        soundManager.setMusicEnabled(enabled)
    }
    
    fun setSoundVolume(volume: Float) {
        soundManager.setSoundVolume(volume)
    }
    
    fun setMusicVolume(volume: Float) {
        soundManager.setMusicVolume(volume)
    }
}
