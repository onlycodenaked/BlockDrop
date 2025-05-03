import React, { useRef, useEffect, useState } from 'react';
import usePhysics from '../hooks/usePhysics';
import useGameLoop from '../hooks/useGameLoop';
import useKeyboard from '../hooks/useKeyboard';
import useMouse from '../hooks/useMouse';
import { createPuck, shootPuck, predictTrajectory, FIELD_WIDTH, FIELD_HEIGHT, PUCK_RADIUS, toScreenCoords } from '../lib/physics';
import { drawDebugInfo, createDebugControls, getDebugMode } from '../lib/debug';
import { PUCK_CLASSES, CLASS_COLORS } from '../lib/constants';

// Define all possible spacebar key values for cross-browser compatibility
const SPACEBAR_KEYS = [' ', 'Space', 'Spacebar', 'U+0020'];

const PhysicsSandbox = () => {
  // References
  const canvasRef = useRef(null);
  const debugControlsRef = useRef(null);
  
  // Physics state
  const { world, running, startSimulation, stopSimulation, resetWorld } = usePhysics({
    autoStart: true,
    timeStep: 1/60,
    velocityIterations: 8,
    positionIterations: 3
  });
  
  // Game state
  const [activePuck, setActivePuck] = useState(null);
  const [shootingPower, setShootingPower] = useState(0);
  const [isCharging, setIsCharging] = useState(false);
  const [trajectoryPoints, setTrajectoryPoints] = useState([]);
  const [physicsParams, setPhysicsParams] = useState({
    friction: 0.1,
    restitution: 0.9,
    density: 0.1,
    linearDamping: 0.5
  });
  
  // Input hooks
  const { keys, isKeyDown } = useKeyboard({
    preventDefault: true,
    targetKeys: SPACEBAR_KEYS // Include all possible spacebar key values
  });
  
  const { position: mousePosition, getAngleFromCenter } = useMouse({
    element: canvasRef.current
  });
  
  // Create a new puck at the shooting position
  const createNewPuck = () => {
    console.log('createNewPuck called, current activePuck:', activePuck);
    
    if (activePuck) {
      console.log('Already have an active puck, not creating a new one');
      return;
    }
    
    console.log('Creating new puck at position:', 100, FIELD_HEIGHT / 2);
    
    // Create puck at the shooting position (left side of field)
    const puck = createPuck(
      world,
      100, // x position
      FIELD_HEIGHT / 2, // y position (center of field)
      PUCK_CLASSES.BRUISER // Default class
    );
    
    console.log('New puck created:', puck);
    setActivePuck(puck);
  };
  
  // Handle shooting the active puck
  const handleShoot = () => {
    console.log('handleShoot called, activePuck:', activePuck);
    
    if (!activePuck) {
      console.log('No active puck to shoot');
      return;
    }
    
    // Calculate angle from puck to mouse
    const puckPos = activePuck.getPosition();
    const screenPos = toScreenCoords(puckPos.x, puckPos.y);
    const angle = getAngleFromCenter(screenPos.x, screenPos.y);
    
    console.log('Shooting puck at angle:', angle, 'with power:', shootingPower);
    console.log('Puck position:', puckPos, 'Screen position:', screenPos);
    
    // Shoot the puck with current power
    const shotResult = shootPuck(activePuck, angle, shootingPower);
    console.log('Shot result:', shotResult);
    
    // Reset state
    setActivePuck(null);
    setShootingPower(0);
    setIsCharging(false);
    setTrajectoryPoints([]);
    
    // Create a new puck after a delay
    console.log('Scheduling new puck creation in 1 second');
    setTimeout(createNewPuck, 1000);
  };
  
  // Update trajectory prediction when aiming
  const updateTrajectory = () => {
    if (!activePuck || !isCharging) return;
    
    // Calculate angle from puck to mouse
    const puckPos = activePuck.getPosition();
    const screenPos = toScreenCoords(puckPos.x, puckPos.y);
    const angle = getAngleFromCenter(screenPos.x, screenPos.y);
    
    // Predict trajectory
    const points = predictTrajectory(world, activePuck, angle, shootingPower);
    setTrajectoryPoints(points);
  };
  
  // Handle physics parameter changes
  const handleParamChange = (param, value) => {
    setPhysicsParams(prev => ({
      ...prev,
      [param]: value
    }));
    
    // Apply changes to existing pucks
    for (let body = world.getBodyList(); body; body = body.getNext()) {
      const userData = body.getUserData();
      if (userData && userData.type === 'puck') {
        // Update fixture properties
        const fixture = body.getFixtureList();
        if (fixture) {
          fixture.setFriction(physicsParams.friction);
          fixture.setRestitution(physicsParams.restitution);
          fixture.setDensity(physicsParams.density);
        }
        
        // Update body properties
        body.setLinearDamping(physicsParams.linearDamping);
      }
    }
  };
  
  // Helper function to check if any spacebar key is pressed
  const isSpacebarPressed = () => {
    return SPACEBAR_KEYS.some(key => isKeyDown(key));
  };
  
  // Game loop update function
  const update = (deltaTime) => {
    // Log current key state and deltaTime
    console.log('Current keys state:', keys, 'deltaTime:', deltaTime);
    
    // Handle spacebar for power charging
    if (isSpacebarPressed()) {
      console.log('Space key is down, isCharging:', isCharging, 'Current power:', shootingPower);
      
      if (!isCharging) {
        console.log('Starting charging sequence');
        setIsCharging(true);
      }
      
      // Increase power while space is held (max 100)
      // Using a larger multiplier to ensure power increases even with small deltaTime values
      setShootingPower(prev => {
        // Ensure we have at least some power increase even with very small deltaTime
        const powerIncrease = Math.max(deltaTime * 200, 1);
        const newPower = Math.min(prev + powerIncrease, 100);
        console.log('Updating power from', prev, 'to', newPower, 'increase:', powerIncrease);
        return newPower;
      });
      
      // Update trajectory prediction
      updateTrajectory();
    } else if (isCharging) {
      console.log('Space released, shooting puck with power:', shootingPower);
      // Space released, shoot the puck
      handleShoot();
    }
  };
  
  // Render function
  const render = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    
    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Draw field background
    ctx.fillStyle = '#f0f0f0';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw zone boundaries
    ctx.strokeStyle = '#cccccc';
    ctx.lineWidth = 2;
    
    // Close zone (0-600)
    ctx.fillStyle = 'rgba(255, 200, 200, 0.2)';
    ctx.fillRect(0, 0, 600, FIELD_HEIGHT);
    ctx.strokeRect(0, 0, 600, FIELD_HEIGHT);
    
    // Mid zone (600-1000)
    ctx.fillStyle = 'rgba(200, 200, 255, 0.2)';
    ctx.fillRect(600, 0, 400, FIELD_HEIGHT);
    ctx.strokeRect(600, 0, 400, FIELD_HEIGHT);
    
    // Far zone (1000-1300)
    ctx.fillStyle = 'rgba(200, 255, 200, 0.2)';
    ctx.fillRect(1000, 0, 300, FIELD_HEIGHT);
    ctx.strokeRect(1000, 0, 300, FIELD_HEIGHT);
    
    // Hazard zone (1300-1600)
    ctx.fillStyle = 'rgba(255, 100, 100, 0.2)';
    ctx.fillRect(1300, 0, 300, FIELD_HEIGHT);
    ctx.strokeRect(1300, 0, 300, FIELD_HEIGHT);
    
    // Draw trajectory prediction
    if (trajectoryPoints.length > 0) {
      ctx.strokeStyle = 'rgba(0, 0, 255, 0.5)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(trajectoryPoints[0].x, trajectoryPoints[0].y);
      
      for (let i = 1; i < trajectoryPoints.length; i++) {
        ctx.lineTo(trajectoryPoints[i].x, trajectoryPoints[i].y);
      }
      
      ctx.stroke();
      
      // Draw points along trajectory
      ctx.fillStyle = 'rgba(0, 0, 255, 0.3)';
      trajectoryPoints.forEach((point, index) => {
        const alpha = 1 - (index / trajectoryPoints.length);
        ctx.beginPath();
        ctx.arc(point.x, point.y, 3, 0, Math.PI * 2);
        ctx.fill();
      });
    }
    
    // Draw all bodies in the physics world
    for (let body = world.getBodyList(); body; body = body.getNext()) {
      const position = body.getPosition();
      const angle = body.getAngle();
      const userData = body.getUserData();
      
      // Skip static bodies (walls) in normal rendering
      if (body.isStatic() && !getDebugMode()) continue;
      
      // Draw the body based on its shape
      for (let fixture = body.getFixtureList(); fixture; fixture = fixture.getNext()) {
        const shape = fixture.getShape();
        const type = shape.getType();
        
        ctx.save();
        ctx.translate(position.x * 30, position.y * 30); // SCALE = 30
        ctx.rotate(angle);
        
        if (type === 'circle') {
          const radius = shape.getRadius() * 30; // SCALE = 30
          
          // Fill based on puck class
          if (userData && userData.type === 'puck') {
            ctx.fillStyle = CLASS_COLORS[userData.class] || '#999999';
          } else {
            ctx.fillStyle = '#999999';
          }
          
          // Highlight active puck
          if (body === activePuck) {
            ctx.strokeStyle = '#ffff00';
            ctx.lineWidth = 3;
          } else {
            ctx.strokeStyle = '#000000';
            ctx.lineWidth = 1;
          }
          
          ctx.beginPath();
          ctx.arc(0, 0, radius, 0, Math.PI * 2);
          ctx.fill();
          ctx.stroke();
          
          // Draw a line to show rotation
          ctx.beginPath();
          ctx.moveTo(0, 0);
          ctx.lineTo(radius, 0);
          ctx.stroke();
        } else if (type === 'polygon' || type === 'edge') {
          // Handle other shapes if needed
          ctx.strokeStyle = '#000000';
          ctx.lineWidth = 1;
          
          // Simple rendering for debug purposes
          if (getDebugMode()) {
            ctx.strokeRect(-10, -10, 20, 20);
          }
        }
        
        ctx.restore();
      }
    }
    
    // Draw power bar if charging
    if (isCharging && activePuck) {
      const barWidth = 200;
      const barHeight = 20;
      const barX = (FIELD_WIDTH - barWidth) / 2;
      const barY = FIELD_HEIGHT - 40;
      
      // Background
      ctx.fillStyle = '#333333';
      ctx.fillRect(barX, barY, barWidth, barHeight);
      
      // Power level
      ctx.fillStyle = `rgb(${255 * (shootingPower / 100)}, ${255 * (1 - shootingPower / 100)}, 0)`;
      ctx.fillRect(barX, barY, barWidth * (shootingPower / 100), barHeight);
      
      // Border
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 2;
      ctx.strokeRect(barX, barY, barWidth, barHeight);
    }
    
    // Draw debug information if enabled
    if (getDebugMode()) {
      drawDebugInfo(ctx, world, {
        showVelocity: true,
        showContacts: true,
        showBodies: true,
        showJoints: false,
        showAABBs: false,
        showCenterOfMass: true
      });
    }
    
    // Always show boundaries for debugging wall collisions
    if (showBoundaries) {
      // Highlight all static bodies (walls) with a bright color
      for (let body = world.getBodyList(); body; body = body.getNext()) {
        if (body.isStatic()) {
          const position = body.getPosition();
          const angle = body.getAngle();
          
          ctx.save();
          ctx.translate(position.x * SCALE, position.y * SCALE);
          ctx.rotate(angle);
          
          // Draw the body based on its shape
          for (let fixture = body.getFixtureList(); fixture; fixture = fixture.getNext()) {
            const shape = fixture.getShape();
            const type = shape.getType();
            
            ctx.fillStyle = 'rgba(255, 0, 255, 0.5)';
            ctx.strokeStyle = 'rgb(255, 0, 255)';
            ctx.lineWidth = 2;
            
            if (type === 'polygon') {
              const vertices = shape.getVertices();
              
              ctx.beginPath();
              ctx.moveTo(vertices[0].x * SCALE, vertices[0].y * SCALE);
              
              for (let i = 1; i < vertices.length; i++) {
                ctx.lineTo(vertices[i].x * SCALE, vertices[i].y * SCALE);
              }
              
              ctx.closePath();
              ctx.fill();
              ctx.stroke();
            } else if (type === 'edge') {
              const v1 = shape.getVertex1();
              const v2 = shape.getVertex2();
              
              ctx.beginPath();
              ctx.moveTo(v1.x * SCALE, v1.y * SCALE);
              ctx.lineTo(v2.x * SCALE, v2.y * SCALE);
              ctx.stroke();
            } else if (type === 'circle') {
              const radius = shape.getRadius() * SCALE;
              
              ctx.beginPath();
              ctx.arc(0, 0, radius, 0, Math.PI * 2);
              ctx.fill();
              ctx.stroke();
            }
          }
          
          ctx.restore();
        }
      }
    }
    
    // Draw mouse position and angle if there's an active puck
    if (activePuck) {
      const puckPos = activePuck.getPosition();
      const screenPos = toScreenCoords(puckPos.x, puckPos.y);
      const angle = getAngleFromCenter(screenPos.x, screenPos.y);
      
      // Draw line from puck to mouse
      ctx.strokeStyle = '#ff0000';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(screenPos.x, screenPos.y);
      ctx.lineTo(mousePosition.x, mousePosition.y);
      ctx.stroke();
      
      // Display angle
      ctx.fillStyle = '#000000';
      ctx.font = '14px Arial';
      ctx.fillText(`Angle: ${angle.toFixed(1)}°`, 10, 20);
      ctx.fillText(`Power: ${shootingPower.toFixed(1)}%`, 10, 40);
    }
  };
  
  // Debug flag for showing physics boundaries
  const [showBoundaries, setShowBoundaries] = useState(true);
  
  // Set up game loop
  const { isRunning, start, stop, toggle, fps } = useGameLoop(update, render);
  
  // Create initial puck
  useEffect(() => {
    createNewPuck();
    
    // Create debug controls
    if (debugControlsRef.current) {
      createDebugControls(debugControlsRef.current, physicsParams, handleParamChange);
    }
  }, []);
  
  // Handle canvas resize
  useEffect(() => {
    const handleResize = () => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      
      // Set canvas size to match field dimensions
      canvas.width = FIELD_WIDTH;
      canvas.height = FIELD_HEIGHT;
    };
    
    handleResize();
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);
  
  return (
    <div className="physics-sandbox">
      <div className="sandbox-controls">
        <h2>Physics Sandbox</h2>
        <div className="control-buttons">
          <button onClick={toggle}>
            {isRunning ? 'Pause Simulation' : 'Start Simulation'}
          </button>
          <button onClick={resetWorld}>Reset World</button>
          <button onClick={createNewPuck}>New Puck</button>
          <button onClick={() => setShowBoundaries(!showBoundaries)}>
            {showBoundaries ? 'Hide Boundaries' : 'Show Boundaries'}
          </button>
        </div>
        <div className="fps-counter">FPS: {fps}</div>
        <div className="instructions">
          <p>Hold SPACE to charge power, release to shoot</p>
          <p>Move mouse to aim</p>
        </div>
        <div ref={debugControlsRef} className="debug-controls"></div>
      </div>
      <div className="canvas-container">
        <canvas 
          ref={canvasRef} 
          width={FIELD_WIDTH} 
          height={FIELD_HEIGHT}
        />
      </div>
    </div>
  );
};

export default PhysicsSandbox;
