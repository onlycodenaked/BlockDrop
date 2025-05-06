package com.example.blockdrop.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.key.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import android.graphics.Paint
import com.example.blockdrop.ui.effects.calculateScreenShakeOffset
import com.example.blockdrop.ui.effects.rememberScreenShake
import com.example.blockdrop.ui.effects.rememberGlowPulse
import com.example.blockdrop.ui.effects.calculateGlowPulseIntensity
import com.example.blockdrop.ui.effects.triggerScreenShake
import com.example.blockdrop.ui.effects.triggerGlowPulse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockdrop.core.engine.GameState
import com.example.blockdrop.core.model.Orb
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.PowerUpType
import com.example.blockdrop.core.model.Tetrimino
import com.example.blockdrop.ui.theme.DarkBackground
import com.example.blockdrop.ui.theme.NeonBlue
import com.example.blockdrop.ui.theme.NeonGreen
import com.example.blockdrop.ui.theme.NeonPink
import kotlin.math.abs
import kotlin.math.min

/**
 * Main game screen composable
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    // Collect state from ViewModel
    val gameState by viewModel.gameState.collectAsState()
    val score by viewModel.score.collectAsState()
    val multiplier by viewModel.multiplier.collectAsState()
    val multiplierTimeRemaining by viewModel.multiplierTimeRemaining.collectAsState()
    val level by viewModel.level.collectAsState()
    val grid by viewModel.gridState.collectAsState()
    val currentTetrimino by viewModel.currentTetrimino.collectAsState()
    val nextTetrimino by viewModel.nextTetrimino.collectAsState()
    val ghostPosition by viewModel.ghostPosition.collectAsState()
    val orbs by viewModel.orbs.collectAsState()
    
    // Keep track of the current tetrimino to detect changes
    var lastTetriminoId by remember { mutableStateOf("") }
    
    // Flag to require a new drag gesture for each new piece
    var requireNewDragGesture by remember { mutableStateOf(false) }
    
    // Visual effects
    val screenShake = rememberScreenShake()
    val glowPulse = rememberGlowPulse()
    val shakeOffset = calculateScreenShakeOffset(screenShake)
    val glowIntensity = calculateGlowPulseIntensity(glowPulse)
    
    // Track drag gesture state
    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartY by remember { mutableStateOf(0f) }
    var accumulatedHorizontalDrag by remember { mutableStateOf(0f) }
    var accumulatedVerticalDrag by remember { mutableStateOf(0f) }
    
    // Track which direction this drag gesture is for (once determined)
    var currentDragDirection by remember { mutableStateOf("") } // "horizontal" or "vertical"
    
    // Track if we're currently accelerating drop speed
    var isAcceleratingDrop by remember { mutableStateOf(false) }
    
    // Function to reset all drag state
    fun resetDragState() {
        accumulatedHorizontalDrag = 0f
        accumulatedVerticalDrag = 0f
        currentDragDirection = "" // Reset direction for new gesture
        isAcceleratingDrop = false
    }
    
    // Start the game when the screen is first displayed
    LaunchedEffect(Unit) {
        if (gameState == GameState.NotStarted) {
            viewModel.startGame()
        }
    }
    
    // Reset drag state when a new tetrimino spawns
    LaunchedEffect(currentTetrimino) {
        currentTetrimino?.let { tetrimino ->
            // Create a unique ID for this tetrimino based on its properties
            val tetriminoId = "${tetrimino.type}_${tetrimino.position.x}_${tetrimino.position.y}_${tetrimino.rotation}"
            
            // If this is a new tetrimino (not just the same one moving)
            if (tetriminoId != lastTetriminoId && tetrimino.position.y <= 1) {
                // Reset all drag state for the new piece
                resetDragState()
                // Require the user to lift finger and start a new drag for this piece
                requireNewDragGesture = true
                lastTetriminoId = tetriminoId
                
                // Trigger visual effects for new tetrimino
                if (tetrimino.powerUp != null) {
                    // Trigger glow effect for power-up tetriminos
                    val color = when (tetrimino.powerUp) {
                        PowerUpType.BOMB -> Color(0xFFFF5555) // Red for bomb
                        PowerUpType.LINE_CLEAR -> Color(0xFF55FFFF) // Cyan for line clear
                        PowerUpType.GHOST -> Color(0xFFFFFF55) // Yellow for ghost
                        else -> Color.White
                    }
                    triggerGlowPulse(glowPulse, color, 0.5f)
                }
            } else if (tetriminoId != lastTetriminoId) {
                // Update the ID but don't reset (this is just movement of the same piece)
                lastTetriminoId = tetriminoId
            }
        }
    }
    
    // Handle line clears
    LaunchedEffect(score.value) {
        // When score changes, it might be due to a line clear
        // Trigger screen shake effect for line clears
        triggerScreenShake(screenShake, 5f, 5)
    }
    
    // Main game layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            // Add keyboard listener
            .onKeyEvent { keyEvent ->
                if (gameState == GameState.Running) {
                    when {
                        // Rotate on Up arrow
                        (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) -> {
                            viewModel.rotate()
                            true
                        }
                        // Move left on Left arrow
                        (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft) -> {
                            viewModel.moveLeft()
                            true
                        }
                        // Move right on Right arrow
                        (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight) -> {
                            viewModel.moveRight()
                            true
                        }
                        // Soft drop on Down arrow
                        (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) -> {
                            viewModel.softDrop()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game header with title, score, and next piece preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game title and level
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "NEON BLOCK DROP",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonPink,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "LEVEL $level",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Score and multiplier display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SCORE: $score",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    // Only show multiplier if it's greater than 1
                    if (multiplier > 1) {
                        val multiplierColor = when {
                            multiplier >= 5 -> NeonPink
                            multiplier >= 3 -> NeonGreen
                            else -> NeonBlue
                        }
                        
                        // Calculate remaining time percentage
                        val timePercentage = (multiplierTimeRemaining.toFloat() / 10000f).coerceIn(0f, 1f)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${multiplier}x",
                                style = MaterialTheme.typography.titleMedium,
                                color = multiplierColor,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Multiplier timer bar
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(8.dp)
                                    .background(Color(0xFF333333), shape = MaterialTheme.shapes.small)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(timePercentage)
                                        .background(multiplierColor, shape = MaterialTheme.shapes.small)
                                )
                            }
                        }
                    }
                }
                
                // Next piece preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "NEXT",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    NextTetriminoPreview(
                        tetrimino = nextTetrimino,
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF1A1A1A))
                            .padding(4.dp)
                    )
                }
            }
            
            // Game grid (centered on screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Fill available vertical space
                contentAlignment = Alignment.Center
            ) {
                // Game grid with all the gesture handlers
                GameGrid(
                    grid = grid,
                    currentTetrimino = currentTetrimino,
                    ghostPosition = ghostPosition,
                    orbs = orbs,
                    modifier = Modifier
                        .wrapContentSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Rotate on tap
                                viewModel.rotate()
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    // Record the starting position
                                    dragStartX = offset.x
                                    dragStartY = offset.y
                                    accumulatedHorizontalDrag = 0f
                                    accumulatedVerticalDrag = 0f
                                    currentDragDirection = "" // Reset direction for new gesture
                                    // Allow drag gestures for this new touch
                                    requireNewDragGesture = false
                                },
                                onDragEnd = {
                                    // Stop accelerating drop when drag ends
                                    if (isAcceleratingDrop) {
                                        isAcceleratingDrop = false
                                    }
                                },
                                onDragCancel = {
                                    // Stop accelerating drop when drag is canceled
                                    if (isAcceleratingDrop) {
                                        isAcceleratingDrop = false
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    
                                    // Skip processing if we're requiring a new drag gesture
                                    if (requireNewDragGesture) {
                                        return@detectDragGestures
                                    }
                                    
                                    val horizontalDrag = dragAmount.x
                                    val verticalDrag = dragAmount.y
                                    val horizontalThreshold = 30f // Threshold for horizontal movement
                                    val verticalThreshold = 40f // Threshold for vertical movement
                                    val directionDeterminingThreshold = 15f // Threshold to determine drag direction
                                    
                                    // If direction hasn't been determined yet, determine it based on initial movement
                                    if (currentDragDirection.isEmpty()) {
                                        if (abs(horizontalDrag) > directionDeterminingThreshold && 
                                            abs(horizontalDrag) > abs(verticalDrag)) {
                                            // This is a horizontal drag gesture
                                            currentDragDirection = "horizontal"
                                        } else if (abs(verticalDrag) > directionDeterminingThreshold && 
                                                 abs(verticalDrag) > abs(horizontalDrag)) {
                                            // This is a vertical drag gesture
                                            currentDragDirection = "vertical"
                                        }
                                    }
                                    
                                    // Process drag based on determined direction
                                    when (currentDragDirection) {
                                        "horizontal" -> {
                                            // Accumulate the horizontal drag distance
                                            accumulatedHorizontalDrag += horizontalDrag
                                            
                                            // Move when accumulated drag exceeds threshold
                                            if (accumulatedHorizontalDrag > horizontalThreshold) {
                                                viewModel.moveRight()
                                                // Reset accumulated drag (but keep remainder for smoother movement)
                                                accumulatedHorizontalDrag -= horizontalThreshold
                                            } else if (accumulatedHorizontalDrag < -horizontalThreshold) {
                                                viewModel.moveLeft()
                                                // Reset accumulated drag (but keep remainder for smoother movement)
                                                accumulatedHorizontalDrag += horizontalThreshold
                                            }
                                        }
                                        "vertical" -> {
                                            // Accumulate the vertical drag distance
                                            accumulatedVerticalDrag += verticalDrag
                                            
                                            // Continuous soft drop when dragging down
                                            if (accumulatedVerticalDrag > verticalThreshold) {
                                                viewModel.softDrop()
                                                // Reset accumulated drag (but keep remainder for smoother movement)
                                                accumulatedVerticalDrag -= verticalThreshold
                                            } else if (verticalDrag < -verticalThreshold * 2) {
                                                // Hard drop on fast swipe up
                                                viewModel.hardDrop()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )
            }
            
            // Game state buttons at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (gameState) {
                    GameState.Running -> {
                        Button(
                            onClick = { viewModel.pauseGame() }
                        ) {
                            Text("PAUSE")
                        }
                    }
                    GameState.Paused -> {
                        Button(
                            onClick = { viewModel.resumeGame() }
                        ) {
                            Text("RESUME")
                        }
                    }
                    GameState.GameOver -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "GAME OVER",
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonPink,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Button(
                                onClick = { viewModel.startGame() }
                            ) {
                                Text("NEW GAME")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

/**
 * Composable for rendering the game grid
 */
