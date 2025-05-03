import { useState, useEffect, useCallback } from 'react';

/**
 * Custom hook for handling keyboard input
 * @param {Object} options - Configuration options
 * @param {boolean} options.preventDefault - Whether to prevent default browser behavior for handled keys
 * @param {Array} options.targetKeys - Array of keys to specifically track (if empty, tracks all keys)
 * @returns {Object} Keyboard state and utility functions
 */
const useKeyboard = ({ 
  preventDefault = true,
  targetKeys = []
} = {}) => {
  // State to track pressed keys
  const [keys, setKeys] = useState({});
  
  // Check if a specific key is pressed
  const isKeyDown = useCallback((key) => {
    return !!keys[key];
  }, [keys]);
  
  // Handle key down event
  const handleKeyDown = useCallback((event) => {
    const { key } = event;
    
    console.log('Key down event:', key);
    
    // If we're only tracking specific keys and this isn't one of them, ignore
    if (targetKeys.length > 0 && !targetKeys.includes(key)) {
      console.log('Key not in target keys:', key, 'Target keys:', targetKeys);
      return;
    }
    
    // Prevent default behavior if requested
    if (preventDefault) {
      event.preventDefault();
    }
    
    // Update state only if the key state is changing
    setKeys(prevKeys => {
      if (prevKeys[key]) return prevKeys;
      console.log('Adding key to pressed keys:', key);
      return { ...prevKeys, [key]: true };
    });
  }, [preventDefault, targetKeys]);
  
  // Handle key up event
  const handleKeyUp = useCallback((event) => {
    const { key } = event;
    
    // If we're only tracking specific keys and this isn't one of them, ignore
    if (targetKeys.length > 0 && !targetKeys.includes(key)) {
      return;
    }
    
    // Prevent default behavior if requested
    if (preventDefault) {
      event.preventDefault();
    }
    
    // Update state only if the key state is changing
    setKeys(prevKeys => {
      if (!prevKeys[key]) return prevKeys;
      
      // Create a new object without the released key
      const newKeys = { ...prevKeys };
      delete newKeys[key];
      return newKeys;
    });
  }, [preventDefault, targetKeys]);
  
  // Add and remove event listeners
  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    
    // Cleanup
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [handleKeyDown, handleKeyUp]);
  
  return {
    keys,
    isKeyDown
  };
};

export default useKeyboard;
