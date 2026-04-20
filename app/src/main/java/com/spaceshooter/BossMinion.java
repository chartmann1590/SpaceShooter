package com.spaceshooter;

import android.graphics.*;

public class BossMinion {
    float x, y;
    int width = 30, height = 30;
    int health;
    int maxHealth;
    int color;
    float speed = 3;
    float shootTimer = 0;
    boolean canShoot = true;
    int screenWidth;
    int orbitAngle = 0;
    float orbitRadius = 150;
    float orbitSpeed = 0.03f;
    float baseX, baseY;
    int parentPhase = 1;
    
    BossMinion(float x, float y, int screenWidth, int phase) {
        this.x = x;
        this.y = y;
        this.baseX = x;
        this.baseY = y;
        this.screenWidth = screenWidth;
        this.parentPhase = phase;
        this.health = 30 + phase * 10;
        this.maxHealth = health;
        this.color = Color.rgb(150, 50, 150);
        this.speed = 2 + phase;
    }
    
    void update(long dt, Boss parent) {
        if (parent != null) {
            orbitAngle += orbitSpeed * dt;
            baseX = parent.x + parent.width / 2;
            baseY = parent.y + parent.height / 2;
            
            x = baseX + (float)Math.cos(orbitAngle) * orbitRadius;
            y = baseY + (float)Math.sin(orbitAngle) * orbitRadius * 0.5f;
        } else {
            y += speed;
        }
        
        x = Math.max(0, Math.min(screenWidth - width, x));
        
        if (canShoot) {
            shootTimer -= dt;
            if (shootTimer <= 0) {
                shootTimer = 800 + (int)(Math.random() * 1200);
            }
        }
    }
    
    boolean shouldShoot() {
        return canShoot && shootTimer <= 0;
    }
    
    void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        
        Path minion = new Path();
        minion.moveTo(x + width / 2f, y);
        minion.lineTo(x + width, y + height);
        minion.lineTo(x, y + height);
        minion.close();
        
        canvas.drawPath(minion, paint);
        
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x + width / 2f, y + height * 0.4f, 5, paint);
        
        if (health < maxHealth) {
            paint.setColor(Color.RED);
            canvas.drawRect(x, y - 8, x + width, y - 4, paint);
            paint.setColor(Color.GREEN);
            canvas.drawRect(x, y - 8, x + (width * health / maxHealth), y - 4, paint);
        }
    }
    
    RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}
