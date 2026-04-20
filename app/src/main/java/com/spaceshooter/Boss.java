package com.spaceshooter;

import android.graphics.*;
import java.util.Random;

public class Boss {
    float x, y;
    int width = 200, height = 150;
    int health = 500;
    int maxHealth = 500;
    int phase = 1;
    long shootTimer = 0;
    long bombTimer = 5000;
    long spiralTimer = 0;
    long laserTimer = 0;
    boolean canBomb = true;
    boolean canSpiral = false;
    boolean canLaser = false;
    boolean enraged = false;
    float moveAngle = 0;
    float enginePulse = 0;
    float targetX = 0;
    float moveSpeed = 2;
    Random random = new Random();
    int screenWidth;
    int screenHeight;
    boolean laserActive = false;
    float laserY = 0;
    long laserDuration = 0;

    Boss(float x, float y, int level, int screenWidth) {
        this.x = x; this.y = y; this.screenWidth = screenWidth;
        this.health = 500 + level * 100; this.maxHealth = health;
        this.canBomb = level >= 3;
        this.targetX = x;
    }

    void update(long deltaTime, PlayerShip player) {
        y += 0.5f;
        if (y > 100) y = 100;
        
        if (Math.abs(x - targetX) < 10) {
            targetX = 100 + random.nextInt(screenWidth - 300);
        }
        x += (targetX > x ? moveSpeed : -moveSpeed);
        x = Math.max(0, Math.min(screenWidth - width, x));
        
        if (health < maxHealth * 0.5f) { phase = 2; canSpiral = true; moveSpeed = 3; }
        if (health < maxHealth * 0.25f) { phase = 3; canLaser = true; moveSpeed = 4; }
        
        enginePulse = (enginePulse + 0.1f) % 6.28f;
        
        if (laserActive) {
            laserY += 15;
            if (System.currentTimeMillis() > laserDuration) laserActive = false;
        } else {
            laserY = y + height;
        }
    }
    
    RectF getBounds() { return new RectF(x, y, x + width, y + height); }
    
    void draw(Canvas canvas, Paint paint, GameSurfaceView game) {
        canvas.save();
        
        int[] colors;
        if (phase == 3) colors = new int[]{Color.rgb(200, 0, 0), Color.rgb(255, 50, 50), Color.rgb(255, 100, 100)};
        else if (phase == 2) colors = new int[]{Color.rgb(150, 0, 150), Color.rgb(200, 50, 200), Color.rgb(255, 100, 255)};
        else colors = new int[]{Color.rgb(100, 0, 100), Color.rgb(150, 0, 150), Color.rgb(200, 0, 200)};
        
        Path bossBody = new Path();
        bossBody.moveTo(x + width / 2f, y);
        bossBody.lineTo(x + width, y + height * 0.3f);
        bossBody.lineTo(x + width, y + height);
        bossBody.lineTo(x + width * 0.7f, y + height * 0.8f);
        bossBody.lineTo(x + width * 0.5f, y + height);
        bossBody.lineTo(x + width * 0.3f, y + height * 0.8f);
        bossBody.lineTo(x, y + height);
        bossBody.lineTo(x, y + height * 0.3f);
        bossBody.close();
        
        LinearGradient bossGrad = new LinearGradient(x, y, x + width, y + height, colors, null, Shader.TileMode.CLAMP);
        paint.setShader(bossGrad);
        canvas.drawPath(bossBody, paint);
        paint.setShader(null);
        
        int eyeColor = phase == 3 ? Color.rgb(255, 100, 100) : (phase == 2 ? Color.rgb(255, 200, 100) : Color.YELLOW);
        paint.setColor(eyeColor);
        int eyeY = (int)(y + height * 0.3f);
        int eyeSize = 15 + phase * 2;
        canvas.drawCircle(x + width * 0.35f, eyeY, eyeSize, paint);
        canvas.drawCircle(x + width * 0.65f, eyeY, eyeSize, paint);
        
        paint.setColor(Color.BLACK);
        int pupilSize = 6 + phase;
        canvas.drawCircle(x + width * 0.35f, eyeY, pupilSize, paint);
        canvas.drawCircle(x + width * 0.65f, eyeY, pupilSize, paint);
        
        if (phase >= 2) {
            int barY = (int)(y + height * 0.6f);
            paint.setColor(Color.rgb(50 + phase * 20, 0, 50 + phase * 20));
            canvas.drawRect(x + 20, barY, x + width - 20, barY + 15, paint);
            
            if (phase == 3) {
                int barY2 = (int)(y + height * 0.75f);
                canvas.drawRect(x + 20, barY2, x + width - 20, barY2 + 10, paint);
            }
        }
        
        if (laserActive) {
            paint.setColor(Color.MAGENTA);
            paint.setAlpha(150);
            float warningY = laserY - 50;
            canvas.drawRect(x + width * 0.3f, y + height * 0.5f, x + width * 0.7f, warningY, paint);
            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
        }
        
        canvas.restore();
        
        int particleCount = 3 + phase;
        for (int i = 0; i < particleCount; i++) {
            if (game != null) {
                game.particles.add(new Particle(x + width / 2 + random.nextFloat() * 40 - 20, y + height,
                    random.nextFloat() * 2 - 1, 2 + random.nextFloat() * 2,
                    Color.argb(255, 255, 100 + (int)(50 * Math.sin(enginePulse)), 0), 200));
            }
        }
    }
    
    void shootSpiral() {
        if (!canSpiral) return;
        spiralTimer += 100;
        int bullets = 8 + phase * 4;
        for (int i = 0; i < bullets; i++) {
            float angle = (float)(Math.PI * 2 * i / bullets + spiralTimer * 0.001);
            float vx = (float)Math.cos(angle) * 5;
            float vy = (float)Math.sin(angle) * 5;
        }
    }
    
    void activateLaser(long duration) {
        if (!canLaser) return;
        laserActive = true;
        laserY = y + height * 0.5f;
        laserDuration = System.currentTimeMillis() + duration;
    }
    
    void enrage() {
        enraged = true;
        phase = 4;
        canLaser = true;
        canSpiral = true;
        canBomb = true;
        moveSpeed = 6;
    }
}
