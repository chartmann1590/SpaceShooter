package com.spaceshooter;

import android.graphics.*;
import android.graphics.RectF;

public class PlayerShip {
    float x, y;
    int width = 60, height = 70;
    int health = 100;
    int maxHealth = 100;
    int shield = 0;
    int shieldHits = 0;
    int maxShieldHits = 3;
    int bombCount = 2;
    int spreadShot = 1;
    float fireRateMultiplier = 1;
    boolean alive = true;
    int invincible = 0;
    float engineFlicker = 0;
    boolean hasShield = false;
    long hitTimer = 0;
    int screenWidth, screenHeight;
    GameSurfaceView game;
    
    float dashCooldown = 0;
    float dashSpeed = 0;
    float dashDirectionX = 0;
    float dashDirectionY = 0;
    float dashTimer = 0;
    long lastTapX = 0;
    long lastTapY = 0;
    long lastTapTime = 0;
    int lastTouchX = 0;
    int lastTouchY = 0;
    
    int currentWeapon = 0;
    long weaponCooldown = 0;
    long homingTimer = 0;
    
    String[] weapons = {"NORMAL", "SPREAD", "LASER", "HOMING", "PLASMA"};
    int[] weaponFireRates = {200, 300, 100, 500, 400};
    
    PlayerShip(float x, float y, int screenW, int screenH, GameSurfaceView game) {
        this.x = x;
        this.y = y;
        this.screenWidth = screenW;
        this.screenHeight = screenH;
        this.game = game;
    }

    RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }

    void update(long deltaTime, int touchX, int touchY, boolean touchPressed) {
        if (dashTimer > 0) {
            dashTimer -= deltaTime;
            x += dashDirectionX * dashSpeed * deltaTime * 0.1f;
            y += dashDirectionY * dashSpeed * deltaTime * 0.1f;
        } else if (touchPressed) {
            float dx = touchX - (x + width / 2);
            float dy = touchY - (y + height / 2);
            x += dx * 0.15f;
            y += dy * 0.15f;
        }
        
        if (dashCooldown > 0) dashCooldown -= deltaTime;
        
        x = Math.max(0, Math.min(screenWidth - width, x));
        y = Math.max(100, Math.min(screenHeight - height - 50, y));
        
        if (invincible > 0) invincible -= deltaTime;
        if (shield > 0) { shield -= deltaTime; hasShield = shield > 0; }
        if (hitTimer > 0) hitTimer -= deltaTime;
        if (weaponCooldown > 0) weaponCooldown -= deltaTime;
        if (homingTimer > 0) homingTimer -= deltaTime;
        
        engineFlicker = (engineFlicker + 0.3f) % 6.28f;
        
        if (Math.random() < 0.3 && game != null) {
            game.particles.add(new Particle(x + width / 2 + (float)(Math.random() * 10 - 5), y + height,
                (float)(Math.random() * 2 - 1), 3 + (float)(Math.random() * 3),
                Color.argb(255, 255, 100 + (int)(50 * Math.sin(engineFlicker)), 0), 300));
        }
    }
    
    void checkDoubleTap(int tapX, int tapY) {
        long now = System.currentTimeMillis();
        if (now - lastTapTime < 300) {
            if (Math.abs(tapX - lastTapX) < 100 && Math.abs(tapY - lastTapY) < 100) {
                performDash();
            }
        }
        lastTapX = tapX;
        lastTapY = tapY;
        lastTapTime = now;
        lastTouchX = tapX;
        lastTouchY = tapY;
    }
    
    void performDash() {
        if (dashCooldown > 0) return;
        
        float dx = lastTouchX - (x + width / 2);
        float dy = lastTouchY - (y + height / 2);
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            dashDirectionX = dx / dist;
            dashDirectionY = dy / dist;
            dashSpeed = 30;
            dashTimer = 200;
            dashCooldown = 1000;
            
            if (game != null) {
                for (int i = 0; i < 10; i++) {
                    game.particles.add(new Particle(x + width / 2, y + height / 2,
                        -dashDirectionX * (2 + random().nextFloat() * 3),
                        -dashDirectionY * (2 + random().nextFloat() * 3),
                        Color.CYAN, 200));
                }
            }
        }
    }
    
    void cycleWeapon() {
        currentWeapon = (currentWeapon + 1) % weapons.length;
    }
    
    int getCurrentFireRate() {
        return weaponFireRates[currentWeapon];
    }

    void takeDamage(int amount) {
        if (invincible > 0 || hasShield) return;
        health -= amount;
        invincible = 500;
        hitTimer = 500;
        if (health <= 0) {
            alive = false;
            if (game != null) game.spawnParticles(x + width / 2, y + height / 2, 30, Color.BLUE);
        }
    }
    
    void absorbHit() {
        shieldHits--;
        if (shieldHits <= 0) {
            hasShield = false;
            shieldHits = 0;
        }
        if (game != null) {
            game.spawnParticles(x + width / 2, y + height / 2, 15, Color.CYAN);
            game.triggerScreenShake(5, 100);
        }
    }

    void draw(Canvas canvas, Paint paint) {
        if (!alive) return;
        
        if (invincible > 0 && (System.currentTimeMillis() / 100) % 2 == 0) return;
        
        canvas.save();
        
        if (dashTimer > 0) {
            canvas.scale(1.3f, 0.7f, x + width / 2, y + height / 2);
        } else {
            canvas.scale(1f + (float)Math.sin(engineFlicker) * 0.05f, 1f);
        }
        
        Path ship = new Path();
        ship.moveTo(x + width / 2, y);
        ship.lineTo(x + width, y + height * 0.7f);
        ship.lineTo(x + width * 0.8f, y + height);
        ship.lineTo(x + width * 0.2f, y + height);
        ship.lineTo(x, y + height * 0.7f);
        ship.close();
        
        int[] colors;
        if (dashTimer > 0) {
            colors = new int[]{Color.rgb(100, 255, 255), Color.rgb(50, 200, 200), Color.rgb(0, 150, 150)};
        } else if (hitTimer > 0) {
            colors = new int[]{Color.rgb(255, 100, 100), Color.rgb(200, 50, 50), Color.rgb(150, 30, 30)};
        } else {
            colors = new int[]{Color.rgb(100, 150, 255), Color.rgb(50, 100, 200), Color.rgb(30, 60, 150)};
        }
        LinearGradient gradient = new LinearGradient(x, y, x + width, y + height, colors, null, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawPath(ship, paint);
        paint.setShader(null);
        
        paint.setColor(Color.rgb(200, 220, 255));
        canvas.drawCircle(x + width / 2, y + height * 0.35f, 12, paint);
        canvas.drawCircle(x + width / 2, y + height * 0.35f, 6, paint);
        
        canvas.restore();
        
        if (hasShield) {
            int shieldColor;
            if (shieldHits >= 3) shieldColor = Color.argb(150, 100, 200, 255);
            else if (shieldHits >= 2) shieldColor = Color.argb(150, 100, 150, 255);
            else shieldColor = Color.argb(150, 100, 100, 255);
            
            paint.setColor(shieldColor);
            paint.setStrokeWidth(3 + shieldHits);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(x + width / 2f, y + height / 2f, width + 10, paint);
            
            for (int i = 0; i < shieldHits; i++) {
                float angle = (float)(System.currentTimeMillis() * 0.003 + i * Math.PI * 2 / 3);
                float px = x + width / 2 + (float)Math.cos(angle) * (width + 15);
                float py = y + height / 2 + (float)Math.sin(angle) * (width + 15);
                canvas.drawCircle(px, py, 5, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
        
        if (dashTimer > 0) {
            paint.setColor(Color.CYAN);
            paint.setAlpha(100);
            canvas.drawLine(x - 20, y + height / 2, x + width + 20, y + height / 2, paint);
            paint.setAlpha(255);
        }
    }
    
    private java.util.Random random() {
        return new java.util.Random();
    }
}
