// Neon Space Tower Defense - Milestone 1
// Core game state, rendering, and UI update

const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

// Upgrade data
const upgrades = {
  guns: {
    level: 1, effect: 1, cost: [100, 200, 300],
    get effectVal() { return this.level; },
    max: 4
  },
  turning: {
    level: 1, effect: 2, cost: [100, 200, 300],
    get effectVal() { return 2 + (this.level - 1); },
    max: 4
  },
  firing: {
    level: 1, effect: 1.2, cost: [100, 200, 300],
    get effectVal() { return 1.2 + (this.level - 1) * 0.5; },
    max: 4
  },
  harden: {
    level: 1, effect: 0, cost: [150, 250, 350],
    get effectVal() { return [0, 0.2, 0.3, 0.4][this.level - 1]; },
    max: 4
  },
  aoe: {
    level: 0, cost: [250, 350, 500],
    get effectVal() { return [0, 120, 180, 260][this.level]; }, // AoE radius (bigger)
    max: 3,
    locked: () => gameState.wave < 3 // Unlock at wave 3
  },
  laser: {
    level: 0, cost: [300, 400, 600],
    get effectVal() { return [0, 35, 50, 70][this.level]; }, // Laser DPS
    max: 3,
    locked: () => upgrades.aoe.level === 0 // Unlock after AoE
  },
  missile: {
    level: 0, cost: [350, 500, 700],
    get effectVal() { return [0, 1, 2, 3][this.level]; }, // Missiles per volley
    max: 3,
    locked: () => upgrades.laser.level === 0 // Unlock after Laser
  }
};

// Game State
const gameState = {
  currency: 0,
  health: 100,
  wave: 1,
  tower: {
    x: canvas.width / 2,
    y: canvas.height / 2,
    rotation: 0 // radians
  },
  enemies: [],
  projectiles: [],
  gameOver: false
};

function earnCurrency(amount) {
  gameState.currency += amount;
  updateUI();
}
function spendCurrency(amount) {
  if (gameState.currency >= amount) {
    gameState.currency -= amount;
    updateUI();
    return true;
  }
  return false;
}

function upgrade(type) {
  const up = upgrades[type];
  // Some upgrades start at level 0 (aoe, laser, missile), others at 1
  const costIndex = (up.level === 0 && (type === 'aoe' || type === 'laser' || type === 'missile')) ? 0 : up.level - 1;
  if (up.level < up.max && spendCurrency(up.cost[costIndex])) {
    up.level++;
    updateUI();
  }
}

// Enemy types
const enemyTypes = {
  drone: { health: 20, speed: 4, damage: 5, reward: 5, color: '#39ff14' },
  bomber: { health: 50, speed: 2, damage: 15, reward: 15, color: '#ff1744' }
};

// Spawn a new wave
function spawnWave(wave) {
  const enemyCount = 3 + wave;
  // Bias: first half bombers (red), second half drones (green)
  const types = [];
  const half = Math.floor(enemyCount / 2);
  for (let i = 0; i < enemyCount; i++) {
    if (i < half) {
      // Higher chance for bomber early
      types.push(Math.random() < 0.7 ? 'bomber' : 'drone');
    } else {
      // Higher chance for drone late
      types.push(Math.random() < 0.7 ? 'drone' : 'bomber');
    }
  }
  // Optionally: slight shuffle for some variation, but keep bias
  for (let i = half - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [types[i], types[j]] = [types[j], types[i]];
  }
  for (let i = enemyCount - 1; i > half; i--) {
    const j = half + 1 + Math.floor(Math.random() * (i - half));
    [types[i], types[j]] = [types[j], types[i]];
  }
  // Spawn enemies in biased order
  for (let i = 0; i < enemyCount; i++) {
    const type = types[i];
    const angle = Math.random() * 2 * Math.PI;
    const distance = Math.max(canvas.width, canvas.height);
    const x = canvas.width / 2 + Math.cos(angle) * distance;
    const y = canvas.height / 2 + Math.sin(angle) * distance;
    const base = enemyTypes[type];
    gameState.enemies.push({
      type,
      x,
      y,
      health: base.health,
      maxHealth: base.health,
      speed: base.speed * 0.5,
      damage: base.damage,
      reward: base.reward,
      color: base.color
    });
  }
}

