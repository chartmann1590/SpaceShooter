package com.spaceshooter;

import android.graphics.*;
import java.util.Random;

public class Meteor {
    float x, y;
    int size;
    int health;
    float speed;
    float rotation = 0;
    float rotationSpeed;
    int color;
    boolean isOnFire = false;
    long fireTimer = 0;
    Random random = new Random();
    
    Meteor(float x, float y, int size, int screenHeight) {
        this.x = x;
        this.y = -size;
        this.size = size;
        this.health = size / 5;
        this.speed = 2 + random.nextFloat() * 3;
        this.rotationSpeed = (random.nextFloat() - 0.5f) * 0.1f;
        this.color = Color.rgb(100 + random.nextInt(50), 80 + random.nextInt(40), 60 + random.nextInt(30));
    }
    
    void update(long dt) {
        y += speed * dt * 0.1f;
        rotation += rotationSpeed * dt;
        
        if (isOnFire) {
            fireTimer -= dt;
            if (fireTimer <= 0) {
                isOnFire = false;
            }
        }
    }
    
    void draw(Canvas canvas, Paint paint) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate((float)Math.toDegrees(rotation));
        
        paint.setStyle(Paint.Style.FILL);
        
        if (isOnFire) {
            paint.setColor(Color.rgb(255, 100, 50));
            canvas.drawCircle(0, 0, size * 1.3f, paint);
        }
        
        paint.setColor(color);
        
        Path meteor = new Path();
        int points = 8;
        for (int i = 0; i < points; i++) {
            float angle = (float)(Math.PI * 2 * i / points);
            float radius = size * (0.7f + random.nextFloat() * 0.3f);
            float px = (float)Math.cos(angle) * radius;
            float py = (float)Math.sin(angle) * radius;
            if (i == 0) meteor.moveTo(px, py);
            else meteor.lineTo(px, py);
        }
        meteor.close();
        canvas.drawPath(meteor, paint);
        
        paint.setColor(Color.rgb(80, 60, 40));
        canvas.drawCircle(-size * 0.2f, -size * 0.2f, size * 0.2f, paint);
        canvas.drawCircle(size * 0.3f, size * 0.1f, size * 0.15f, paint);
        
        canvas.restore();
    }
    
    RectF getBounds() {
        return new RectF(x - size, y - size, x + size, y + size);
    }
    
    void setOnFire(long duration) {
        isOnFire = true;
        fireTimer = duration;
    }
}
