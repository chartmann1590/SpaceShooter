package com.spaceshooter;

import android.graphics.*;

public class Enemy {
    float x, y;
    int width, height;
    int health;
    int maxHealth;
    int damage;
    int scoreValue;
    int color;
    int explosionColor;
    boolean canShoot = false;
    float speed = 2;
    float shootTimer = 1000;
    PatternType pattern = PatternType.STRAIGHT;
    boolean isSplit = false;
    float specialTimer = 0;
    int originalHealth = 0;
    
    enum PatternType { STRAIGHT, ZIGZAG, CIRCLE, DIVING, HEALING }
    
    void update(long deltaTime, PlayerShip player) {
        switch (pattern) {
            case ZIGZAG:
                x += Math.sin(y * 0.02f) * 3;
                break;
            case CIRCLE:
                x += Math.cos(y * 0.01f) * 2;
                break;
            case DIVING:
                if (player != null && player.alive && y > 100 && y < 300) {
                    float dx = player.x + player.width / 2 - (x + width / 2);
                    x += Math.signum(dx) * 4;
                }
                break;
            case HEALING:
                if (health < maxHealth && y > 50) {
                    specialTimer += deltaTime;
                    if (specialTimer > 500) {
                        health = Math.min(maxHealth, health + 2);
                        specialTimer = 0;
                    }
                }
                x += Math.sin(y * 0.015f) * 1.5f;
                break;
        }
        y += speed;
        x = Math.max(0, Math.min(1080 - width, x));
    }

    void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        
        if (pattern == PatternType.HEALING) {
            paint.setColor(Color.rgb(100, 255, 100));
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.01) * 0.2 + 1);
            canvas.save();
            canvas.scale(pulse, pulse, x + width / 2, y + height / 2);
        }
        
        Path enemyShape = new Path();
        enemyShape.moveTo(x + width / 2f, y + height);
        enemyShape.lineTo(x + width, y);
        enemyShape.lineTo(x + width * 0.7f, y + height * 0.3f);
        enemyShape.lineTo(x + width * 0.3f, y + height * 0.3f);
        enemyShape.lineTo(x, y);
        enemyShape.close();
        
        canvas.drawPath(enemyShape, paint);
        
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x + width / 2f, y + height * 0.4f, width * 0.15f, paint);
        
        if (maxHealth > 0) {
            paint.setColor(Color.RED);
            canvas.drawRect(x, y - 8, x + width, y - 4, paint);
            paint.setColor(Color.GREEN);
            canvas.drawRect(x, y - 8, x + (width * health / maxHealth), y - 4, paint);
        }
        
        if (pattern == PatternType.HEALING) canvas.restore();
    }
    
    RectF getBounds() { return new RectF(x, y, x + width, y + height); }
    
    static Enemy create(String type, float x, float y, int level) {
        switch (type) {
            case "scout": return new ScoutEnemy(x, y, level);
            case "fighter": return new FighterEnemy(x, y, level);
            case "tank": return new TankEnemy(x, y, level);
            case "bomber": return new BomberEnemy(x, y, level);
            case "chaser": return new ChaserEnemy(x, y, level);
            case "splitter": return new SplitterEnemy(x, y, level);
            case "healer": return new HealerEnemy(x, y, level);
            default: return new ScoutEnemy(x, y, level);
        }
    }
}

class ScoutEnemy extends Enemy {
    ScoutEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 40; this.height = 40;
        this.health = 30 + level * 5; this.maxHealth = health;
        this.damage = 10; this.scoreValue = 100;
        this.color = Color.rgb(255, 100, 100); this.explosionColor = Color.RED;
        this.speed = 4 + level * 0.3f; this.canShoot = false;
        this.pattern = PatternType.ZIGZAG;
    }
}

class FighterEnemy extends Enemy {
    FighterEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 50; this.height = 50;
        this.health = 50 + level * 10; this.maxHealth = health;
        this.damage = 15; this.scoreValue = 200;
        this.color = Color.rgb(255, 165, 0); this.explosionColor = Color.rgb(255, 140, 0);
        this.speed = 2 + level * 0.2f; this.canShoot = true; this.shootTimer = 500;
        this.pattern = PatternType.STRAIGHT;
    }
}

class TankEnemy extends Enemy {
    TankEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 60; this.height = 60;
        this.health = 100 + level * 20; this.maxHealth = health;
        this.damage = 25; this.scoreValue = 300;
        this.color = Color.rgb(128, 0, 128); this.explosionColor = Color.rgb(200, 0, 200);
        this.speed = 1 + level * 0.1f; this.canShoot = true; this.shootTimer = 800;
        this.pattern = PatternType.STRAIGHT;
    }
}

class BomberEnemy extends Enemy {
    BomberEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 45; this.height = 55;
        this.health = 40 + level * 8; this.maxHealth = health;
        this.damage = 20; this.scoreValue = 250;
        this.color = Color.rgb(100, 100, 100); this.explosionColor = Color.DKGRAY;
        this.speed = 1.5f + level * 0.15f; this.canShoot = true; this.shootTimer = 300;
        this.pattern = PatternType.CIRCLE;
    }
}

class ChaserEnemy extends Enemy {
    ChaserEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 35; this.height = 35;
        this.health = 40 + level * 8; this.maxHealth = health;
        this.damage = 15; this.scoreValue = 250;
        this.color = Color.rgb(255, 50, 255); this.explosionColor = Color.rgb(200, 0, 200);
        this.speed = 2 + level * 0.2f; this.canShoot = true; this.shootTimer = 800;
        this.pattern = PatternType.CIRCLE;
    }
    
    @Override
    void update(long deltaTime, PlayerShip player) {
        if (player != null && player.alive) {
            float dx = player.x + player.width / 2 - (x + width / 2);
            float dy = player.y - y;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);
            if (dist > 0) {
                x += (dx / dist) * speed * 0.5f;
                y += speed * 0.7f;
            }
        }
        x = Math.max(0, Math.min(1080 - width, x));
    }
}

class SplitterEnemy extends Enemy {
    SplitterEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 55; this.height = 55;
        this.health = 80 + level * 15; this.maxHealth = health; this.originalHealth = health;
        this.damage = 20; this.scoreValue = 400;
        this.color = Color.rgb(255, 0, 128); this.explosionColor = Color.rgb(255, 100, 200);
        this.speed = 1.5f + level * 0.1f; this.canShoot = false;
        this.pattern = PatternType.DIVING;
    }
    
    @Override
    void update(long deltaTime, PlayerShip player) {
        super.update(deltaTime, player);
        if (health <= maxHealth / 2 && width == 55) {
            maxHealth = maxHealth / 2;
            width = 35;
            height = 35;
        }
    }
}

class HealerEnemy extends Enemy {
    HealerEnemy(float x, float y, int level) {
        this.x = x; this.y = y; this.width = 45; this.height = 45;
        this.health = 60 + level * 10; this.maxHealth = health;
        this.damage = 10; this.scoreValue = 350;
        this.color = Color.rgb(100, 255, 100); this.explosionColor = Color.GREEN;
        this.speed = 1 + level * 0.1f; this.canShoot = false;
        this.pattern = PatternType.HEALING;
    }
}
