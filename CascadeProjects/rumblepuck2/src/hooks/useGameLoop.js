import { useRef, useEffect, useState, useCallback } from 'react';

/**
 * Custom hook for managing the game loop
 * @param {Function} update - Function to call on each update
 * @param {Function} render - Function to call on each render
 * @param {number} fps - Target frames per second (default: 60)
 * @returns {Object} Game loop controls and stats
 */
const useGameLoop = (update, render, fps = 60) => {
  const [isRunning, setIsRunning] = useState(false);
  const [fpsCount, setFpsCount] = useState(0);
  
  const requestRef = useRef();
  const previousTimeRef = useRef();
  const fpsIntervalRef = useRef();
  const frameCountRef = useRef(0);
  
  // Target time between frames in ms
  const frameInterval = 1000 / fps;
  
  // Start the game loop
  const start = useCallback(() => {
    if (!isRunning) {
      setIsRunning(true);
    }
  }, [isRunning]);
  
  // Stop the game loop
  const stop = useCallback(() => {
    if (isRunning) {
      setIsRunning(false);
    }
  }, [isRunning]);
  
  // Toggle the game loop
  const toggle = useCallback(() => {
    setIsRunning(prev => !prev);
  }, []);
  
  // The animation loop
  useEffect(() => {
    if (!isRunning) return;
    
    let lastFrameTime = 0;
    
    // Set up FPS counter
    frameCountRef.current = 0;
    const countFPS = () => {
      setFpsCount(frameCountRef.current);
      frameCountRef.current = 0;
    };
    
    fpsIntervalRef.current = setInterval(countFPS, 1000);
    
    // Animation frame callback
    const animate = (timestamp) => {
      if (previousTimeRef.current === undefined) {
        previousTimeRef.current = timestamp;
      }
      
      const deltaTime = timestamp - previousTimeRef.current;
      previousTimeRef.current = timestamp;
      
      // Call update with delta time in seconds
      if (update) {
        update(deltaTime / 1000);
      }
      
      // Determine if we should render this frame
      const elapsed = timestamp - lastFrameTime;
      
      if (elapsed >= frameInterval) {
        // Call render
        if (render) {
          render();
        }
        
        // Adjust for the frame interval
        lastFrameTime = timestamp - (elapsed % frameInterval);
        
        // Increment frame counter for FPS calculation
        frameCountRef.current++;
      }
      
      // Request next frame
      requestRef.current = requestAnimationFrame(animate);
    };
    
    requestRef.current = requestAnimationFrame(animate);
    
    // Cleanup
    return () => {
      cancelAnimationFrame(requestRef.current);
      clearInterval(fpsIntervalRef.current);
      previousTimeRef.current = undefined;
    };
  }, [isRunning, update, render, frameInterval]);
  
  return {
    isRunning,
    start,
    stop,
    toggle,
    fps: fpsCount
  };
};

export default useGameLoop;
