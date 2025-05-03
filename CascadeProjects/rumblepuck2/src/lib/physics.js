import planck from 'planck';

// Constants for physics configuration
const SCALE = 30; // 1 meter = 30 pixels
const FIELD_WIDTH = 1600;
const FIELD_HEIGHT = 900;
const PUCK_RADIUS = 15;

// Create the physics world
const createWorld = () => {
  const world = planck.World({
    gravity: planck.Vec2(0, 0) // Zero gravity for top-down game
  });

  // Create field boundaries
  createFieldBoundaries(world);

  return world;
};

// Create the field boundaries (walls)
const createFieldBoundaries = (world) => {
  // Wall thickness (much thicker than before)
  const WALL_THICKNESS = 20 / SCALE;
  
  // Create walls with userData for identification
  const createWall = (x, y, width, height, name) => {
    const wall = world.createBody({
      type: 'static',
      position: planck.Vec2(x / SCALE, y / SCALE),
      userData: { type: 'wall', name }
    });
    
    wall.createFixture({
      shape: planck.Box(width / SCALE / 2, height / SCALE / 2), // Note: Box dimensions are half-widths
      friction: 0.3,
      restitution: 0.9,
      density: 0, // Static bodies don't need density
    });
    
    console.log(`Created ${name} wall at (${x}, ${y}) with dimensions ${width}x${height}`);
    return wall;
  };
  
  // Top wall - positioned outside the visible field to create a barrier
  const topWall = createWall(
    FIELD_WIDTH / 2,           // x center
    -WALL_THICKNESS / 2,       // y just above the field
    FIELD_WIDTH,               // width spans the entire field
    WALL_THICKNESS,            // height is our wall thickness
    'top'
  );
  
  // Bottom wall - positioned outside the visible field
  const bottomWall = createWall(
    FIELD_WIDTH / 2,           // x center
    FIELD_HEIGHT + WALL_THICKNESS / 2, // y just below the field
    FIELD_WIDTH,               // width spans the entire field
    WALL_THICKNESS,            // height is our wall thickness
    'bottom'
  );
  
  // Left wall - positioned outside the visible field
  const leftWall = createWall(
    -WALL_THICKNESS / 2,       // x just left of the field
    FIELD_HEIGHT / 2,          // y center
    WALL_THICKNESS,            // width is our wall thickness
    FIELD_HEIGHT,              // height spans the entire field
    'left'
  );
  
  // Right wall - positioned outside the visible field
  const rightWall = createWall(
    FIELD_WIDTH + WALL_THICKNESS / 2, // x just right of the field
    FIELD_HEIGHT / 2,          // y center
    WALL_THICKNESS,            // width is our wall thickness
    FIELD_HEIGHT,              // height spans the entire field
    'right'
  );
  
  // Add contact listener to debug collisions
  world.on('begin-contact', contact => {
    const bodyA = contact.getFixtureA().getBody();
    const bodyB = contact.getFixtureB().getBody();
    
    const userDataA = bodyA.getUserData();
    const userDataB = bodyB.getUserData();
    
    if (userDataA && userDataA.type === 'wall' || userDataB && userDataB.type === 'wall') {
      const wallName = userDataA && userDataA.type === 'wall' ? userDataA.name : userDataB.name;
      console.log(`Collision detected with ${wallName} wall`);
    }
  });
};

// Create a puck
const createPuck = (world, x, y, className) => {
  const puck = world.createBody({
    type: 'dynamic',
    position: planck.Vec2(x / SCALE, y / SCALE),
    linearDamping: 0.5, // For natural deceleration
    angularDamping: 5.0, // Prevent excessive rotation
    bullet: true, // Enable continuous collision detection for fast-moving objects
    allowSleep: true, // Allow the body to sleep when it comes to rest
    userData: {
      type: 'puck',
      class: className,
      ability: null,
      abilityActivated: false
    }
  });

  // Add circular fixture
  puck.createFixture({
    shape: planck.Circle(PUCK_RADIUS / SCALE),
    density: 0.1,
    friction: 0.1,
    restitution: 0.9
  });

  return puck;
};

// Apply force to shoot a puck
const shootPuck = (puck, angle, power) => {
  // Convert angle to radians
  const radians = angle * (Math.PI / 180);
  
  // Ensure we have a minimum power value
  const effectivePower = Math.max(power, 20); // Minimum power of 20
  
  // Calculate force components with a higher multiplier for better effect
  // and ensure a minimum force even with low power
  const forceX = Math.cos(radians) * effectivePower * 0.05;
  const forceY = Math.sin(radians) * effectivePower * 0.05;
  
  console.log('Applying force:', forceX, forceY, 'with effective power:', effectivePower);
  
  // Apply impulse at center of puck
  puck.applyLinearImpulse(
    planck.Vec2(forceX, forceY),
    puck.getWorldCenter(),
    true
  );
  
  // Return for testing validation
  return { angle, power: effectivePower, force: { x: forceX, y: forceY } };
};

// Predict trajectory for a shot
const predictTrajectory = (world, puck, angle, power, steps = 60) => {
  // Clone the world for prediction
  // Note: Planck.js doesn't have a built-in clone method, so we'll simulate
  // by creating a new world and applying the same force
  
  // Create a new world with the same settings
  const cloneWorld = planck.World({
    gravity: planck.Vec2(0, 0)
  });
  
  // Create field boundaries in the clone world
  createFieldBoundaries(cloneWorld);
  
  // Create a clone puck at the same position
  const position = puck.getPosition();
  const clonePuck = createPuck(
    cloneWorld, 
    position.x * SCALE, 
    position.y * SCALE, 
    puck.getUserData().class
  );
  
  // Apply the same force as the actual shot would
  shootPuck(clonePuck, angle, power);
  
  // Simulate physics for a number of steps
  const positions = [];
  const timeStep = 1/60;
  
  for (let i = 0; i < steps; i++) {
    cloneWorld.step(timeStep);
    
    const pos = clonePuck.getPosition();
    positions.push({ 
      x: pos.x * SCALE, 
      y: pos.y * SCALE 
    });
    
    // Stop if puck is nearly stopped
    const velocity = clonePuck.getLinearVelocity();
    if (velocity.length() < 0.1) break;
  }
  
  return positions;
};

// Convert from screen coordinates to physics world coordinates
const toWorldCoords = (x, y) => ({
  x: x / SCALE,
  y: y / SCALE
});

// Convert from physics world coordinates to screen coordinates
const toScreenCoords = (x, y) => ({
  x: x * SCALE,
  y: y * SCALE
});

export {
  createWorld,
  createPuck,
  shootPuck,
  predictTrajectory,
  toWorldCoords,
  toScreenCoords,
  SCALE,
  FIELD_WIDTH,
  FIELD_HEIGHT,
  PUCK_RADIUS
};
