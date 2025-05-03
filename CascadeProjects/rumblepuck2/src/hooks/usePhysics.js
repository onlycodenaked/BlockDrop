import { useState, useEffect, useRef } from 'react';
import { createWorld } from '../lib/physics';

/**
 * Custom hook for managing the physics world and simulation
 * @param {Object} options - Configuration options
 * @param {boolean} options.autoStart - Whether to start the simulation automatically
 * @param {number} options.timeStep - Physics simulation time step (default: 1/60)
 * @param {number} options.velocityIterations - Velocity iterations for physics solver
 * @param {number} options.positionIterations - Position iterations for physics solver
 * @returns {Object} Physics world and control functions
 */
const usePhysics = ({
  autoStart = true,
  timeStep = 1/60,
  velocityIterations = 8,
  positionIterations = 3
} = {}) => {
  // Create physics world
  const [world] = useState(() => createWorld());
  const [running, setRunning] = useState(autoStart);
  const requestRef = useRef();
  const previousTimeRef = useRef();
  const accumulatorRef = useRef(0);

  // Start the physics simulation
  const startSimulation = () => {
    if (!running) {
      setRunning(true);
    }
  };

  // Stop the physics simulation
  const stopSimulation = () => {
    if (running) {
      setRunning(false);
    }
  };

  // Reset the physics world
  const resetWorld = () => {
    // Clear all dynamic bodies
    for (let body = world.getBodyList(); body; body = body.getNext()) {
      if (body.isDynamic()) {
        world.destroyBody(body);
      }
    }
  };

  // Animation loop for physics simulation
  useEffect(() => {
    if (!running) return;

    const animate = (time) => {
      if (previousTimeRef.current !== undefined) {
        const deltaTime = (time - previousTimeRef.current) / 1000;
        
        // Fixed time step accumulator
        accumulatorRef.current += deltaTime;
        
        // Step physics multiple times if needed to maintain fixed time step
        while (accumulatorRef.current >= timeStep) {
          world.step(timeStep, velocityIterations, positionIterations);
          accumulatorRef.current -= timeStep;
        }
      }
      
      previousTimeRef.current = time;
      requestRef.current = requestAnimationFrame(animate);
    };

    requestRef.current = requestAnimationFrame(animate);
    
    return () => {
      cancelAnimationFrame(requestRef.current);
      previousTimeRef.current = undefined;
      accumulatorRef.current = 0;
    };
  }, [running, world, timeStep, velocityIterations, positionIterations]);

  // Clean up physics world on unmount
  useEffect(() => {
    return () => {
      // Planck.js doesn't have a direct world.destroy() method,
      // but we can clear all bodies and joints
      for (let joint = world.getJointList(); joint; joint = joint.getNext()) {
        world.destroyJoint(joint);
      }
      
      for (let body = world.getBodyList(); body; body = body.getNext()) {
        world.destroyBody(body);
      }
    };
  }, [world]);

  return {
    world,
    running,
    startSimulation,
    stopSimulation,
    resetWorld
  };
};

export default usePhysics;