// Projectile
function fireProjectile(target) {
  const angle = Math.atan2(target.y - gameState.tower.y, target.x - gameState.tower.x);
  gameState.projectiles.push({
    x: gameState.tower.x,
    y: gameState.tower.y,
    angle,
    speed: 8,
    damage: 20
  });
}

let lastShotTime = 0;
let lastAoeTime = 0;
let aoeAnim = null; // {start, radius, active} for visual


// Starfield background
const stars = [];
const STAR_COUNT = 120;
for (let i = 0; i < STAR_COUNT; i++) {
  stars.push({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    radius: Math.random() * 1.5 + 0.5,
    alpha: Math.random() * 0.7 + 0.3
  });
}

function drawBackground() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  // Draw stars
  for (const star of stars) {
    ctx.save();
    ctx.globalAlpha = star.alpha;
    ctx.beginPath();
    ctx.arc(star.x, star.y, star.radius, 0, 2 * Math.PI);
    ctx.fillStyle = '#fff';
    ctx.shadowBlur = 8;
    ctx.shadowColor = '#00f2ff';
    ctx.fill();
    ctx.restore();
  }
}

function drawEnemies() {
  for (const enemy of gameState.enemies) {
    ctx.save();
    ctx.beginPath();
    ctx.arc(enemy.x, enemy.y, 20, 0, 2 * Math.PI);
    if (enemy.aoeHit && enemy.aoeHit > 0) {
      ctx.fillStyle = '#fff';
      ctx.shadowBlur = 32;
      ctx.shadowColor = '#00fff7';
      enemy.aoeHit--;
    } else {
      ctx.fillStyle = enemy.color;
      ctx.shadowBlur = 16;
      ctx.shadowColor = enemy.color;
    }
    ctx.fill();
    // Health bar
    ctx.globalAlpha = 0.8;
    ctx.fillStyle = '#222';
    ctx.fillRect(enemy.x - 18, enemy.y - 28, 36, 6);
    ctx.fillStyle = '#fff';
    ctx.fillRect(enemy.x - 18, enemy.y - 28, 36 * (enemy.health / enemy.maxHealth), 6);
    ctx.globalAlpha = 1;
    ctx.restore();
  }
}

function drawProjectiles() {
  for (const p of gameState.projectiles) {
    ctx.save();
    ctx.beginPath();
    ctx.arc(p.x, p.y, 6, 0, 2 * Math.PI);
    ctx.fillStyle = '#fff';
    ctx.shadowBlur = 8;
    ctx.shadowColor = '#00f2ff';
    ctx.fill();
    ctx.restore();
  }
}

function drawAoe() {
  if (aoeAnim && (aoeAnim.active || aoeAnim.flash > 0.1)) {
    // Screen flash
    if (aoeAnim.flash > 0.1) {
      ctx.save();
      ctx.globalAlpha = Math.min(0.18, aoeAnim.flash);
      ctx.fillStyle = '#00fff7';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.restore();
    }
    // Main shockwave
    if (aoeAnim.active) {
      ctx.save();
      for (let i = 0; i < 3; i++) {
        ctx.beginPath();
        ctx.arc(gameState.tower.x, gameState.tower.y, aoeAnim.radius * (1 - i*0.18), 0, 2 * Math.PI);
        ctx.strokeStyle = `rgba(0,255,255,${0.28-0.09*i})`;
        ctx.lineWidth = 10 - 3*i;
        ctx.shadowBlur = 30 - 8*i;
        ctx.shadowColor = '#00fff7';
        ctx.stroke();
      }
      ctx.restore();
      // Particle sparks
      if (aoeAnim.sparks) {
        for (const s of aoeAnim.sparks) {
          ctx.save();
          ctx.globalAlpha = s.alpha;
          ctx.beginPath();
          ctx.arc(s.x, s.y, 2.2, 0, 2 * Math.PI);
          ctx.fillStyle = '#00fff7';
          ctx.shadowBlur = 8;
          ctx.shadowColor = '#00fff7';
          ctx.fill();
          ctx.restore();
        }
      }
    }
  }
}

