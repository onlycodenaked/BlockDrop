// Game field dimensions
export const FIELD_WIDTH = 1600;
export const FIELD_HEIGHT = 900;

// Zone boundaries (x-axis)
export const ZONES = {
  CLOSE: { min: 0, max: 600, points: 1 },
  MID: { min: 600, max: 1000, points: 2 },
  FAR: { min: 1000, max: 1300, points: 4 },
  HAZARD: { min: 1300, max: 1600, points: 0 }
};

// Puck classes
export const PUCK_CLASSES = {
  BRUISER: 'bruiser',
  TACTICIAN: 'tactician',
  SNIPER: 'sniper'
};

// Puck class colors
export const CLASS_COLORS = {
  [PUCK_CLASSES.BRUISER]: '#e74c3c', // Red
  [PUCK_CLASSES.TACTICIAN]: '#3498db', // Blue
  [PUCK_CLASSES.SNIPER]: '#2ecc71' // Green
};

// Ability ranges and parameters
export const ABILITY_PARAMS = {
  // Bruiser abilities
  BLAST: { radius: 115, force: 40 },
  REPEL: { radius: 100, force: 60 },
  RAM: { distance: 75 },
  SHIELD: { duration: Infinity }, // Lasts until end of round
  ANCHOR: { immovable: true },
  CRUSH: { multiplier: 2 },
  SPIKE: { radius: 80, force: 100 },
  WALL: { size: 50, distance: 100, duration: 1000 }, // 1 second

  // Tactician abilities
  MAGNET: { radius: 150, force: 50 },
  DISRUPTOR: { radius: 120 },
  MIRAGE: { radius: 200 },
  BEACON: { radius: 50, bonus: 1 },
  PULSE: { radius: 100, force: 20, pulses: 3, interval: 500 }, // 500ms
  SWAP: { radius: 150 },
  LINK: { radius: 100, distance: 50 },
  ECHO: { radius: 120 },

  // Sniper abilities
  SNIPER: { multiplier: 2 },
  PIERCE: { distance: 100, force: 30 },
  TARGET: { bonus: 2 },
  SCOPE: { bonus: 1 },
  RIFLE: { radius: 300, force: 50 },
  MARK: { radius: 200 },
  AIM: { accuracyBonus: 0.1 }, // 10%
  SHOT: { bonus: 3 }
};

// Game settings
export const GAME_SETTINGS = {
  ROUNDS: 3,
  PUCKS_PER_ROUND: 4,
  DECK_SIZE: 12,
  MAX_SAME_PUCK: 2,
  MIN_UNIQUE_PUCKS: 6,
  FAR_ZONE_CONTROL_BONUS: 3,
  POWER_LOAD_TIME: 2000, // 2 seconds to fully load power bar
};

// Bot AI settings
export const BOT_SETTINGS = {
  NORMAL: {
    accuracy: 0.8,
    deviation: 0.1
  },
  HARD: {
    accuracy: 0.95,
    deviation: 0.05,
    weights: {
      position: 0.4,
      collision: 0.3,
      combo: 0.3
    }
  }
};