@Composable
fun GameGrid(
    grid: com.example.blockdrop.core.model.Grid?,
    currentTetrimino: Tetrimino?,
    ghostPosition: Position?,
    modifier: Modifier = Modifier,
    orbs: List<Orb> = emptyList()
) {
    // Calculate cell size based on available width
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Use 85% of the screen width to calculate cell size
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val gridWidth = 10 // 10 columns
    val cellSize = screenWidth * 0.85f / gridWidth // 85% of available width for the grid
    
    // Calculate total grid width and height
    val totalGridWidth = cellSize * gridWidth
    val totalGridHeight = cellSize * 20 // 20 rows
    
    // Create a box to contain the grid
    Box(
        modifier = modifier
            .aspectRatio(0.5f) // 10:20 aspect ratio for the grid
            .background(Color.Transparent)
            .border(width = 1.dp, color = Color(0xFF333333).copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Draw the grid using Canvas
        Canvas(
            modifier = Modifier.size(
                width = with(density) { totalGridWidth.toDp() },
                height = with(density) { totalGridHeight.toDp() }
            )
        ) {
            // Apply screen shake effect
            translate(shakeOffset.x, shakeOffset.y) {
                // Draw grid lines
                drawGridLines(cellSize)
                
                // Draw placed blocks
                grid?.let {
                    for (y in 0 until it.height) {
                        for (x in 0 until it.width) {
                            val color = it.getColorAt(Position(x, y))
                            if (color != null) {
                                drawBlock(x, y, cellSize, color)
                            }
                        }
                    }
                }
                
                // Draw ghost blocks
                ghostPosition?.let { ghostPos ->
                    currentTetrimino?.let { tetrimino ->
                        // Create a ghost tetrimino at the ghost position
                        val ghostTetrimino = tetrimino.copy(position = ghostPos)
                        
                        // Draw each block of the ghost tetrimino
                        ghostTetrimino.getBlocks().forEach { position ->
                            drawGhostBlock(position.x, position.y, cellSize, tetrimino.color)
                        }
                    }
                }
                
                // Draw current tetrimino
                currentTetrimino?.let { tetrimino ->
                    val isPowerUp = tetrimino.powerUp != null
                    tetrimino.getBlocks().forEach { position ->
                        if (position.y >= 0) { // Only draw blocks that are within the visible grid
                            drawBlock(position.x, position.y, cellSize, tetrimino.color, isPowerUp)
                        }
                    }
                }
                
                // Draw orbs (last, so they appear on top of everything)
                orbs.forEach { orb ->
                    drawOrb(orb, cellSize)
                }
                
                // Draw particles
                viewModel.particleSystem.getParticles().forEach { particle ->
                    rotate(particle.rotation) {
                        drawCircle(
                            color = particle.color.copy(alpha = particle.alpha),
                            radius = particle.size,
                            center = particle.position
                        )
                    }
                }
                
                // Draw glow effect overlay if active
                if (glowIntensity > 0) {
                    drawRect(
                        color = glowPulse.value.color.copy(alpha = glowIntensity * 0.2f),
                        size = size
                    )
                }
            }
        }
    }
}

/**
 * Draw grid lines (now empty as we're removing the grid)
 */
private fun DrawScope.drawGridLines(cellSize: Float) {
    // Grid lines removed as requested
}

/**
 * Draw a block at the specified grid position
 */
private fun DrawScope.drawBlock(x: Int, y: Int, cellSize: Float, color: Color, isPowerUp: Boolean = false) {
    // Only draw if within the visible grid
    if (y >= 0) {
        val blockSize = cellSize * 0.85f // Slightly smaller than cell
        val padding = (cellSize - blockSize) / 2
        
        // Draw block background
        drawRect(
            color = color.copy(alpha = 0.2f),
            topLeft = Offset(x * cellSize + padding, y * cellSize + padding),
            size = Size(blockSize, blockSize)
        )
        
        // Draw block border
        drawRect(
            color = color,
            topLeft = Offset(x * cellSize + padding, y * cellSize + padding),
            size = Size(blockSize, blockSize),
            style = Stroke(width = 3f)
        )
        
        // Draw inner glow
        drawRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(x * cellSize + padding + 3, y * cellSize + padding + 3),
            size = Size(blockSize - 6, blockSize - 6),
            style = Stroke(width = 1.5f)
        )
        
        // Draw power-up indicator if this is a power-up block
        if (isPowerUp) {
            // Draw a star symbol for power-up tetriminos
            val centerX = x * cellSize + cellSize / 2
            val centerY = y * cellSize + cellSize / 2
            val radius = cellSize / 4
            
            // Draw a simple star
            val starPath = android.graphics.Path()
            for (i in 0 until 10) {
                val angle = Math.PI * i / 5
                val r = if (i % 2 == 0) radius else radius / 2
                val px = centerX + r * Math.cos(angle).toFloat()
                val py = centerY + r * Math.sin(angle).toFloat()
                
                if (i == 0) {
                    starPath.moveTo(px, py)
                } else {
                    starPath.lineTo(px, py)
                }
            }
            starPath.close()
            
            // Draw the star with a pulsing effect
            val pulseScale = 1f + (Math.sin(System.currentTimeMillis() / 200f) * 0.1f)
            
            // Draw the star
            drawPath(
                path = starPath.asComposePath(),
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1f * pulseScale)
            )
        }
    }
}

