package com.spaceshooter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.*;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread gameThread;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Random random;
    
    public PlayerShip player;
    public List<Bullet> playerBullets = new ArrayList<>();
    public List<Bullet> enemyBullets = new ArrayList<>();
    public List<Enemy> enemies = new ArrayList<>();
    public List<PowerUp> powerUps = new ArrayList<>();
    public List<Particle> particles = new ArrayList<>();
    public List<Star> stars = new ArrayList<>();
    public List<LaserBeam> lasers = new ArrayList<>();
    public List<Meteor> meteors = new ArrayList<>();
    public List<Nuke> nukes = new ArrayList<>();
    public List<BossMinion> bossMinions = new ArrayList<>();
    public List<HomingBullet> homingBullets = new ArrayList<>();
    public List<PlasmaBullet> plasmaBullets = new ArrayList<>();
    
    private int screenWidth, screenHeight;
    public int gameState = 0;
    public int score = 0;
    public int highScore = 0;
    public int level = 1;
    public int enemiesKilled = 0;
    public int totalKills = 0;
    private long lastEnemySpawn = 0;
    private long lastPowerUpSpawn = 0;
    private long lastMeteorSpawn = 0;
    public boolean bossActive = false;
    public Boss boss = null;
    private SharedPreferences prefs;
    private int touchX = 0, touchY = 0;
    private boolean touchPressed = false;
    private long lastShot = 0;
    public int fireRate = 200;
    private long lastNukeTap = 0;
    private boolean nukeReady = false;
    private long nukeCooldown = 0;
    
    private int comboCount = 0;
    private long lastKillTime = 0;
    private int comboMultiplier = 1;
    private long screenShakeTime = 0;
    private float screenShakeIntensity = 0;
    private boolean hasLaser = false;
    private long laserStartTime = 0;
    private long lastLaserTime = 0;
    private int waveAnnounceTime = 0;
    private String waveMessage = "";
    
    private int screenFlashTime = 0;
    private int screenFlashColor = 0;
    
    private List<String> achievements = new ArrayList<>();
    private int achievementNotifyTime = 0;
    private String achievementMessage = "";
    
    private int formationCount = 0;
    
    public int difficulty = 1;
    private int lives = 3;
    private long invincibleTime = 0;
    
    public static final int STATE_MENU = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_GAME_OVER = 3;
    public static final int STATE_LEVEL_COMPLETE = 4;
    public static final int STATE_BOSS = 5;
    public static final int STATE_VICTORY = 6;
    public static final int STATE_PAUSED = 7;
    public static final int STATE_DIFFICULTY = 8;

    public GameSurfaceView(Context context) {
        super(context);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);
        paint = new Paint();
        paint.setAntiAlias(true);
        random = new Random();
        prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("high_score", 0);
        totalKills = prefs.getInt("total_kills", 0);
        loadAchievements();
        initStars();
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < 150; i++) {
            stars.add(new Star(random.nextInt(1080), random.nextInt(1920), 1 + random.nextInt(3), 0.5f + random.nextFloat() * 2f));
        }
    }
    
    private void loadAchievements() {
        String saved = prefs.getString("achievements", "");
        if (!saved.isEmpty()) {
            for (String a : saved.split(",")) {
                if (!a.isEmpty()) achievements.add(a);
            }
        }
    }
    
    private void saveAchievements() {
        StringBuilder sb = new StringBuilder();
        for (String a : achievements) {
            sb.append(a).append(",");
        }
        prefs.edit().putString("achievements", sb.toString()).apply();
    }
    
    private void unlockAchievement(String name) {
        if (!achievements.contains(name)) {
            achievements.add(name);
            achievementMessage = "ACHIEVEMENT: " + name;
            achievementNotifyTime = 3000;
            saveAchievements();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth() > 0 ? getWidth() : 1080;
        screenHeight = getHeight() > 0 ? getHeight() : 1920;
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        initStars();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { stop(); }

    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stop() {
        running = false;
        try { gameThread.join(500); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (running) {
            long deltaTime = System.currentTimeMillis() - lastTime;
            lastTime = System.currentTimeMillis();
            if ((gameState == STATE_PLAYING || gameState == STATE_BOSS) && !paused) {
                update(deltaTime);
            }
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) synchronized (surfaceHolder) { render(canvas); }
            } finally {
                if (canvas != null) try { surfaceHolder.unlockCanvasAndPost(canvas); } catch (Exception e) {}
            }
            try { Thread.sleep(16); } catch (InterruptedException e) {}
        }
    }

    private void update(long dt) {
        for (Star s : stars) { s.y += s.speed * 2; if (s.y > screenHeight) { s.y = 0; s.x = random.nextInt(screenWidth); }}
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx * dt * 0.05f; p.y += p.vy * dt * 0.05f; p.life -= dt;
            if (p.life <= 0) particles.remove(i);
        }
        if (screenShakeTime > 0) screenShakeTime -= dt;
        if (screenFlashTime > 0) screenFlashTime -= dt;
        if (achievementNotifyTime > 0) achievementNotifyTime -= dt;
        
        if (nukeCooldown > 0) nukeCooldown -= dt;
        else nukeReady = true;
        
        if (invincibleTime > 0) invincibleTime -= dt;
        
        if (player != null && player.alive) {
            player.update(dt, touchX, touchY, touchPressed);
            int effRate = (int)(fireRate / Math.max(1, player.spreadShot));
            if (touchPressed && System.currentTimeMillis() - lastShot > effRate) { shoot(); lastShot = System.currentTimeMillis(); }
            if (hasLaser && System.currentTimeMillis() - laserStartTime < 5000) {
                if (System.currentTimeMillis() - lastLaserTime > 100) {
                    lasers.add(new LaserBeam(player.x + player.width / 2, player.y + player.height, screenHeight));
                    lastLaserTime = System.currentTimeMillis();
                }
            }
            updatePlayerBullets(dt);
            updateEnemyBullets(dt);
            updateLasers(dt);
            updateMeteors(dt);
            updateNukes(dt);
            updateHomingBullets(dt);
            updatePlasmaBullets(dt);
            updateEnemies(dt);
            updatePowerUps(dt);
            updateBossMinions(dt);
            if (bossActive && boss != null) updateBoss(dt);
            checkAllCollisions();
            checkAchievements();
            spawnEnemies();
        }
    }
    
    private void updatePlayerBullets(long dt) {
        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            b.y += b.vy * dt * 0.1f; b.x += b.vx * dt * 0.1f;
            if (b.y < -50 || b.x < -50 || b.x > screenWidth + 50) playerBullets.remove(i);
        }
    }
    
    private void updateEnemyBullets(long dt) {
        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            b.y += b.vy * dt * 0.1f; b.x += b.vx * dt * 0.1f;
            if (b.y > screenHeight + 50 || b.x < -50 || b.x > screenWidth + 50) enemyBullets.remove(i);
        }
    }
    
    private void updateLasers(long dt) {
        for (int i = lasers.size() - 1; i >= 0; i--) {
            LaserBeam l = lasers.get(i);
            l.update(dt);
            if (l.y > screenHeight) lasers.remove(i);
        }
    }
    
    private void updateMeteors(long dt) {
        for (int i = meteors.size() - 1; i >= 0; i--) {
            Meteor m = meteors.get(i);
            m.update(dt);
            if (m.y > screenHeight + 100) meteors.remove(i);
        }
    }
    
    private void updateNukes(long dt) {
        for (int i = nukes.size() - 1; i >= 0; i--) {
            Nuke n = nukes.get(i);
            n.update(dt);
            if (n.isExplosionDone()) {
                if (n.exploded) {
                    nukeReady = false;
                    nukeCooldown = 10000;
                    triggerNukeExplosion(n.targetX, n.targetY);
                }
                nukes.remove(i);
            }
        }
    }
    
    private void triggerNukeExplosion(float tx, float ty) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            float dx = e.x + e.width / 2 - tx;
            float dy = e.y + e.height / 2 - ty;
            if (Math.sqrt(dx * dx + dy * dy) < 200) {
                e.health = 0;
                score += e.scoreValue * comboMultiplier;
                enemiesKilled++;
                totalKills++;
                spawnExplosion(e.x + e.width / 2, e.y + e.height / 2, e.explosionColor);
                enemies.remove(i);
            }
        }
        for (int i = bossMinions.size() - 1; i >= 0; i--) {
            BossMinion m = bossMinions.get(i);
            float dx = m.x + m.width / 2 - tx;
            float dy = m.y + m.height / 2 - ty;
            if (Math.sqrt(dx * dx + dy * dy) < 200) {
                spawnExplosion(m.x + m.width / 2, m.y + m.height / 2, Color.MAGENTA);
                bossMinions.remove(i);
            }
        }
        if (boss != null) {
            float dx = boss.x + boss.width / 2 - tx;
            float dy = boss.y + boss.height / 2 - ty;
            if (Math.sqrt(dx * dx + dy * dy) < 250) {
                boss.health -= 100;
                if (boss.health <= 0) { score += 5000 * comboMultiplier; bossDefeated(); }
            }
        }
        triggerScreenShake(25, 800);
        triggerScreenFlash(Color.rgb(255, 200, 100), 400);
    }
    
    private void updateHomingBullets(long dt) {
        for (int i = homingBullets.size() - 1; i >= 0; i--) {
            HomingBullet hb = homingBullets.get(i);
            float targetX = screenWidth / 2;
            float targetY = screenHeight;
            if (player != null && hb.isPlayerBullet) {
                targetX = player.x + player.width / 2;
                targetY = 0;
                if (enemies.size() > 0) {
                    Enemy nearest = enemies.get(0);
                    float nearestDist = 10000;
                    for (Enemy e : enemies) {
                        float dx = e.x - hb.x;
                        float dy = e.y - hb.y;
                        float dist = (float)Math.sqrt(dx * dx + dy * dy);
                        if (dist < nearestDist && e.y < screenHeight / 2) {
                            nearest = e;
                            nearestDist = dist;
                        }
                    }
                    targetX = nearest.x + nearest.width / 2;
                    targetY = nearest.y + nearest.height / 2;
                }
            }
            hb.update(dt, null, targetX, targetY);
            if (hb.isExpired()) homingBullets.remove(i);
        }
    }
    
    private void updatePlasmaBullets(long dt) {
        for (int i = plasmaBullets.size() - 1; i >= 0; i--) {
            PlasmaBullet pb = plasmaBullets.get(i);
            pb.update(dt);
            if (pb.isExpired()) plasmaBullets.remove(i);
        }
    }
    
    private void updateEnemies(long dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(dt, player);
            if (e.shootTimer <= 0 && e.canShoot) { enemyShoot(e); e.shootTimer = 1000 + random.nextInt(2000); }
            e.shootTimer -= dt;
            if (e.y > screenHeight + 100) enemies.remove(i);
        }
    }
    
    private void updatePowerUps(long dt) {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp p = powerUps.get(i); p.y += 2; p.rotation += 2;
            if (p.y > screenHeight + 50) powerUps.remove(i);
        }
    }
    
    private void updateBossMinions(long dt) {
        for (int i = bossMinions.size() - 1; i >= 0; i--) {
            BossMinion m = bossMinions.get(i);
            m.update(dt, boss);
            if (m.shouldShoot()) {
                enemyBullets.add(new Bullet(m.x + m.width / 2, m.y + m.height, 0, 8, false));
            }
            if (m.y > screenHeight + 100) bossMinions.remove(i);
        }
    }
    
    private void updateBoss(long dt) {
        boss.update(dt, player);
        if (boss.shootTimer <= 0) { bossShoot(); boss.shootTimer = 500 + random.nextInt(1000); }
        boss.shootTimer -= dt;
        if (boss.bombTimer <= 0 && boss.canBomb) { bossBomb(); boss.bombTimer = 3000 + random.nextInt(3000); }
        boss.bombTimer -= dt;
        
        if (boss.phase >= 2 && bossMinions.size() < boss.phase * 2) {
            if (random.nextInt(500) < 5) {
                bossMinions.add(new BossMinion(boss.x + random.nextInt(boss.width), boss.y + boss.height, screenWidth, boss.phase));
            }
        }
        
        if (boss.health <= boss.maxHealth * 0.1f && !boss.enraged) {
            boss.enrage();
            unlockAchievement("Enrage Survivor");
        }
    }
    
    private void spawnEnemies() {
        long now = System.currentTimeMillis();
        int spawnRate = Math.max(300, 2000 - level * 200 - difficulty * 100);
        
        formationCount++;
        if (now - lastEnemySpawn > spawnRate && enemies.size() < 12 + level && !bossActive) {
            if (formationCount % 10 == 0 && level >= 3) {
                spawnFormation();
            } else {
                String[] types = {"scout", "fighter", "tank", "bomber", "chaser", "splitter"};
                if (level >= 5) types = addElement(types, "healer");
                enemies.add(Enemy.create(types[random.nextInt(types.length)], 50 + random.nextInt(screenWidth - 100), -50, level));
            }
            lastEnemySpawn = now;
        }
        
        if (now - lastMeteorSpawn > 8000 - difficulty * 500 && meteors.size() < 3 && level >= 2 && !bossActive) {
            int size = 30 + random.nextInt(40);
            meteors.add(new Meteor(random.nextInt(screenWidth), -size, size, screenHeight));
            lastMeteorSpawn = now;
        }
        
        if (now - lastPowerUpSpawn > 10000 && powerUps.size() < 3) {
            int type = random.nextInt(7);
            powerUps.add(new PowerUp(50 + random.nextInt(screenWidth - 100), -50, type));
            lastPowerUpSpawn = now;
        }
        if (enemiesKilled >= 12 * level && !bossActive) spawnBoss();
    }
    
    private void spawnFormation() {
        String[] types = {"scout", "fighter", "bomber"};
        String type = types[random.nextInt(types.length)];
        int count = 3 + level / 2;
        int spacing = 80;
        int startX = (screenWidth - count * spacing) / 2;
        
        for (int i = 0; i < count; i++) {
            Enemy e = Enemy.create(type, startX + i * spacing, -50 - i * 30, level);
            e.speed *= 0.5f;
            enemies.add(e);
        }
        waveMessage = "FORMATION!"; waveAnnounceTime = 1500;
    }
    
    private String[] addElement(String[] arr, String element) {
        String[] newArr = new String[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        newArr[arr.length] = element;
        return newArr;
    }
        
    private void checkAllCollisions() {
        if (player == null || !player.alive) return;
        RectF playerRect = player.getBounds();
        
        checkPlayerBulletsVsEnemies();
        checkPlayerBulletsVsMeteors();
        checkPlayerBulletsVsBossMinions();
        checkPlayerBulletsVsBoss();
        checkPlayerBulletsVsEnemiesLaser();
        
        checkEnemyBulletsVsPlayer();
        checkEnemiesVsPlayer();
        checkMeteorsVsPlayer();
        checkBossMinionsVsPlayer();
        checkPowerUpsVsPlayer();
        checkBossLaserVsPlayer();
        checkHomingBulletsVsPlayer();
        checkPlasmaBulletsVsPlayer();
    }
    
    private void checkPlayerBulletsVsEnemies() {
        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (b.x > e.x && b.x < e.x + e.width && b.y > e.y && b.y < e.y + e.height) {
                    playerBullets.remove(i);
                    e.health -= 25;
                    spawnParticles(b.x, b.y, 5, Color.YELLOW);
                    if (e.health <= 0) { handleEnemyDeath(e, j); }
                    break;
                }
            }
        }
    }
    
    private void checkPlayerBulletsVsEnemiesLaser() {
        for (int i = lasers.size() - 1; i >= 0; i--) {
            LaserBeam laser = lasers.get(i);
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (laser.x > e.x && laser.x < e.x + e.width && laser.y > e.y && laser.y < e.y + e.height) {
                    e.health -= 50;
                    spawnParticles(laser.x, laser.y, 5, Color.CYAN);
                    if (e.health <= 0) { handleEnemyDeath(e, j); }
                }
            }
        }
    }
    
    private void checkPlayerBulletsVsMeteors() {
        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            for (int j = meteors.size() - 1; j >= 0; j--) {
                Meteor m = meteors.get(j);
                if (b.x > m.x - m.size && b.x < m.x + m.size && b.y > m.y - m.size && b.y < m.y + m.size) {
                    playerBullets.remove(i);
                    m.health--;
                    m.setOnFire(500);
                    spawnParticles(b.x, b.y, 5, Color.rgb(255, 165, 0));
                    if (m.health <= 0) { score += 50 * comboMultiplier; spawnExplosion(m.x, m.y, Color.rgb(150, 100, 50)); meteors.remove(j); }
                    break;
                }
            }
        }
        for (int i = lasers.size() - 1; i >= 0; i--) {
            LaserBeam laser = lasers.get(i);
            for (int j = meteors.size() - 1; j >= 0; j--) {
                Meteor m = meteors.get(j);
                if (laser.x > m.x - m.size && laser.x < m.x + m.size && laser.y > m.y - m.size && laser.y < m.y + m.size) {
                    m.health -= 5;
                    m.setOnFire(300);
                    if (m.health <= 0) { score += 50 * comboMultiplier; spawnExplosion(m.x, m.y, Color.rgb(150, 100, 50)); meteors.remove(j); }
                }
            }
        }
    }
    
    private void checkPlayerBulletsVsBossMinions() {
        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            for (int j = bossMinions.size() - 1; j >= 0; j--) {
                BossMinion m = bossMinions.get(j);
                if (b.x > m.x && b.x < m.x + m.width && b.y > m.y && b.y < m.y + m.height) {
                    playerBullets.remove(i);
                    m.health -= 25;
                    spawnParticles(b.x, b.y, 5, Color.MAGENTA);
                    if (m.health <= 0) { score += 200 * comboMultiplier; spawnExplosion(m.x + m.width / 2, m.y + m.height / 2, Color.rgb(200, 100, 200)); bossMinions.remove(j); }
                    break;
                }
            }
        }
    }
    
    private void checkPlayerBulletsVsBoss() {
        if (boss == null) return;
        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            if (b.x > boss.x && b.x < boss.x + boss.width && b.y > boss.y && b.y < boss.y + boss.height) {
                playerBullets.remove(i);
                boss.health -= 15;
                spawnParticles(b.x, b.y, 3, Color.YELLOW);
                if (boss.health <= 0) { score += 5000 * comboMultiplier; bossDefeated(); }
            }
        }
        for (int i = lasers.size() - 1; i >= 0; i--) {
            LaserBeam laser = lasers.get(i);
            if (laser.x > boss.x && laser.x < boss.x + boss.width && laser.y > boss.y && laser.y < boss.y + boss.height) {
                boss.health -= 30;
                spawnParticles(laser.x, laser.y, 5, Color.CYAN);
                if (boss.health <= 0) { score += 5000 * comboMultiplier; bossDefeated(); }
            }
        }
    }
    
    private void handleEnemyDeath(Enemy e, int index) {
        score += e.scoreValue * comboMultiplier;
        enemiesKilled++;
        totalKills++;
        spawnExplosion(e.x + e.width / 2, e.y + e.height / 2, e.explosionColor);
        if (random.nextInt(100) < 30) powerUps.add(new PowerUp(e.x, e.y, random.nextInt(7)));
        enemies.remove(index);
        triggerCombo();
        triggerScreenShake(5, 100);
        checkAchievements();
    }
    
    private void checkEnemyBulletsVsPlayer() {
        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            if (b.x > player.x && b.x < player.x + player.width && b.y > player.y && b.y < player.y + player.height) {
                enemyBullets.remove(i);
                if (player.hasShield) { player.absorbHit(); }
                else if (invincibleTime <= 0) { player.health -= 15; player.hitTimer = 200; triggerDamageEffects(10, 200, Color.RED, 100); }
            }
        }
    }
    
    private void checkEnemiesVsPlayer() {
        RectF playerRect = player.getBounds();
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (playerRect.intersect(e.getBounds())) {
                enemies.remove(i);
                spawnExplosion(e.x + e.width / 2, e.y + e.height / 2, Color.MAGENTA);
                if (player.hasShield) { player.absorbHit(); }
                else if (invincibleTime <= 0) { player.health -= 20; player.hitTimer = 300; triggerDamageEffects(15, 300, Color.RED, 150); }
            }
        }
    }
    
    private void checkMeteorsVsPlayer() {
        RectF playerRect = player.getBounds();
        for (int i = meteors.size() - 1; i >= 0; i--) {
            Meteor m = meteors.get(i);
            if (playerRect.intersect(m.getBounds())) {
                spawnExplosion(m.x, m.y, Color.rgb(200, 150, 100));
                meteors.remove(i);
                if (player.hasShield) { player.absorbHit(); }
                else if (invincibleTime <= 0) { player.health -= 30; player.hitTimer = 400; triggerDamageEffects(20, 400, Color.rgb(255, 165, 0), 200); }
            }
        }
    }
    
    private void checkBossMinionsVsPlayer() {
        RectF playerRect = player.getBounds();
        for (int i = bossMinions.size() - 1; i >= 0; i--) {
            BossMinion m = bossMinions.get(i);
            if (playerRect.intersect(m.getBounds())) {
                bossMinions.remove(i);
                spawnExplosion(m.x + m.width / 2, m.y + m.height / 2, Color.MAGENTA);
                if (player.hasShield) { player.absorbHit(); }
                else if (invincibleTime <= 0) { player.health -= 15; player.hitTimer = 200; triggerDamageEffects(10, 200, Color.RED, 150); }
            }
        }
    }
    
    private void checkPowerUpsVsPlayer() {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp p = powerUps.get(i);
            if (p.x > player.x && p.x < player.x + player.width && p.y > player.y && p.y < player.y + player.height) {
                applyPowerUp(p.type);
                waveMessage = getPowerUpName(p.type) + "!"; waveAnnounceTime = 1500;
                powerUps.remove(i);
            }
        }
    }
    
    private void checkBossLaserVsPlayer() {
        if (boss == null || !boss.laserActive) return;
        float laserLeft = boss.x + boss.width * 0.3f;
        float laserRight = boss.x + boss.width * 0.7f;
        float laserX = player.x + player.width / 2;
        float laserY = player.y + player.height / 2;
        if (laserX > laserLeft && laserX < laserRight && laserY > boss.y) {
            if (player.hasShield) { player.absorbHit(); }
            else if (invincibleTime <= 0) { player.health -= 30; player.hitTimer = 200; triggerDamageEffects(15, 300, Color.MAGENTA, 150); }
        }
    }
    
    private void checkHomingBulletsVsPlayer() {
        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            if (b.x > player.x && b.x < player.x + player.width && b.y > player.y && b.y < player.y + player.height) {
                enemyBullets.remove(i);
                if (player.hasShield) { player.absorbHit(); }
                else if (invincibleTime <= 0) { player.health -= 10; player.hitTimer = 200; triggerDamageEffects(8, 150, Color.CYAN, 100); }
            }
        }
    }
    
    private void checkPlasmaBulletsVsPlayer() {
        for (int i = plasmaBullets.size() - 1; i >= 0; i--) {
            PlasmaBullet pb = plasmaBullets.get(i);
            if (pb.isPlayerBullet) continue;
            float dx = pb.getX() - (player.x + player.width / 2);
            float dy = pb.getY() - (player.y + player.height / 2);
            if (Math.sqrt(dx * dx + dy * dy) < pb.getRadius() + player.width / 2) {
                plasmaBullets.remove(i);
                if (player.hasShield) { player.absorbHit(); }
                else if (invincibleTime <= 0) { player.health -= 25; player.hitTimer = 300; triggerDamageEffects(15, 250, Color.RED, 150); }
            }
        }
    }
    
    private void triggerDamageEffects(int damage, long shakeDur, int flashColor, int flashDur) {
        triggerScreenShake(damage, shakeDur);
        triggerScreenFlash(flashColor, flashDur);
        invincibleTime = 500;
        if (player.health <= 0) { 
            lives--;
            if (lives <= 0) { player.alive = false; gameOver(); }
            else { player.health = 100; invincibleTime = 2000; waveMessage = lives + " LIVES LEFT!"; waveAnnounceTime = 2000; }
        }
    }
    
    private void checkAchievements() {
        if (totalKills >= 1) unlockAchievement("First Blood");
        if (totalKills >= 10) unlockAchievement("Getting Started");
        if (totalKills >= 50) unlockAchievement("Massacre");
        if (totalKills >= 100) unlockAchievement("Century");
        if (score >= 1000) unlockAchievement("Scoring");
        if (score >= 10000) unlockAchievement("High Scorer");
        if (level >= 5) unlockAchievement("Veteran");
        if (level >= 10) unlockAchievement("Master");
        if (comboMultiplier >= 4) unlockAchievement("Combo King");
        if (comboMultiplier >= 8) unlockAchievement("Combo Legend");
        if (bossActive && boss != null && boss.phase >= 3) unlockAchievement("Survivor");
        if (nukes.size() > 0) unlockAchievement("Nuke It!");
        if (lives == 3 && totalKills >= 20) unlockAchievement("Perfect Start");
    }
        
    private void render(Canvas canvas) {
        int shakeX = 0, shakeY = 0;
        if (screenShakeTime > 0) { shakeX = (int)((random.nextFloat() - 0.5f) * screenShakeIntensity * 2); shakeY = (int)((random.nextFloat() - 0.5f) * screenShakeIntensity * 2); }
        canvas.save(); canvas.translate(shakeX, shakeY);
        canvas.drawColor(Color.BLACK);
        for (Star s : stars) { paint.setColor(Color.WHITE); paint.setAlpha((int)(255 * s.brightness)); canvas.drawRect(s.x, s.y, s.x + s.size, s.y + s.size, paint); }
        if (gameState == STATE_MENU) renderMenu(canvas);
        else if (gameState == STATE_DIFFICULTY) renderDifficulty(canvas);
        else if (gameState == STATE_PLAYING || gameState == STATE_BOSS) renderGame(canvas);
        else if (gameState == STATE_GAME_OVER) renderGameOver(canvas);
        else if (gameState == STATE_LEVEL_COMPLETE) renderLevelComplete(canvas);
        else if (gameState == STATE_VICTORY) renderVictory(canvas);
        else if (gameState == STATE_PAUSED) renderPaused(canvas);
        if (screenFlashTime > 0) { paint.setColor(screenFlashColor); paint.setAlpha(screenFlashTime / 5); canvas.drawRect(0, 0, screenWidth, screenHeight, paint); }
        canvas.restore();
    }
        
    private void renderMenu(Canvas canvas) {
        paint.setTextSize(80); paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SPACE SHOOTER", screenWidth / 2, screenHeight / 4, paint);
        paint.setTextSize(35); canvas.drawText("Tap CENTER to Start", screenWidth / 2, screenHeight / 2 - 50, paint);
        canvas.drawText("Tap TOP for Difficulty", screenWidth / 2, screenHeight / 2 + 10, paint);
        canvas.drawText("Double-tap CORNERS for Nuke/Dash", screenWidth / 2, screenHeight / 2 + 70, paint);
        paint.setTextSize(30); paint.setColor(Color.YELLOW);
        canvas.drawText("High Score: " + highScore, screenWidth / 2, screenHeight * 2 / 3, paint);
        canvas.drawText("Total Kills: " + totalKills, screenWidth / 2, screenHeight * 2 / 3 + 40, paint);
    }
    
    private void renderDifficulty(Canvas canvas) {
        paint.setTextSize(60); paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SELECT DIFFICULTY", screenWidth / 2, screenHeight / 4, paint);
        
        String[] diffNames = {"EASY", "NORMAL", "HARD", "EXTREME"};
        int[] diffColors = {Color.GREEN, Color.YELLOW, Color.rgb(255, 165, 0), Color.RED};
        
        for (int i = 0; i < 4; i++) {
            paint.setColor(diffColors[i]);
            paint.setTextSize(45);
            String label = diffNames[i];
            if (difficulty == i + 1) label += " <--";
            canvas.drawText(label, screenWidth / 2, screenHeight / 2 + i * 70, paint);
        }
        
        paint.setColor(Color.GRAY); paint.setTextSize(30);
        canvas.drawText("Tap to select, then tap BACK", screenWidth / 2, screenHeight * 3 / 4, paint);
    }
        
    private void renderPaused(Canvas canvas) {
        renderGame(canvas);
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setTextSize(80); paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAUSED", screenWidth / 2, screenHeight / 2, paint);
        paint.setTextSize(35);
        canvas.drawText("Tap to Resume", screenWidth / 2, screenHeight / 2 + 80, paint);
    }
        
    private void renderGame(Canvas canvas) {
        for (Particle p : particles) { paint.setColor(p.color); paint.setAlpha((int)(255 * p.life / p.maxLife)); canvas.drawCircle(p.x, p.y, p.size, paint); }
        for (Meteor m : meteors) { m.draw(canvas, paint); }
        for (Nuke n : nukes) { n.draw(canvas, paint); }
        for (PowerUp p : powerUps) { paint.setColor(p.getColor()); canvas.drawCircle(p.x, p.y, 20, paint); }
        for (Bullet b : playerBullets) { paint.setColor(Color.GREEN); canvas.drawRect(b.x - 3, b.y - 10, b.x + 3, b.y + 10, paint); }
        for (HomingBullet hb : homingBullets) { hb.draw(canvas, paint); }
        for (PlasmaBullet pb : plasmaBullets) { pb.draw(canvas, paint); }
        for (Bullet b : enemyBullets) { paint.setColor(Color.RED); canvas.drawCircle(b.x, b.y, 8, paint); }
        for (LaserBeam l : lasers) { paint.setColor(Color.CYAN); paint.setAlpha(200); canvas.drawRect(l.x - 5, 0, l.x + 5, l.y, paint); paint.setAlpha(255); }
        for (Enemy e : enemies) { e.draw(canvas, paint); }
        for (BossMinion m : bossMinions) { m.draw(canvas, paint); }
        if (bossActive && boss != null) {
            boss.draw(canvas, paint, this);
            if (boss.laserActive) {
                paint.setColor(Color.MAGENTA);
                paint.setAlpha(100);
                canvas.drawRect(boss.x + boss.width * 0.3f, boss.y, boss.x + boss.width * 0.7f, boss.laserY, paint);
                paint.setAlpha(255);
            }
            if (boss.enraged) {
                paint.setColor(Color.RED);
                paint.setAlpha((int)(100 + Math.sin(System.currentTimeMillis() * 0.01) * 50));
                canvas.drawRect(0, 0, screenWidth, 5, paint);
                canvas.drawRect(0, screenHeight - 5, screenWidth, screenHeight, paint);
                canvas.drawRect(0, 0, 5, screenHeight, paint);
                canvas.drawRect(screenWidth - 5, 0, screenWidth, screenHeight, paint);
                paint.setAlpha(255);
            }
        }
        if (player != null && player.alive) player.draw(canvas, paint);
        if (waveAnnounceTime > 0) { paint.setTextSize(60); paint.setColor(Color.YELLOW); paint.setTextAlign(Paint.Align.CENTER); canvas.drawText(waveMessage, screenWidth / 2, screenHeight / 2, paint); waveAnnounceTime -= 16; }
        if (achievementNotifyTime > 0) { paint.setTextSize(35); paint.setColor(Color.rgb(255, 215, 0)); paint.setTextAlign(Paint.Align.CENTER); canvas.drawText(achievementMessage, screenWidth / 2, screenHeight / 3, paint); }
        
        paint.setTextSize(25); paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Lives: " + lives, 20, 35, paint);
        canvas.drawText("Score: " + score + (comboMultiplier > 1 ? " x" + comboMultiplier : ""), 20, 70, paint);
        canvas.drawText("Level: " + level, 20, 105, paint);
        canvas.drawText("Kills: " + enemiesKilled + "/" + (12 * level), 20, 140, paint);
        
        if (player != null) {
            paint.setColor(Color.RED); canvas.drawRect((float)20, (float)155, (float)220, (float)185, paint);
            paint.setColor(Color.GREEN); canvas.drawRect((float)20, (float)155, (float)(20 + (float)player.health / 100 * 200), (float)185, paint);
            if (player.hasShield) {
                paint.setColor(Color.CYAN); paint.setTextSize(20);
                canvas.drawText("Shield: " + player.shieldHits + " hits", 20, 210, paint);
            }
            if (player.dashCooldown > 0) {
                paint.setColor(Color.GRAY); paint.setTextSize(18);
                canvas.drawText("Dash: " + (player.dashCooldown / 1000) + "s", 20, 235, paint);
            } else {
                paint.setColor(Color.CYAN); paint.setTextSize(18);
                canvas.drawText("Dash: READY", 20, 235, paint);
            }
        }
        
        if (bossActive && boss != null) {
            paint.setColor(Color.RED); canvas.drawRect((float)(screenWidth / 2 - 150), (float)30, (float)(screenWidth / 2 + 150), (float)50, paint);
            paint.setColor(boss.enraged ? Color.RED : Color.YELLOW);
            canvas.drawRect((float)(screenWidth / 2 - 150), (float)30, (float)(screenWidth / 2 - 150 + (float)boss.health / boss.maxHealth * 300), (float)50, paint);
            paint.setColor(Color.WHITE); paint.setTextSize(20);
            canvas.drawText("BOSS P" + boss.phase + (boss.enraged ? " ENRAGED!" : ""), screenWidth / 2, 45, paint);
        }
        
        if (comboMultiplier > 1) { paint.setColor(Color.MAGENTA); paint.setTextSize(25); canvas.drawText(comboMultiplier + "x COMBO!", screenWidth - 150, 50, paint); }
        if (hasLaser) { long remaining = 5000 - (System.currentTimeMillis() - laserStartTime); if (remaining > 0) { paint.setColor(Color.CYAN); paint.setTextSize(25); canvas.drawText("LASER: " + (remaining / 1000) + "s", screenWidth - 150, 90, paint); } }
        if (nukeReady) { paint.setColor(Color.rgb(255, 150, 50)); paint.setTextSize(20); canvas.drawText("NUKE!", screenWidth - 80, screenHeight - 50, paint); }
        else if (nukeCooldown > 0) { paint.setColor(Color.GRAY); paint.setTextSize(18); canvas.drawText("Nuke: " + (nukeCooldown / 1000) + "s", screenWidth - 100, screenHeight - 50, paint); }
    }
        
    private void renderGameOver(Canvas canvas) {
        paint.setTextSize(80); paint.setColor(Color.RED); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", screenWidth / 2, screenHeight / 3, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(40);
        canvas.drawText("Score: " + score, screenWidth / 2, screenHeight / 2, paint);
        canvas.drawText("High Score: " + highScore, screenWidth / 2, screenHeight / 2 + 60, paint);
        canvas.drawText("Kills: " + totalKills + " | Level: " + level, screenWidth / 2, screenHeight / 2 + 100, paint);
        canvas.drawText("Tap to Restart", screenWidth / 2, screenHeight / 2 + 180, paint);
    }
        
    private void renderLevelComplete(Canvas canvas) {
        paint.setTextSize(80); paint.setColor(Color.GREEN); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LEVEL " + level + " COMPLETE!", screenWidth / 2, screenHeight / 3, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(40);
        canvas.drawText("Score: " + score, screenWidth / 2, screenHeight / 2, paint);
        canvas.drawText("Tap to Continue", screenWidth / 2, screenHeight / 2 + 100, paint);
    }
        
    private void renderVictory(Canvas canvas) {
        paint.setTextSize(80); paint.setColor(Color.rgb(255, 215, 0)); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("VICTORY!", screenWidth / 2, screenHeight / 3, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(40);
        canvas.drawText("Final Score: " + score, screenWidth / 2, screenHeight / 2, paint);
        canvas.drawText("High Score: " + highScore, screenWidth / 2, screenHeight / 2 + 60, paint);
        canvas.drawText("Total Kills: " + totalKills, screenWidth / 2, screenHeight / 2 + 100, paint);
        canvas.drawText("Tap to Play Again", screenWidth / 2, screenHeight / 2 + 180, paint);
    }
        
    private void shoot() {
        if (player == null) return;
        int weapon = player.currentWeapon;
        switch (weapon) {
            case 0:
                playerBullets.add(new Bullet(player.x + player.width / 2, player.y - 10, 0, -25, true));
                break;
            case 1:
                int count = Math.min(player.spreadShot, 5);
                float spacing = 20;
                for (int i = 0; i < count; i++) {
                    float offsetX = (i - (count - 1) / 2f) * spacing;
                    playerBullets.add(new Bullet(player.x + player.width / 2 + offsetX, player.y - 10, 0, -25, true));
                }
                break;
            case 2:
                if (hasLaser) {
                    lasers.add(new LaserBeam(player.x + player.width / 2, player.y + player.height, screenHeight));
                }
                break;
            case 3:
                homingBullets.add(new HomingBullet(player.x + player.width / 2, player.y, 0, -12, true));
                break;
            case 4:
                plasmaBullets.add(new PlasmaBullet(player.x + player.width / 2, player.y, 0, -8, true));
                break;
        }
    }
        
    private void enemyShoot(Enemy e) {
        enemyBullets.add(new Bullet(e.x + e.width / 2, e.y + e.height, 0, 10, false));
    }
        
    private void bossShoot() {
        if (boss == null) return;
        int spread = 3 + boss.phase * 2;
        if (boss.enraged) spread += 4;
        for (int i = 0; i < spread; i++) {
            float angle = (float)Math.PI / 2 + (i - (spread - 1) / 2f) * 0.3f;
            float speed = 6 + boss.phase;
            float vx = (float)Math.cos(angle) * speed;
            float vy = (float)Math.sin(angle) * speed;
            enemyBullets.add(new Bullet(boss.x + boss.width / 2, boss.y + boss.height, vx, vy, false));
        }
    }
        
    private void bossBomb() {
        if (boss == null) return;
        int bullets = 12 + boss.phase * 4;
        if (boss.enraged) bullets += 8;
        for (int i = 0; i < bullets; i++) {
            float angle = (float)(Math.PI * 2 * i / bullets);
            float vx = (float)Math.cos(angle) * (4 + boss.phase);
            float vy = (float)Math.sin(angle) * (4 + boss.phase);
            enemyBullets.add(new Bullet(boss.x + boss.width / 2, boss.y + boss.height / 2, vx, vy, false));
        }
    }
        
    private void spawnBoss() {
        boss = new Boss(screenWidth / 2 - 75, -200, level, screenWidth);
        bossActive = true;
        waveMessage = "BOSS INCOMING!"; waveAnnounceTime = 2000;
        triggerScreenShake(20, 500);
        triggerScreenFlash(Color.WHITE, 300);
    }
        
    private void bossDefeated() {
        spawnExplosion(boss.x + boss.width / 2, boss.y + boss.height / 2, Color.YELLOW);
        spawnExplosion(boss.x + boss.width / 4, boss.y + boss.height / 2, Color.MAGENTA);
        spawnExplosion(boss.x + boss.width * 3 / 4, boss.y + boss.height / 2, Color.CYAN);
        triggerScreenShake(30, 1000);
        triggerScreenFlash(Color.WHITE, 500);
        for (BossMinion m : new ArrayList<>(bossMinions)) {
            spawnExplosion(m.x + m.width / 2, m.y + m.height / 2, Color.MAGENTA);
        }
        bossMinions.clear();
        boss = null; bossActive = false;
        if (level >= 10) { gameState = STATE_VICTORY; saveHighScore(); unlockAchievement("Champion"); }
        else { level++; waveMessage = "LEVEL " + level + "!"; waveAnnounceTime = 2000; }
    }
        
    private void gameOver() {
        gameState = STATE_GAME_OVER; saveHighScore();
        spawnExplosion(player.x + player.width / 2, player.y + player.height / 2, Color.BLUE);
        triggerScreenFlash(Color.RED, 500);
    }
        
    private void applyPowerUp(int type) {
        if (player == null) return;
        switch (type) {
            case 0: player.health = Math.min(100, player.health + 30); break;
            case 1: player.spreadShot = Math.min(5, player.spreadShot + 1); break;
            case 2: player.hasShield = true; player.shield = 5000; player.shieldHits = 3; break;
            case 3: for (Enemy e : new ArrayList<>(enemies)) { spawnExplosion(e.x + e.width / 2, e.y + e.height / 2, Color.RED); enemies.remove(e); score += e.scoreValue; enemiesKilled++; totalKills++; } enemyBullets.clear(); triggerScreenShake(15, 500); triggerScreenFlash(Color.RED, 200); break;
            case 4: fireRate = Math.max(50, fireRate - 50); break;
            case 5: hasLaser = true; laserStartTime = System.currentTimeMillis(); lasers.clear(); break;
            case 6: nukeReady = true; nukeCooldown = 0; break;
        }
    }
        
    private String getPowerUpName(int type) {
        String[] names = {"HEALTH+", "MULTI-SHOT", "SHIELD x3", "BOMB", "RAPID FIRE", "LASER", "NUKE"};
        return names[type];
    }
        
    void spawnParticles(float x, float y, int count, int color) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float speed = 2 + random.nextFloat() * 5;
            particles.add(new Particle(x, y, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, color, 300 + random.nextInt(200)));
        }
    }
    
    void spawnExplosion(float x, float y, int color) {
        for (int i = 0; i < 25; i++) {
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float speed = 3 + random.nextFloat() * 8;
            float size = 3 + random.nextFloat() * 5;
            Particle p = new Particle(x, y, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, color, 400 + random.nextInt(300));
            p.size = size;
            particles.add(p);
        }
    }
    
    private void triggerCombo() {
        long now = System.currentTimeMillis();
        if (now - lastKillTime < 2000) { comboCount++; comboMultiplier = Math.min(8, 1 + comboCount / 2); }
        else { comboCount = 0; comboMultiplier = 1; }
        lastKillTime = now;
        if (comboMultiplier > 1) { waveMessage = comboMultiplier + "x COMBO!"; waveAnnounceTime = 1000; }
    }
        
    void triggerScreenShake(float intensity, long duration) { screenShakeIntensity = Math.max(screenShakeIntensity, intensity); if (screenShakeTime < duration) screenShakeTime = duration; }
    void triggerScreenFlash(int color, long duration) { screenFlashColor = color; screenFlashTime = (int)duration; }
        
    private void saveHighScore() { 
        if (score > highScore) { highScore = score; prefs.edit().putInt("high_score", highScore).apply(); }
        prefs.edit().putInt("total_kills", totalKills).apply();
    }
    
    private void launchNuke() {
        if (nukeReady && player != null) {
            float targetX = screenWidth / 2 + (random.nextFloat() - 0.5f) * screenWidth * 0.5f;
            float targetY = screenHeight * 0.3f + random.nextFloat() * screenHeight * 0.3f;
            nukes.add(new Nuke(player.x + player.width / 2, player.y, targetX, targetY));
            nukeReady = false;
        }
    }
    
    private void selectDifficulty(int diff) {
        difficulty = diff;
        gameState = STATE_MENU;
    }
        
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchX = (int)event.getX(); touchY = (int)event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) touchPressed = true;
        else touchPressed = false;
        
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int)event.getX();
            int y = (int)event.getY();
            
            if (gameState == STATE_MENU) {
                if (y < 150) {
                    gameState = STATE_DIFFICULTY;
                    return true;
                }
                if (y > screenHeight / 2 - 100 && y < screenHeight / 2 + 100) {
                    handleTap(x, y);
                    return true;
                }
            } else if (gameState == STATE_DIFFICULTY) {
                if (y > screenHeight / 2 && y < screenHeight / 2 + 280) {
                    int diff = (y - (int)(screenHeight / 2)) / 70;
                    if (diff >= 0 && diff < 4) selectDifficulty(diff + 1);
                }
                return true;
            }
            
            if (player != null) player.checkDoubleTap(x, y);
            
            if (x > screenWidth - 200 && y < 200 && nukeReady) {
                long now = System.currentTimeMillis();
                if (now - lastNukeTap < 500) {
                    launchNuke();
                }
                lastNukeTap = now;
            }
            
            handleTap(x, y);
        }
        return true;
    }
        
    private void handleTap(int x, int y) {
        if (gameState == STATE_PAUSED) { paused = false; gameState = STATE_PLAYING; }
        else if (gameState == STATE_MENU) { gameState = STATE_PLAYING; resetGame(); }
        else if (gameState == STATE_GAME_OVER || gameState == STATE_VICTORY) { gameState = STATE_PLAYING; resetGame(); }
        else if (gameState == STATE_LEVEL_COMPLETE) { gameState = STATE_PLAYING; }
    }
        
    private void resetGame() {
        player = new PlayerShip(screenWidth / 2 - 30, screenHeight - 200, screenWidth, screenHeight, this);
        playerBullets.clear(); enemyBullets.clear(); enemies.clear(); powerUps.clear(); particles.clear(); lasers.clear();
        meteors.clear(); nukes.clear(); bossMinions.clear(); homingBullets.clear(); plasmaBullets.clear();
        score = 0; level = 1; enemiesKilled = 0; bossActive = false; boss = null; comboCount = 0; comboMultiplier = 1; hasLaser = false; fireRate = 200; player.spreadShot = 1; player.hasShield = false;
        enemiesKilled = 0; lastEnemySpawn = System.currentTimeMillis(); lastPowerUpSpawn = System.currentTimeMillis(); lastMeteorSpawn = System.currentTimeMillis();
        formationCount = 0; nukeReady = true; nukeCooldown = 0; lives = 3; invincibleTime = 0; paused = false;
    }
}
