package com.spaceshooter;

import android.graphics.*;

public class HomingBullet {
    float x, y;
    float vx, vy;
    float speed = 12;
    float turnRate = 0.08f;
    float lifetime = 3000;
    float age = 0;
    boolean isPlayerBullet;
    
    HomingBullet(float x, float y, float vx, float vy, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.isPlayerBullet = isPlayer;
    }
    
    void update(long dt, Object target, float targetX, float targetY) {
        age += dt;
        
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            float targetVx = (dx / dist) * speed;
            float targetVy = (dy / dist) * speed;
            
            float cos = (float)Math.cos(turnRate);
            float sin = (float)Math.sin(turnRate);
            
            float newVx = vx * cos + targetVx * sin * 0.1f;
            float newVy = vy * cos + targetVy * sin * 0.1f;
            
            float mag = (float)Math.sqrt(newVx * newVx + newVy * newVy);
            if (mag > 0) {
                vx = (newVx / mag) * speed;
                vy = (newVy / mag) * speed;
            }
        }
        
        x += vx * dt * 0.05f;
        y += vy * dt * 0.05f;
    }
    
    void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isPlayerBullet ? Color.CYAN : Color.rgb(255, 100, 100));
        
        float angle = (float)Math.atan2(vy, vx);
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate((float)Math.toDegrees(angle) + 90);
        
        Path bullet = new Path();
        bullet.moveTo(0, -12);
        bullet.lineTo(5, 8);
        bullet.lineTo(-5, 8);
        bullet.close();
        canvas.drawPath(bullet, paint);
        
        paint.setColor(Color.WHITE);
        canvas.drawCircle(0, 0, 3, paint);
        
        canvas.restore();
    }
    
    boolean isExpired() {
        return age > lifetime || y < -50 || y > 2000 || x < -50 || x > 2000;
    }
    
    float getX() { return x; }
    float getY() { return y; }
}
