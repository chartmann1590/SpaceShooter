package com.spaceshooter;

import android.graphics.*;

public class PlasmaBullet {
    float x, y;
    float vx, vy;
    float radius = 15;
    float lifetime = 2000;
    float age = 0;
    boolean isPlayerBullet;
    float pulse = 0;
    
    PlasmaBullet(float x, float y, float vx, float vy, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.isPlayerBullet = isPlayer;
    }
    
    void update(long dt) {
        age += dt;
        pulse += 0.2f;
        x += vx * dt * 0.05f;
        y += vy * dt * 0.05f;
    }
    
    void draw(Canvas canvas, Paint paint) {
        float currentRadius = radius + (float)Math.sin(pulse) * 3;
        
        int coreColor = isPlayerBullet ? Color.CYAN : Color.RED;
        int midColor = isPlayerBullet ? Color.rgb(0, 150, 200) : Color.rgb(200, 50, 50);
        int outerColor = isPlayerBullet ? Color.rgb(0, 80, 100) : Color.rgb(100, 20, 20);
        
        RadialGradient gradient = new RadialGradient(x, y, currentRadius, 
            new int[]{coreColor, midColor, outerColor, Color.TRANSPARENT}, 
            new float[]{0f, 0.3f, 0.7f, 1f}, 
            Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawCircle(x, y, currentRadius, paint);
        paint.setShader(null);
        
        paint.setColor(Color.WHITE);
        paint.setAlpha(150);
        canvas.drawCircle(x, y, currentRadius * 0.3f, paint);
        paint.setAlpha(255);
    }
    
    float getRadius() { return radius; }
    float getX() { return x; }
    float getY() { return y; }
    boolean isExpired() { return age > lifetime || y < -50 || y > 2000; }
}
