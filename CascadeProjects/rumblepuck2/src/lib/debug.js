import { SCALE } from './physics';

// Debug mode flag - set to false in production
let debugMode = true;

// Toggle debug mode
const toggleDebugMode = () => {
  debugMode = !debugMode;
  return debugMode;
};

// Get current debug mode
const getDebugMode = () => debugMode;

// Draw debug information on canvas
const drawDebugInfo = (ctx, world, debugOptions = {}) => {
  if (!debugMode) return;

  const {
    showVelocity = true,
    showContacts = true,
    showBodies = true,
    showJoints = true,
    showAABBs = false,
    showCenterOfMass = true
  } = debugOptions;

  // Draw all bodies
  if (showBodies) {
    for (let body = world.getBodyList(); body; body = body.getNext()) {
      const position = body.getPosition();
      const angle = body.getAngle();
      
      ctx.save();
      ctx.translate(position.x * SCALE, position.y * SCALE);
      ctx.rotate(angle);
      
      // Draw fixtures
      for (let fixture = body.getFixtureList(); fixture; fixture = fixture.getNext()) {
        const shape = fixture.getShape();
        const type = shape.getType();
        
        ctx.strokeStyle = body.isAwake() ? '#00ff00' : '#999999';
        ctx.lineWidth = 1;
        
        if (type === 'circle') {
          const radius = shape.getRadius() * SCALE;
          ctx.beginPath();
          ctx.arc(0, 0, radius, 0, 2 * Math.PI);
          ctx.stroke();
          
          // Draw a line from center to edge to show rotation
          ctx.beginPath();
          ctx.moveTo(0, 0);
          ctx.lineTo(radius, 0);
          ctx.stroke();
        } else if (type === 'polygon') {
          const vertices = shape.getVertices();
          ctx.beginPath();
          ctx.moveTo(vertices[0].x * SCALE, vertices[0].y * SCALE);
          
          for (let i = 1; i < vertices.length; i++) {
            ctx.lineTo(vertices[i].x * SCALE, vertices[i].y * SCALE);
          }
          
          ctx.closePath();
          ctx.stroke();
        } else if (type === 'edge') {
          const v1 = shape.getVertex1();
          const v2 = shape.getVertex2();
          
          ctx.beginPath();
          ctx.moveTo(v1.x * SCALE, v1.y * SCALE);
          ctx.lineTo(v2.x * SCALE, v2.y * SCALE);
          ctx.stroke();
        }
      }
      
      // Draw center of mass
      if (showCenterOfMass) {
        const centerOfMass = body.getWorldCenter();
        ctx.fillStyle = '#ff0000';
        ctx.beginPath();
        ctx.arc(
          (centerOfMass.x - position.x) * SCALE, 
          (centerOfMass.y - position.y) * SCALE, 
          2, 0, 2 * Math.PI
        );
        ctx.fill();
      }
      
      ctx.restore();
      
      // Draw velocity
      if (showVelocity && body.isDynamic()) {
        const velocity = body.getLinearVelocity();
        const pos = body.getPosition();
        
        ctx.strokeStyle = '#ff0000';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(pos.x * SCALE, pos.y * SCALE);
        ctx.lineTo(
          (pos.x + velocity.x * 0.2) * SCALE, 
          (pos.y + velocity.y * 0.2) * SCALE
        );
        ctx.stroke();
      }
      
      // Draw AABB
      if (showAABBs) {
        for (let fixture = body.getFixtureList(); fixture; fixture = fixture.getNext()) {
          const aabb = fixture.getAABB(0);
          
          ctx.strokeStyle = '#ff00ff';
          ctx.lineWidth = 1;
          ctx.strokeRect(
            aabb.lowerBound.x * SCALE, 
            aabb.lowerBound.y * SCALE,
            (aabb.upperBound.x - aabb.lowerBound.x) * SCALE,
            (aabb.upperBound.y - aabb.lowerBound.y) * SCALE
          );
        }
      }
    }
  }
  
  // Draw contacts
  if (showContacts) {
    ctx.fillStyle = '#ff0000';
    
    for (let contact = world.getContactList(); contact; contact = contact.getNext()) {
      if (!contact.isTouching()) continue;
      
      const worldManifold = contact.getWorldManifold();
      const points = worldManifold.points;
      
      for (let i = 0; i < points.length; i++) {
        ctx.beginPath();
        ctx.arc(points[i].x * SCALE, points[i].y * SCALE, 3, 0, 2 * Math.PI);
        ctx.fill();
      }
    }
  }
  
  // Draw joints
  if (showJoints) {
    ctx.strokeStyle = '#0000ff';
    ctx.lineWidth = 1;
    
    for (let joint = world.getJointList(); joint; joint = joint.getNext()) {
      const bodyA = joint.getBodyA();
      const bodyB = joint.getBodyB();
      
      const anchorA = joint.getAnchorA();
      const anchorB = joint.getAnchorB();
      
      ctx.beginPath();
      ctx.moveTo(anchorA.x * SCALE, anchorA.y * SCALE);
      ctx.lineTo(anchorB.x * SCALE, anchorB.y * SCALE);
      ctx.stroke();
    }
  }
};