function drawTower() {
  const { x, y, rotation } = gameState.tower;
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(rotation);
  // Draw tower base
  ctx.beginPath();
  ctx.arc(0, 0, 32, 0, 2 * Math.PI);
  ctx.fillStyle = '#1e90ff';
  ctx.shadowBlur = 24;
  ctx.shadowColor = '#00f2ff';
  ctx.fill();
  // Draw glowing core
  ctx.beginPath();
  ctx.arc(0, 0, 12, 0, 2 * Math.PI);
  ctx.fillStyle = '#00f2ff';
  ctx.shadowBlur = 18;
  ctx.shadowColor = '#00e0ff';
  ctx.fill();
  // Draw gun barrel (static for now)
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.lineTo(0, -38);
  ctx.lineWidth = 8;
  ctx.strokeStyle = '#fff';
  ctx.shadowBlur = 10;
  ctx.shadowColor = '#00f2ff';
  ctx.stroke();
  ctx.restore();
}

function updateUI() {
  document.getElementById('currency').textContent = `Credits: ${gameState.currency}`;
  document.getElementById('health').textContent = `Health: ${gameState.health}`;
  document.getElementById('wave').textContent = `Wave: ${gameState.wave}`;
  // Upgrade buttons
  const gun = upgrades.guns;
  const turning = upgrades.turning;
  const firing = upgrades.firing;
  const harden = upgrades.harden;
  const aoe = upgrades.aoe;
  const laser = upgrades.laser;
  const missile = upgrades.missile;
  // Guns
  document.getElementById('upgrade-guns').textContent = `Guns: ${gun.level} / ${gun.max}\nCost: ${gun.level < gun.max ? gun.cost[gun.level-1] : '-'}\n+1 gun`;
  document.getElementById('upgrade-guns').disabled = gun.level >= gun.max || gameState.currency < gun.cost[gun.level-1];
  // Turning
  document.getElementById('upgrade-turning').textContent = `Turning: ${turning.level} / ${turning.max}\nCost: ${turning.level < turning.max ? turning.cost[turning.level-1] : '-'}\n${turning.effectVal}°/frame`;
  document.getElementById('upgrade-turning').disabled = turning.level >= turning.max || gameState.currency < turning.cost[turning.level-1];
  // Firing
  document.getElementById('upgrade-firing').textContent = `Firing: ${firing.level} / ${firing.max}\nCost: ${firing.level < firing.max ? firing.cost[firing.level-1] : '-'}\n${firing.effectVal.toFixed(2)} shots/sec`;
  document.getElementById('upgrade-firing').disabled = firing.level >= firing.max || gameState.currency < firing.cost[firing.level-1];
  // Harden
  document.getElementById('upgrade-harden').textContent = `Harden: ${harden.level} / ${harden.max}\nCost: ${harden.level < harden.max ? harden.cost[harden.level-1] : '-'}\n-${Math.round(harden.effectVal*100)}% dmg`;
  document.getElementById('upgrade-harden').disabled = harden.level >= harden.max || gameState.currency < harden.cost[harden.level-1];
  // AoE
  let aoeBtn = document.getElementById('upgrade-aoe');
  if (aoe.locked()) {
    aoeBtn.textContent = `AoE: Locked\nUnlocks at Wave 3`;
    aoeBtn.classList.add('locked');
    aoeBtn.disabled = true;
  } else {
    aoeBtn.textContent = `AoE: ${aoe.level} / ${aoe.max}\nCost: ${aoe.level < aoe.max ? aoe.cost[aoe.level] : '-'}\n+${aoe.effectVal} radius`;
    aoeBtn.classList.remove('locked');
    aoeBtn.disabled = aoe.level >= aoe.max || gameState.currency < aoe.cost[aoe.level];
  }
  // Laser
  let laserBtn = document.getElementById('upgrade-laser');
  if (laser.locked()) {
    laserBtn.textContent = `Laser: Locked\nRequires AoE`;
    laserBtn.classList.add('locked');
    laserBtn.disabled = true;
  } else {
    laserBtn.textContent = `Laser: ${laser.level} / ${laser.max}\nCost: ${laser.level < laser.max ? laser.cost[laser.level] : '-'}\n+${laser.effectVal} DPS`;
    laserBtn.classList.remove('locked');
    laserBtn.disabled = laser.level >= laser.max || gameState.currency < laser.cost[laser.level];
  }
  // Missile
  let missileBtn = document.getElementById('upgrade-missile');
  if (missile.locked()) {
    missileBtn.textContent = `Missile: Locked\nRequires Laser`;
    missileBtn.classList.add('locked');
    missileBtn.disabled = true;
  } else {
    missileBtn.textContent = `Missile: ${missile.level} / ${missile.max}\nCost: ${missile.level < missile.max ? missile.cost[missile.level] : '-'}\n+${missile.effectVal} missiles`;
    missileBtn.classList.remove('locked');
    missileBtn.disabled = missile.level >= missile.max || gameState.currency < missile.cost[missile.level];
  }
}
// Button listeners
window.onload = () => {
  updateUI();
  spawnWave(gameState.wave);
  document.getElementById('upgrade-guns').onclick = () => { upgrade('guns'); };
  document.getElementById('upgrade-turning').onclick = () => { upgrade('turning'); };
  document.getElementById('upgrade-firing').onclick = () => { upgrade('firing'); };
  document.getElementById('upgrade-harden').onclick = () => { upgrade('harden'); };
  document.getElementById('upgrade-aoe').onclick = () => { if (!upgrades.aoe.locked()) upgrade('aoe'); };
  document.getElementById('upgrade-laser').onclick = () => { if (!upgrades.laser.locked()) upgrade('laser'); };
  document.getElementById('upgrade-missile').onclick = () => { if (!upgrades.missile.locked()) upgrade('missile'); };
  gameLoop(performance.now());
};

