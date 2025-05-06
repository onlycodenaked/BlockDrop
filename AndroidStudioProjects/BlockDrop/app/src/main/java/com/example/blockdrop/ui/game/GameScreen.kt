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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockdrop.core.engine.GameState
import com.example.blockdrop.core.model.Position
import com.example.blockdrop.core.model.Tetrimino
import com.example.blockdrop.ui.theme.DarkBackground
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
    val grid by viewModel.gridState.collectAsState()
    val currentTetrimino by viewModel.currentTetrimino.collectAsState()
    val nextTetrimino by viewModel.nextTetrimino.collectAsState()
    val ghostPosition by viewModel.ghostPosition.collectAsState()
    val visibleBlocks by viewModel.visibleBlocks.collectAsState()
    
    // Keep track of the current tetrimino to detect changes
    var lastTetriminoId by remember { mutableStateOf("") }
    
    // Flag to require a new drag gesture for each new piece
    var requireNewDragGesture by remember { mutableStateOf(false) }
    
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
        currentDragDirection = ""
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
            } else if (tetriminoId != lastTetriminoId) {
                // Update the ID but don't reset (this is just movement of the same piece)
                lastTetriminoId = tetriminoId
            }
        }
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
                // Game title
                Text(
                    text = "NEON BLOCK DROP",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Score display
                Text(
                    text = "SCORE: $score",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
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
                    visibleBlocks = visibleBlocks,
                    gameState = gameState,
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
    visibleBlocks: List<Position> = emptyList(),
    gameState: GameState = GameState.NotStarted,
    modifier: Modifier = Modifier
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
            
            // Draw ghost tetrimino (only in running state)
            if (gameState == GameState.Running) {
                currentTetrimino?.let { tetrimino ->
                    ghostPosition?.let { ghostPos ->
                        // Create a copy of the current tetrimino with the ghost position
                        val ghostTetrimino = tetrimino.copy(position = ghostPos)
                        
                        // Draw the ghost blocks
                        ghostTetrimino.getBlocks().forEach { pos ->
                            drawGhostBlock(pos.x, pos.y, cellSize, tetrimino.color)
                        }
                    }
                }
            }
            
            // Draw current tetrimino (in running or paused state)
            if (gameState == GameState.Running || gameState == GameState.Paused) {
                currentTetrimino?.let { tetrimino ->
                    tetrimino.getBlocks().forEach { pos ->
                        drawBlock(pos.x, pos.y, cellSize, tetrimino.color)
                    }
                }
            }
            
            // Draw visible blocks in game over state
            if (gameState == GameState.GameOver && visibleBlocks.isNotEmpty()) {
                // Get the color from the current tetrimino (should be the same for all blocks)
                val color = currentTetrimino?.color ?: Color.White
                
                // Draw only the visible blocks
                visibleBlocks.forEach { pos ->
                    if (pos.y >= 0) { // Only draw blocks that are within the visible grid
                        drawBlock(pos.x, pos.y, cellSize, color)
                    }
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
private fun DrawScope.drawBlock(x: Int, y: Int, cellSize: Float, color: Color) {
    // Only draw blocks that are within the visible grid
    if (y >= 0) {
        val blockSize = cellSize - 2 // Smaller than cell for spacing
        val xPos = x * cellSize + 1
        val yPos = y * cellSize + 1
        
        // Draw glow effect
        drawRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(xPos - 2, yPos - 2),
            size = Size(blockSize + 4, blockSize + 4),
            style = Stroke(width = 4f)
        )
        
        // Draw solid block
        drawRect(
            color = color,
            topLeft = Offset(xPos, yPos),
            size = Size(blockSize, blockSize)
        )
        
        // Draw highlight
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(xPos, yPos),
            size = Size(blockSize, blockSize / 4)
        )
    }
}

/**
 * Draw a ghost block at the specified grid position
 */
private fun DrawScope.drawGhostBlock(x: Int, y: Int, cellSize: Float, color: Color) {
    // Only draw blocks that are within the visible grid
    if (y >= 0) {
        val blockSize = cellSize - 2 // Smaller than cell for spacing
        val xPos = x * cellSize + 1
        val yPos = y * cellSize + 1
        
        // Draw ghost block (outline only)
        drawRect(
            color = color.copy(alpha = 0.3f),
            topLeft = Offset(xPos, yPos),
            size = Size(blockSize, blockSize),
            style = Stroke(width = 2f)
        )
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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        tetrimino?.let { next ->
            val shape = next.shape
            
            // Calculate cell size based on available space
            val previewSize = min(120f, 120f) // Use the smaller dimension
            val maxDimension = shape.size.coerceAtLeast(shape.firstOrNull()?.size ?: 0)
            val cellSize = previewSize / (maxDimension + 2) // Add padding
            
            Canvas(
                modifier = Modifier.size(previewSize.dp)
            ) {
                // Center the tetrimino in the preview
                val offsetX = (size.width - shape[0].size * cellSize) / 2
                val offsetY = (size.height - shape.size * cellSize) / 2
                
                // Draw the tetrimino
                for (y in shape.indices) {
                    for (x in shape[y].indices) {
                        if (shape[y][x] == 1) {
                            val xPos = offsetX + x * cellSize
                            val yPos = offsetY + y * cellSize
                            
                            // Draw glow effect
                            drawRect(
                                color = next.color.copy(alpha = 0.5f),
                                topLeft = Offset(xPos - 2, yPos - 2),
                                size = Size(cellSize - 2 + 4, cellSize - 2 + 4),
                                style = Stroke(width = 4f)
                            )
                            
                            // Draw solid block
                            drawRect(
                                color = next.color,
                                topLeft = Offset(xPos, yPos),
                                size = Size(cellSize - 2, cellSize - 2)
                            )
                            
                            // Draw highlight
                            drawRect(
                                color = Color.White.copy(alpha = 0.3f),
                                topLeft = Offset(xPos, yPos),
                                size = Size(cellSize - 2, (cellSize - 2) / 4)
                            )
                        }
                    }
                }
            }
        }
    }
}