// Log physics data for debugging
const logPhysicsData = (world) => {
  if (!debugMode) return;
  
  console.log('--- Physics World Debug ---');
  
  // Log bodies
  let bodyCount = 0;
  for (let body = world.getBodyList(); body; body = body.getNext()) {
    bodyCount++;
    
    const position = body.getPosition();
    const velocity = body.getLinearVelocity();
    const userData = body.getUserData();
    
    console.log(`Body #${bodyCount}:`);
    console.log(`  Position: (${position.x.toFixed(2)}, ${position.y.toFixed(2)})`);
    console.log(`  Velocity: (${velocity.x.toFixed(2)}, ${velocity.y.toFixed(2)})`);
    console.log(`  Awake: ${body.isAwake()}`);
    console.log(`  Type: ${body.getType()}`);
    
    if (userData) {
      console.log(`  UserData: ${JSON.stringify(userData)}`);
    }
  }
  
  // Log contacts
  let contactCount = 0;
  for (let contact = world.getContactList(); contact; contact = contact.getNext()) {
    contactCount++;
  }
  
  console.log(`Total Bodies: ${bodyCount}`);
  console.log(`Total Contacts: ${contactCount}`);
};

// Create a parameter adjustment UI
const createDebugControls = (container, params, onChange) => {
  if (!debugMode) return;
  
  // Clear existing controls
  container.innerHTML = '';
  
  // Create header
  const header = document.createElement('h3');
  header.textContent = 'Physics Debug Controls';
  container.appendChild(header);
  
  // Create controls for each parameter
  Object.entries(params).forEach(([key, value]) => {
    const controlDiv = document.createElement('div');
    controlDiv.className = 'debug-control';
    
    const label = document.createElement('label');
    label.textContent = key;
    controlDiv.appendChild(label);
    
    if (typeof value === 'boolean') {
      // Checkbox for boolean values
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = value;
      checkbox.addEventListener('change', () => {
        onChange(key, checkbox.checked);
      });
      controlDiv.appendChild(checkbox);
    } else if (typeof value === 'number') {
      // Slider for numeric values
      const slider = document.createElement('input');
      slider.type = 'range';
      slider.min = 0;
      slider.max = value * 2;
      slider.step = value / 100;
      slider.value = value;
      
      const valueDisplay = document.createElement('span');
      valueDisplay.textContent = value.toFixed(2);
      
      slider.addEventListener('input', () => {
        const newValue = parseFloat(slider.value);
        valueDisplay.textContent = newValue.toFixed(2);
        onChange(key, newValue);
      });
      
      controlDiv.appendChild(slider);
      controlDiv.appendChild(valueDisplay);
    }
    
    container.appendChild(controlDiv);
  });
  
  // Add toggle debug mode button
  const toggleButton = document.createElement('button');
  toggleButton.textContent = 'Toggle Debug Mode';
  toggleButton.addEventListener('click', () => {
    const newMode = toggleDebugMode();
    toggleButton.textContent = newMode ? 'Debug: ON' : 'Debug: OFF';
  });
  container.appendChild(toggleButton);
};

export {
  toggleDebugMode,
  getDebugMode,
  drawDebugInfo,
  logPhysicsData,
  createDebugControls
};