function gameLoop(timestamp) {
  drawBackground();
  drawTower();
  drawAoe();
  drawEnemies();
  drawProjectiles();
  updateUI();
  if (gameState.gameOver) {
    ctx.save();
    ctx.font = 'bold 48px Orbitron, Arial';
    ctx.fillStyle = '#ff1744';
    ctx.textAlign = 'center';
    ctx.shadowBlur = 18;
    ctx.shadowColor = '#ff1744';
    ctx.fillText('GAME OVER', canvas.width / 2, canvas.height / 2);
    ctx.restore();
    return;
  }
  // --- Tower rotation ---
  let closest = null, minDist = Infinity;
  for (const enemy of gameState.enemies) {
    const dx = enemy.x - gameState.tower.x;
    const dy = enemy.y - gameState.tower.y;
    const dist = Math.sqrt(dx*dx + dy*dy);
    if (dist < minDist) {
      minDist = dist;
      closest = enemy;
    }
  }
  // Multi-gun targeting
  let targets = [];
  if (gameState.enemies.length > 0) {
    // Get N closest enemies
    targets = [...gameState.enemies]
      .map(e => ({e, d: Math.hypot(e.x-gameState.tower.x, e.y-gameState.tower.y)}))
      .sort((a,b) => a.d-b.d)
      .slice(0, upgrades.guns.level)
      .map(obj => obj.e);
  }
  if (closest) {
    const targetAngle = Math.atan2(closest.y - gameState.tower.y, closest.x - gameState.tower.x);
    let delta = targetAngle - gameState.tower.rotation;
    // Normalize angle
    while (delta > Math.PI) delta -= 2 * Math.PI;
    while (delta < -Math.PI) delta += 2 * Math.PI;
    // Rotate up to X deg/frame
    const maxStep = upgrades.turning.effectVal * Math.PI / 180;
    if (Math.abs(delta) < maxStep) {
      gameState.tower.rotation = targetAngle;
    } else {
      gameState.tower.rotation += Math.sign(delta) * maxStep;
    }
  }
  // --- Firing ---
  const firingInterval = 1000 / upgrades.firing.effectVal;
  if (targets.length > 0 && timestamp - lastShotTime > firingInterval) {
    for (const t of targets) fireProjectile(t);
    lastShotTime = timestamp;
  }
  // --- AoE Shockwave ---
  if (upgrades.aoe.level > 0) {
    const aoeCooldown = 2000; // 2 seconds
    if (!lastAoeTime) lastAoeTime = timestamp;
    if (timestamp - lastAoeTime > aoeCooldown) {
      // Activate AoE
      const radius = upgrades.aoe.effectVal;
      aoeAnim = { start: timestamp, radius: 0, max: radius, active: true, flash: 1, sparks: [] };
      // Damage enemies
      for (const enemy of gameState.enemies) {
        const dx = enemy.x - gameState.tower.x, dy = enemy.y - gameState.tower.y;
        if (Math.sqrt(dx*dx + dy*dy) < radius) {
          enemy.health -= 80 + 40 * upgrades.aoe.level; // much more damage
          enemy.aoeHit = 8; // frames to flash
        }
      }
      lastAoeTime = timestamp;
    }
    // Animate AoE
    if (aoeAnim && (aoeAnim.active || aoeAnim.flash > 0.1)) {
      const elapsed = timestamp - aoeAnim.start;
      if (aoeAnim.active) {
        aoeAnim.radius = Math.min(aoeAnim.max, (aoeAnim.max) * (elapsed / 450));
        if (elapsed > 450) aoeAnim.active = false;
        // Particle sparks
        if (elapsed < 400) {
          for (let i = 0; i < 10; i++) {
            const angle = Math.random() * 2 * Math.PI;
            const dist = aoeAnim.radius + 10;
            aoeAnim.sparks.push({
              x: gameState.tower.x + Math.cos(angle) * dist,
              y: gameState.tower.y + Math.sin(angle) * dist,
              dx: Math.cos(angle) * (2 + Math.random()*2),
              dy: Math.sin(angle) * (2 + Math.random()*2),
              alpha: 1
            });
          }
        }
        aoeAnim.sparks = (aoeAnim.sparks||[]).map(s => ({...s, x: s.x+s.dx, y: s.y+s.dy, alpha: s.alpha-0.04})).filter(s => s.alpha > 0.1);
      }
      // Animate flash
      if (aoeAnim.flash) aoeAnim.flash -= 0.08;
    }
  }
  // --- Update projectiles ---
  for (let i = gameState.projectiles.length - 1; i >= 0; i--) {
    const p = gameState.projectiles[i];
    p.x += Math.cos(p.angle) * p.speed;
    p.y += Math.sin(p.angle) * p.speed;
    // Remove if out of bounds
    if (p.x < 0 || p.x > canvas.width || p.y < 0 || p.y > canvas.height) {
      gameState.projectiles.splice(i, 1);
      continue;
    }
    // Check collision with enemies
    for (let j = gameState.enemies.length - 1; j >= 0; j--) {
      const e = gameState.enemies[j];
      const dx = p.x - e.x, dy = p.y - e.y;
      if (dx*dx + dy*dy < 26*26) { // 20+6 radius
        e.health -= p.damage;
        gameState.projectiles.splice(i, 1);
        if (e.health <= 0) {
          gameState.currency += e.reward;
          gameState.enemies.splice(j, 1);
        }
        break;
      }
    }
  }
  // --- Update enemies ---
  for (let i = gameState.enemies.length - 1; i >= 0; i--) {
    const e = gameState.enemies[i];
    const dx = gameState.tower.x - e.x, dy = gameState.tower.y - e.y;
    const dist = Math.sqrt(dx*dx + dy*dy);
    if (dist < 32 + 20) { // Tower radius + enemy radius
      // Apply harden upgrade
      const dmg = e.damage * (1 - upgrades.harden.effectVal);
      gameState.health -= dmg;
      gameState.enemies.splice(i, 1);
      if (gameState.health <= 0) {
        gameState.health = 0;
        gameState.gameOver = true;
      }
      continue;
    }
    // Move toward tower
    e.x += (dx / dist) * e.speed;
    e.y += (dy / dist) * e.speed;
  }
  // --- Wave progression ---
  if (gameState.enemies.length === 0) {
    gameState.wave++;
    spawnWave(gameState.wave);
  }
  drawBackground();
  drawTower();
  drawEnemies();
  drawProjectiles();
  updateUI();
  requestAnimationFrame(gameLoop);
}



