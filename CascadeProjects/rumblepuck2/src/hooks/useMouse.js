import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Custom hook for handling mouse input
 * @param {Object} options - Configuration options
 * @param {HTMLElement} options.element - Element to track mouse events on (defaults to document)
 * @param {boolean} options.preventDefault - Whether to prevent default browser behavior
 * @returns {Object} Mouse state and utility functions
 */
const useMouse = ({
  element = null,
  preventDefault = false
} = {}) => {
  // State for mouse position and buttons
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [buttons, setButtons] = useState({
    left: false,
    middle: false,
    right: false
  });
  const [isInside, setIsInside] = useState(false);
  
  // Reference to the tracked element
  const elementRef = useRef(element);
  
  // Update element reference if it changes
  useEffect(() => {
    elementRef.current = element;
  }, [element]);
  
  // Handle mouse move
  const handleMouseMove = useCallback((event) => {
    if (preventDefault) {
      event.preventDefault();
    }
    
    // Calculate position relative to the element or page
    let x, y;
    
    if (elementRef.current) {
      const rect = elementRef.current.getBoundingClientRect();
      x = event.clientX - rect.left;
      y = event.clientY - rect.top;
    } else {
      x = event.clientX;
      y = event.clientY;
    }
    
    setPosition({ x, y });
  }, [preventDefault]);
  
  // Handle mouse down
  const handleMouseDown = useCallback((event) => {
    if (preventDefault) {
      event.preventDefault();
    }
    
    // Update button state based on which button was pressed
    setButtons(prev => ({
      ...prev,
      left: event.button === 0 ? true : prev.left,
      middle: event.button === 1 ? true : prev.middle,
      right: event.button === 2 ? true : prev.right
    }));
  }, [preventDefault]);
  
  // Handle mouse up
  const handleMouseUp = useCallback((event) => {
    if (preventDefault) {
      event.preventDefault();
    }
    
    // Update button state based on which button was released
    setButtons(prev => ({
      ...prev,
      left: event.button === 0 ? false : prev.left,
      middle: event.button === 1 ? false : prev.middle,
      right: event.button === 2 ? false : prev.right
    }));
  }, [preventDefault]);
  
  // Handle mouse enter
  const handleMouseEnter = useCallback(() => {
    setIsInside(true);
  }, []);
  
  // Handle mouse leave
  const handleMouseLeave = useCallback(() => {
    setIsInside(false);
    
    // Reset buttons when mouse leaves the element
    setButtons({
      left: false,
      middle: false,
      right: false
    });
  }, []);
  
  // Add and remove event listeners
  useEffect(() => {
    const target = elementRef.current || document;
    
    // Add event listeners
    target.addEventListener('mousemove', handleMouseMove);
    target.addEventListener('mousedown', handleMouseDown);
    target.addEventListener('mouseup', handleMouseUp);
    
    // Only add enter/leave listeners if we have a specific element
    if (elementRef.current) {
      target.addEventListener('mouseenter', handleMouseEnter);
      target.addEventListener('mouseleave', handleMouseLeave);
    }
    
    // Cleanup
    return () => {
      target.removeEventListener('mousemove', handleMouseMove);
      target.removeEventListener('mousedown', handleMouseDown);
      target.removeEventListener('mouseup', handleMouseUp);
      
      if (elementRef.current) {
        target.removeEventListener('mouseenter', handleMouseEnter);
        target.removeEventListener('mouseleave', handleMouseLeave);
      }
    };
  }, [
    handleMouseMove, 
    handleMouseDown, 
    handleMouseUp, 
    handleMouseEnter, 
    handleMouseLeave
  ]);
  
  // Calculate angle from center to mouse position
  const getAngleFromCenter = useCallback((centerX, centerY) => {
    const dx = position.x - centerX;
    const dy = position.y - centerY;
    
    // Calculate angle in radians, then convert to degrees
    let angle = Math.atan2(dy, dx) * (180 / Math.PI);
    
    // Normalize to 0-360 range
    if (angle < 0) {
      angle += 360;
    }
    
    return angle;
  }, [position]);
  
  // Calculate distance from center to mouse position
  const getDistanceFromCenter = useCallback((centerX, centerY) => {
    const dx = position.x - centerX;
    const dy = position.y - centerY;
    
    return Math.sqrt(dx * dx + dy * dy);
  }, [position]);
  
  return {
    position,
    buttons,
    isInside,
    getAngleFromCenter,
    getDistanceFromCenter
  };
};

export default useMouse;