/**
 * Draw a ghost block at the specified grid position
 */
private fun DrawScope.drawGhostBlock(x: Int, y: Int, cellSize: Float, color: Color) {
    // Only draw if within the visible grid
    if (y >= 0) {
        val blockSize = cellSize * 0.85f // Slightly smaller than cell
        val padding = (cellSize - blockSize) / 2
        
        // Draw ghost block (just an outline)
        drawRect(
            color = color.copy(alpha = 0.3f),
            topLeft = Offset(x * cellSize + padding, y * cellSize + padding),
            size = Size(blockSize, blockSize),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Draw an orb at the specified grid position
 */
private fun DrawScope.drawOrb(x: Int, y: Int, cellSize: Float, color: Color, multiplier: Int) {
    // Only draw if within the visible grid
    if (y >= 0) {
        val orbSize = cellSize * 0.7f // Smaller than a block
        
        // Calculate center position
        val centerX = x * cellSize + cellSize / 2
        val centerY = y * cellSize + cellSize / 2
        
        // Draw orb background (circle)
        drawCircle(
            color = Color(0xFF222222),
            radius = orbSize / 2,
            center = Offset(centerX, centerY)
        )
        
        // Draw orb glow
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = orbSize / 2 + 4f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
        
        // Draw orb
        drawCircle(
            color = color,
            radius = orbSize / 2,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3f)
        )
        
        // Draw multiplier text (simplified to avoid Canvas issues)
        // Instead of text, we'll draw a simple visual indicator of the multiplier
        when (multiplier) {
            2 -> {
                // Draw a small inner circle for 2x
                drawCircle(
                    color = color,
                    radius = orbSize / 4,
                    center = Offset(centerX, centerY)
                )
            }
            3 -> {
                // Draw a triangle pattern for 3x
                val innerRadius = orbSize / 4
                for (i in 0 until 3) {
                    val angle = Math.PI * 2 * i / 3
                    val innerX = centerX + (innerRadius * Math.cos(angle)).toFloat()
                    val innerY = centerY + (innerRadius * Math.sin(angle)).toFloat()
                    drawCircle(
                        color = color,
                        radius = orbSize / 8,
                        center = Offset(innerX, innerY)
                    )
                }
            }
            5 -> {
                // Draw a star pattern for 5x
                val innerRadius = orbSize / 4
                for (i in 0 until 5) {
                    val angle = Math.PI * 2 * i / 5
                    val innerX = centerX + (innerRadius * Math.cos(angle)).toFloat()
                    val innerY = centerY + (innerRadius * Math.sin(angle)).toFloat()
                    drawCircle(
                        color = color,
                        radius = orbSize / 8,
                        center = Offset(innerX, innerY)
                    )
                }
            }
        }
    }
}

/**
 * Composable for rendering the next tetrimino preview
 */
@Composable
fun NextTetriminoPreview(
    tetrimino: Tetrimino?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                tetrimino?.let {
                    // Calculate cell size based on available space and tetrimino shape
                    val shape = it.shape
                    val rows = shape.size
                    val cols = if (rows > 0) shape[0].size else 0
                    
                    val cellSize = min(
                        size.width / (cols + 2), // Add padding
                        size.height / (rows + 2)  // Add padding
                    )
                    
                    // Calculate offset to center the tetrimino
                    val offsetX = (size.width - cellSize * cols) / 2
                    val offsetY = (size.height - cellSize * rows) / 2
                    
                    // Check if this is a power-up tetrimino
                    val isPowerUp = it.powerUp != null
                    
                    // Draw each block of the tetrimino
                    for (y in shape.indices) {
                        for (x in 0 until shape[y].size) {
                            if (shape[y][x] == 1) {
                                // Calculate position
                                val blockX = offsetX + x * cellSize
                                val blockY = offsetY + y * cellSize
                                
                                // Draw block background
                                drawRect(
                                    color = it.color.copy(alpha = 0.2f),
                                    topLeft = Offset(blockX, blockY),
                                    size = Size(cellSize * 0.8f, cellSize * 0.8f)
                                )
                                
                                // Draw block border
                                drawRect(
                                    color = it.color,
                                    topLeft = Offset(blockX, blockY),
                                    size = Size(cellSize * 0.8f, cellSize * 0.8f),
                                    style = Stroke(width = 2f)
                                )
                                
                                // Draw power-up indicator if applicable
                                if (isPowerUp) {
                                    // Draw a small cross in the center of the block
                                    val centerX = blockX + cellSize * 0.4f
                                    val centerY = blockY + cellSize * 0.4f
                                    val starRadius = cellSize * 0.2f
                                    
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.8f),
                                        start = Offset(centerX - starRadius/2, centerY),
                                        end = Offset(centerX + starRadius/2, centerY),
                                        strokeWidth = 2f
                                    )
                                    
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.8f),
                                        start = Offset(centerX, centerY - starRadius/2),
                                        end = Offset(centerX, centerY + starRadius/2),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Add power-up label below the preview if applicable
        tetrimino?.powerUp?.let { powerUp ->
            Text(
                text = when(powerUp) {
                    PowerUpType.BOMB -> "BOMB"
                    PowerUpType.LINE_CLEAR -> "LINE"
                    PowerUpType.GHOST -> "GHOST"
                },
                color = powerUp.color,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
