package com.spaceshooter;

import android.graphics.*;

public class Nuke {
    float x, y;
    float targetX, targetY;
    float speed = 15;
    boolean exploded = false;
    long explosionTime = 0;
    float explosionRadius = 200;
    
    Nuke(float x, float y, float targetX, float targetY) {
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
    }
    
    void update(long dt) {
        if (exploded) {
            explosionTime += dt;
            return;
        }
        
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 5) {
            x += (dx / dist) * speed * dt * 0.1f;
            y += (dy / dist) * speed * dt * 0.1f;
        } else {
            exploded = true;
            explosionTime = 0;
        }
    }
    
    void draw(Canvas canvas, Paint paint) {
        if (exploded) {
            float progress = explosionTime / 500f;
            if (progress <= 1) {
                float radius = explosionRadius * progress;
                paint.setStyle(Paint.Style.FILL);
                
                int alpha = (int)(255 * (1 - progress));
                paint.setColor(Color.argb(alpha, 255, 200, 100));
                canvas.drawCircle(targetX, targetY, radius, paint);
                
                paint.setColor(Color.argb(alpha / 2, 255, 100, 50));
                canvas.drawCircle(targetX, targetY, radius * 0.7f, paint);
                
                paint.setColor(Color.argb(alpha, 255, 255, 200));
                canvas.drawCircle(targetX, targetY, radius * 0.3f, paint);
            }
        } else {
            paint.setStyle(Paint.Style.FILL);
            
            float angle = (float)Math.atan2(targetY - y, targetX - x);
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate((float)Math.toDegrees(angle) + 90);
            
            paint.setColor(Color.rgb(255, 150, 50));
            Path nuke = new Path();
            nuke.moveTo(0, -20);
            nuke.lineTo(8, 10);
            nuke.lineTo(4, 10);
            nuke.lineTo(4, 20);
            nuke.lineTo(-4, 20);
            nuke.lineTo(-4, 10);
            nuke.lineTo(-8, 10);
            nuke.close();
            canvas.drawPath(nuke, paint);
            
            paint.setColor(Color.rgb(255, 255, 200));
            canvas.drawCircle(0, -10, 5, paint);
            
            canvas.restore();
            
            paint.setColor(Color.rgb(100, 100, 100));
            canvas.drawLine(x, y, x, y + 30, paint);
        }
    }
    
    boolean isExplosionDone() {
        return exploded && explosionTime > 500;
    }
}
